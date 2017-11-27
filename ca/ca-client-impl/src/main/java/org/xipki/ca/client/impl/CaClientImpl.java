/*
 *
 * Copyright (c) 2013 - 2017 Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 *
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.ca.client.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.bouncycastle.asn1.cmp.CMPCertificate;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.crmf.CertRequest;
import org.bouncycastle.asn1.crmf.ProofOfPossession;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.client.api.CaClient;
import org.xipki.ca.client.api.CaClientException;
import org.xipki.ca.client.api.CertIdOrError;
import org.xipki.ca.client.api.CertOrError;
import org.xipki.ca.client.api.CertprofileInfo;
import org.xipki.ca.client.api.EnrollCertResult;
import org.xipki.ca.client.api.PkiErrorException;
import org.xipki.ca.client.api.dto.CsrEnrollCertRequest;
import org.xipki.ca.client.api.dto.EnrollCertRequest;
import org.xipki.ca.client.api.dto.EnrollCertRequestEntry;
import org.xipki.ca.client.api.dto.EnrollCertResultEntry;
import org.xipki.ca.client.api.dto.EnrollCertResultResp;
import org.xipki.ca.client.api.dto.ErrorResultEntry;
import org.xipki.ca.client.api.dto.ResultEntry;
import org.xipki.ca.client.api.dto.RevokeCertRequest;
import org.xipki.ca.client.api.dto.RevokeCertRequestEntry;
import org.xipki.ca.client.api.dto.RevokeCertResultEntry;
import org.xipki.ca.client.api.dto.RevokeCertResultType;
import org.xipki.ca.client.api.dto.UnrevokeOrRemoveCertEntry;
import org.xipki.ca.client.api.dto.UnrevokeOrRemoveCertRequest;
import org.xipki.ca.client.impl.jaxb.CAClientType;
import org.xipki.ca.client.impl.jaxb.CAType;
import org.xipki.ca.client.impl.jaxb.CertprofileType;
import org.xipki.ca.client.impl.jaxb.CertprofilesType;
import org.xipki.ca.client.impl.jaxb.CmpControlType;
import org.xipki.ca.client.impl.jaxb.FileOrValueType;
import org.xipki.ca.client.impl.jaxb.ObjectFactory;
import org.xipki.ca.client.impl.jaxb.RequestorType;
import org.xipki.ca.client.impl.jaxb.ResponderType;
import org.xipki.common.HealthCheckResult;
import org.xipki.common.ObjectCreationException;
import org.xipki.common.RequestResponseDebug;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.CompareUtil;
import org.xipki.common.util.IoUtil;
import org.xipki.common.util.LogUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.common.util.XmlUtil;
import org.xipki.security.AlgorithmValidator;
import org.xipki.security.CollectionAlgorithmValidator;
import org.xipki.security.ConcurrentContentSigner;
import org.xipki.security.SecurityFactory;
import org.xipki.security.SignerConf;
import org.xipki.security.util.X509Util;
import org.xml.sax.SAXException;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public final class CaClientImpl implements CaClient {

    private class ClientConfigUpdater implements Runnable {

        private static final long MINUTE = 60L * 1000;

        private AtomicBoolean inProcess = new AtomicBoolean(false);

        private long lastUpdate;

        ClientConfigUpdater() {
        }

        @Override
        public void run() {
            if (inProcess.get()) {
                return;
            }

            inProcess.set(true);

            try {
                // just updated within the last 2 minutes
                if (System.currentTimeMillis() - lastUpdate < 2 * MINUTE) {
                    return;
                }

                StringBuilder sb = new StringBuilder("scheduled configuring CAs ");
                sb.append(autoConfCaNames);

                LOG.info(sb.toString());
                Set<String> failedCaNames = autoConfCas(autoConfCaNames);

                if (CollectionUtil.isNonEmpty(failedCaNames)) {
                    LOG.warn("could not configure following CAs {}", failedCaNames);
                }

            } finally {
                lastUpdate = System.currentTimeMillis();
                inProcess.set(false);
            }
        }

    } // class ClientConfigUpdater

    private static final Logger LOG = LoggerFactory.getLogger(CaClientImpl.class);

    private static Object jaxbUnmarshallerLock = new Object();

    private static Unmarshaller jaxbUnmarshaller;

    private final Map<String, CaConf> casMap = new HashMap<>();

    private final Set<String> autoConfCaNames = new HashSet<>();

    private SecurityFactory securityFactory;

    private String confFile;

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    public CaClientImpl() {
    }

    public void setSecurityFactory(final SecurityFactory securityFactory) {
        this.securityFactory = securityFactory;
    }

    /**
     *
     * @return names of CAs which must not been configured.
     */
    private Set<String> autoConfCas(Set<String> caNames) {
        if (caNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> caNamesWithError = new HashSet<>();

        Set<String> errorCaNames = new HashSet<>();
        for (String name : caNames) {
            CaConf ca = casMap.get(name);

            try {
                CaInfo caInfo = ca.requestor().retrieveCaInfo(name, null);
                if (ca.isCertAutoconf()) {
                    ca.setCert(caInfo.cert());
                }
                if (ca.isCertprofilesAutoconf()) {
                    ca.setCertprofiles(caInfo.certprofiles());
                }
                if (ca.isCmpControlAutoconf()) {
                    ca.setCmpControl(caInfo.cmpControl());
                }
                LOG.info("retrieved CAInfo for CA " + name);
            } catch (CmpRequestorException | PkiErrorException | CertificateEncodingException
                    | RuntimeException ex) {
                errorCaNames.add(name);
                caNamesWithError.add(name);
                LogUtil.error(LOG, ex, "could not retrieve CAInfo for CA " + name);
            }
        }

        return caNamesWithError;
    } // method autoConfCas

    public void init() throws CaClientException {
        init0(true);
    }

    private synchronized void init0(final boolean force) throws CaClientException {
        if (confFile == null) {
            throw new IllegalStateException("confFile is not set");
        }
        if (securityFactory == null) {
            throw new IllegalStateException("securityFactory is not set");
        }

        if (!force && initialized.get()) {
            return;
        }

        // reset
        this.casMap.clear();
        this.autoConfCaNames.clear();
        if (this.scheduledThreadPoolExecutor != null) {
            this.scheduledThreadPoolExecutor.shutdownNow();
        }
        this.initialized.set(false);

        LOG.info("initializing ...");
        File configFile = new File(IoUtil.expandFilepath(confFile));
        if (!configFile.exists()) {
            throw new CaClientException("could not find configuration file " + confFile);
        }

        CAClientType config;
        try {
            config = parse(new FileInputStream(configFile));
        } catch (FileNotFoundException ex) {
            throw new CaClientException("could not read file " + confFile);
        }
        int numActiveCAs = 0;

        for (CAType caType : config.getCAs().getCA()) {
            if (!caType.isEnabled()) {
                LOG.info("CA " + caType.getName() + " is disabled");
                continue;
            }
            numActiveCAs++;
        }

        if (numActiveCAs == 0) {
            LOG.warn("no active CA is configured");
        }

        Boolean bo = config.isDevMode();
        boolean devMode = bo != null && bo.booleanValue();

        // responders
        Map<String, CmpResponder> responders = new HashMap<>();
        for (ResponderType m : config.getResponders().getResponder()) {
            X509Certificate cert;
            try {
                cert = X509Util.parseCert(readData(m.getCert()));
            } catch (CertificateException | IOException ex) {
                LogUtil.error(LOG, ex, "could not configure responder " + m.getName());
                throw new CaClientException(ex.getMessage(), ex);
            }

            Set<String> algoNames = new HashSet<>();
            for (String algo : m.getSignatureAlgos().getSignatureAlgo()) {
                algoNames.add(algo);
            }
            AlgorithmValidator sigAlgoValidator;
            try {
                sigAlgoValidator = new CollectionAlgorithmValidator(algoNames);
            } catch (NoSuchAlgorithmException ex) {
                throw new CaClientException(ex.getMessage());
            }

            responders.put(m.getName(), new CmpResponder(cert, sigAlgoValidator));
        }

        // CA
        Set<CaConf> cas = new HashSet<>();
        for (CAType caType : config.getCAs().getCA()) {
            bo = caType.isEnabled();
            if (!bo.booleanValue()) {
                continue;
            }

            String caName = caType.getName();
            try {
                // responder
                CmpResponder responder = responders.get(caType.getResponder());
                if (responder == null) {
                    throw new CaClientException("no responder named " + caType.getResponder()
                            + " is configured");
                }
                CaConf ca = new CaConf(caName, caType.getUrl(), caType.getHealthUrl(),
                        caType.getRequestor(), responder);

                // CA cert
                if (caType.getCaCert().getAutoconf() != null) {
                    ca.setCertAutoconf(true);
                } else {
                    ca.setCertAutoconf(false);
                    ca.setCert(X509Util.parseCert(readData(caType.getCaCert().getCert())));
                }

                // CMPControl
                CmpControlType cmpCtrlType = caType.getCmpControl();
                if (cmpCtrlType.getAutoconf() != null) {
                    ca.setCmpControlAutoconf(true);
                } else {
                    ca.setCmpControlAutoconf(false);
                    Boolean tmpBo = cmpCtrlType.isRrAkiRequired();
                    ClientCmpControl control = new ClientCmpControl(
                            (tmpBo == null) ? false : tmpBo.booleanValue());
                    ca.setCmpControl(control);
                }

                // Certprofiles
                CertprofilesType certprofilesType = caType.getCertprofiles();
                if (certprofilesType.getAutoconf() != null) {
                    ca.setCertprofilesAutoconf(true);
                } else {
                    ca.setCertprofilesAutoconf(false);
                    List<CertprofileType> types = certprofilesType.getCertprofile();
                    Set<CertprofileInfo> profiles = new HashSet<>(types.size());
                    for (CertprofileType m : types) {
                        String conf = null;
                        if (m.getConf() != null) {
                            conf = m.getConf().getValue();
                            if (conf == null) {
                                conf = new String(IoUtil.read(m.getConf().getFile()));
                            }
                        }
                        CertprofileInfo profile = new CertprofileInfo(m.getName(), m.getType(),
                                conf);
                        profiles.add(profile);
                    }
                    ca.setCertprofiles(profiles);
                }

                cas.add(ca);
                if (ca.isCertAutoconf() || ca.isCertprofilesAutoconf()
                        || ca.isCmpControlAutoconf()) {
                    autoConfCaNames.add(caName);
                }
            } catch (IOException | CertificateException ex) {
                LogUtil.error(LOG, ex, "could not configure CA " + caName);
                if (!devMode) {
                    throw new CaClientException(ex.getMessage(), ex);
                }
            }
        }

        // requestors
        Map<String, X509Certificate> requestorCerts = new HashMap<>();
        Map<String, ConcurrentContentSigner> requestorSigners = new HashMap<>();
        Map<String, Boolean> requestorSignRequests = new HashMap<>();

        for (RequestorType requestorConf : config.getRequestors().getRequestor()) {
            String name = requestorConf.getName();
            requestorSignRequests.put(name, requestorConf.isSignRequest());

            X509Certificate requestorCert = null;
            if (requestorConf.getCert() != null) {
                try {
                    requestorCert = X509Util.parseCert(readData(requestorConf.getCert()));
                    requestorCerts.put(name, requestorCert);
                } catch (Exception ex) {
                    throw new CaClientException(ex.getMessage(), ex);
                }
            }

            if (requestorConf.getSignerType() != null) {
                try {
                    SignerConf signerConf = new SignerConf(requestorConf.getSignerConf());
                    ConcurrentContentSigner requestorSigner = securityFactory.createSigner(
                            requestorConf.getSignerType(), signerConf, requestorCert);
                    requestorSigners.put(name, requestorSigner);
                } catch (ObjectCreationException ex) {
                    throw new CaClientException(ex.getMessage(), ex);
                }
            } else {
                if (requestorConf.isSignRequest()) {
                    throw new CaClientException("signer of requestor must be configured");
                } else if (requestorCert == null) {
                    throw new CaClientException(
                        "at least one of certificate and signer of requestor must be configured");
                }
            }
        }

        for (CaConf ca :cas) {
            if (this.casMap.containsKey(ca.name())) {
                throw new CaClientException("duplicate CAs with the same name " + ca.name());
            }

            String requestorName = ca.requestorName();

            X509CmpRequestor cmpRequestor;
            if (requestorSigners.containsKey(requestorName)) {
                cmpRequestor = new DefaultHttpX509CmpRequestor(requestorSigners.get(requestorName),
                        ca.responder(), ca.url(), securityFactory);
                cmpRequestor.setSignRequest(requestorSignRequests.get(requestorName));
            } else if (requestorCerts.containsKey(requestorName)) {
                cmpRequestor = new DefaultHttpX509CmpRequestor(requestorCerts.get(requestorName),
                        ca.responder(), ca.url(), securityFactory);
            } else {
                throw new CaClientException("could not find requestor named " + requestorName
                        + " for CA " + ca.name());
            }

            ca.setRequestor(cmpRequestor);
            this.casMap.put(ca.name(), ca);
        }

        if (!autoConfCaNames.isEmpty()) {
            Integer caInfoUpdateInterval = config.getCAs().getCAInfoUpdateInterval();
            if (caInfoUpdateInterval == null) {
                caInfoUpdateInterval = 10;
            } else if (caInfoUpdateInterval <= 0) {
                caInfoUpdateInterval = 0;
            } else if (caInfoUpdateInterval < 5) {
                caInfoUpdateInterval = 5;
            }

            LOG.info("configuring CAs {}", autoConfCaNames);
            Set<String> failedCaNames = autoConfCas(autoConfCaNames);

            // try to re-configure the failed CAs
            if (CollectionUtil.isNonEmpty(failedCaNames)) {
                for (int i = 0; i < 3; i++) {
                    LOG.info("configuring ({}-th retry) CAs {}", i + 1, failedCaNames);

                    failedCaNames = autoConfCas(failedCaNames);
                    if (CollectionUtil.isEmpty(failedCaNames)) {
                        break;
                    }

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        LOG.warn("interrupted", ex);
                    }
                }
            }

            if (CollectionUtil.isNonEmpty(failedCaNames)) {
                final String msg = "could not configure following CAs " + failedCaNames;
                if (devMode) {
                    LOG.warn(msg);
                } else {
                    throw new CaClientException(msg);
                }
            }

            if (caInfoUpdateInterval > 0) {
                scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
                scheduledThreadPoolExecutor.scheduleAtFixedRate(new ClientConfigUpdater(),
                        caInfoUpdateInterval, caInfoUpdateInterval, TimeUnit.MINUTES);
            }
        }

        initialized.set(true);
        LOG.info("initialized");
    } // method init

    public void shutdown() {
        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdown();
            while (!scheduledThreadPoolExecutor.isTerminated()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    LOG.warn("interrupted: {}", ex.getMessage());
                }
            }
            scheduledThreadPoolExecutor = null;
        }
    }

    @Override
    public EnrollCertResult requestCert(final String caName, final CertificationRequest csr,
            final String profile, final Date notBefore, final Date notAfter,
            final RequestResponseDebug debug) throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("csr", csr);

        String tmpCaName = caName;
        if (tmpCaName == null) {
            tmpCaName = getCaNameForProfile(profile);
        }

        if (tmpCaName == null) {
            throw new CaClientException("certprofile " + profile + " is not supported by any CA");
        }

        CaConf ca = casMap.get(tmpCaName.trim());
        if (ca == null) {
            throw new CaClientException("could not find CA named " + tmpCaName);
        }

        final String id = "cert-1";
        CsrEnrollCertRequest request = new CsrEnrollCertRequest(id, profile, csr);
        EnrollCertResultResp result;
        try {
            result = ca.requestor().requestCertificate(request, notBefore, notAfter, debug);
        } catch (CmpRequestorException ex) {
            throw new CaClientException(ex.getMessage(), ex);
        }

        return parseEnrollCertResult(result, tmpCaName);
    } // method requestCert

    @Override
    public EnrollCertResult requestCerts(final String caName, final EnrollCertRequest request,
            final RequestResponseDebug debug) throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("request", request);

        List<EnrollCertRequestEntry> requestEntries = request.requestEntries();
        if (CollectionUtil.isEmpty(requestEntries)) {
            return null;
        }

        String tmpCaName = caName;
        boolean bo = (tmpCaName != null);
        if (tmpCaName == null) {
            // detect the CA name
            String profile = requestEntries.get(0).certprofile();
            tmpCaName = getCaNameForProfile(profile);
            if (tmpCaName == null) {
                throw new CaClientException("certprofile " + profile
                        + " is not supported by any CA");
            }
        }

        if (bo || request.requestEntries().size() > 1) {
            // make sure that all requests are targeted on the same CA
            for (EnrollCertRequestEntry entry : request.requestEntries()) {
                String profile = entry.certprofile();
                checkCertprofileSupportInCa(profile, tmpCaName);
            }
        }

        CaConf ca = casMap.get(tmpCaName.trim());
        if (ca == null) {
            throw new CaClientException("could not find CA named " + tmpCaName);
        }

        EnrollCertResultResp result;
        try {
            result = ca.requestor().requestCertificate(request, debug);
        } catch (CmpRequestorException ex) {
            throw new CaClientException(ex.getMessage(), ex);
        }

        return parseEnrollCertResult(result, tmpCaName);
    } // method requestCerts

    private void checkCertprofileSupportInCa(final String certprofile, final String caName)
            throws CaClientException {
        String tmpCaName = caName;
        if (tmpCaName != null) {
            CaConf ca = casMap.get(tmpCaName.trim());
            if (ca == null) {
                throw new CaClientException("unknown ca: " + tmpCaName);
            }

            if (!ca.supportsProfile(certprofile)) {
                throw new CaClientException("certprofile " + certprofile
                        + " is not supported by the CA " + tmpCaName);
            }
            return;
        }

        for (CaConf ca : casMap.values()) {
            if (!ca.isCaInfoConfigured()) {
                continue;
            }
            if (!ca.supportsProfile(certprofile)) {
                continue;
            }

            if (tmpCaName == null) {
                tmpCaName = ca.name();
            } else {
                throw new CaClientException("certprofile " + certprofile
                        + " supported by more than one CA, please specify the CA name.");
            }
        }

        if (tmpCaName == null) {
            throw new CaClientException("unsupported certprofile " + certprofile);
        }
    }

    @Override
    public CertIdOrError revokeCert(final String caName, final X509Certificate cert,
            final int reason, final Date invalidityDate, final RequestResponseDebug debug)
            throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("cert", cert);
        CaConf ca = getCa(caName);
        assertIssuedByCa(cert, ca);
        return revokeCert(ca, cert.getSerialNumber(), reason, invalidityDate, debug);
    }

    @Override
    public CertIdOrError revokeCert(final String caName, final BigInteger serial,
            final int reason, final Date invalidityDate, final RequestResponseDebug debug)
            throws CaClientException, PkiErrorException {
        CaConf ca = getCa(caName);
        return revokeCert(ca, serial, reason, invalidityDate, debug);
    }

    private CertIdOrError revokeCert(final CaConf ca, final BigInteger serial,
            final int reason, final Date invalidityDate, final RequestResponseDebug debug)
            throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("ca", ca);
        ParamUtil.requireNonNull("serial", serial);

        final String id = "cert-1";
        RevokeCertRequestEntry entry = new RevokeCertRequestEntry(id, ca.subject(), serial,
                reason, invalidityDate);
        if (ca.cmpControl().isRrAkiRequired()) {
            entry.setAuthorityKeyIdentifier(ca.subjectKeyIdentifier());
        }

        RevokeCertRequest request = new RevokeCertRequest();
        request.addRequestEntry(entry);
        Map<String, CertIdOrError> result = revokeCerts(request, debug);
        return (result == null) ? null : result.get(id);
    }

    @Override
    public Map<String, CertIdOrError> revokeCerts(final RevokeCertRequest request,
            final RequestResponseDebug debug) throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("request", request);

        List<RevokeCertRequestEntry> requestEntries = request.requestEntries();
        if (CollectionUtil.isEmpty(requestEntries)) {
            return Collections.emptyMap();
        }

        X500Name issuer = requestEntries.get(0).issuer();
        for (int i = 1; i < requestEntries.size(); i++) {
            if (!issuer.equals(requestEntries.get(i).issuer())) {
                throw new PkiErrorException(PKIStatus.REJECTION, PKIFailureInfo.badRequest,
                        "revoking certificates issued by more than one CA is not allowed");
            }
        }

        final String caName = getCaNameByIssuer(issuer);
        CaConf caConf = casMap.get(caName);
        if (caConf.cmpControl().isRrAkiRequired()) {
            byte[] aki = caConf.subjectKeyIdentifier();
            List<RevokeCertRequestEntry> entries = request.requestEntries();
            for (RevokeCertRequestEntry entry : entries) {
                if (entry.authorityKeyIdentifier() == null) {
                    entry.setAuthorityKeyIdentifier(aki);
                }
            }
        }

        X509CmpRequestor cmpRequestor = caConf.requestor();
        RevokeCertResultType result;
        try {
            result = cmpRequestor.revokeCertificate(request, debug);
        } catch (CmpRequestorException ex) {
            throw new CaClientException(ex.getMessage(), ex);
        }

        return parseRevokeCertResult(result);
    }

    private Map<String, CertIdOrError> parseRevokeCertResult(final RevokeCertResultType result)
            throws CaClientException {
        Map<String, CertIdOrError> ret = new HashMap<>();

        for (ResultEntry re : result.resultEntries()) {
            CertIdOrError certIdOrError;
            if (re instanceof RevokeCertResultEntry) {
                RevokeCertResultEntry entry = (RevokeCertResultEntry) re;
                certIdOrError = new CertIdOrError(entry.certId());
            } else if (re instanceof ErrorResultEntry) {
                ErrorResultEntry entry = (ErrorResultEntry) re;
                certIdOrError = new CertIdOrError(entry.statusInfo());
            } else {
                throw new CaClientException("unknown type " + re.getClass().getName());
            }

            ret.put(re.id(), certIdOrError);
        }

        return ret;
    }

    @Override
    public X509CRL downloadCrl(final String caName, final RequestResponseDebug debug)
            throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("caName", caName);
        return downloadCrl(caName, (BigInteger) null, debug);
    }

    @Override
    public X509CRL downloadCrl(final String caName, final BigInteger crlNumber,
            final RequestResponseDebug debug) throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("caName", caName);
        init0(false);

        CaConf ca = casMap.get(caName.trim());
        if (ca == null) {
            throw new IllegalArgumentException("unknown CA " + caName);
        }

        X509CmpRequestor requestor = ca.requestor();
        X509CRL result;
        try {
            result = (crlNumber == null) ? requestor.downloadCurrentCrl(debug)
                    : requestor.downloadCrl(crlNumber, debug);
        } catch (CmpRequestorException ex) {
            throw new CaClientException(ex.getMessage(), ex);
        }

        return result;
    }

    @Override
    public X509CRL generateCrl(final String caName, final RequestResponseDebug debug)
            throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("caName", caName);

        CaConf ca = casMap.get(caName.trim());
        if (ca == null) {
            throw new IllegalArgumentException("unknown CA " + caName);
        }

        X509CmpRequestor requestor = ca.requestor();
        try {
            return requestor.generateCrl(debug);
        } catch (CmpRequestorException ex) {
            throw new CaClientException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getCaNameByIssuer(final X500Name issuer) throws CaClientException {
        ParamUtil.requireNonNull("issuer", issuer);

        for (String name : casMap.keySet()) {
            final CaConf ca = casMap.get(name);
            if (!ca.isCaInfoConfigured()) {
                continue;
            }

            if (CompareUtil.equalsObject(ca.subject(), issuer)) {
                return name;
            }
        }

        throw new CaClientException("unknown CA for issuer: " + issuer);
    }

    private String getCaNameForProfile(final String certprofile) throws CaClientException {
        String caName = null;
        for (CaConf ca : casMap.values()) {
            if (!ca.isCaInfoConfigured()) {
                continue;
            }

            if (!ca.supportsProfile(certprofile)) {
                continue;
            }

            if (caName == null) {
                caName = ca.name();
            } else {
                throw new CaClientException("certprofile " + certprofile
                        + " supported by more than one CA, please specify the CA name.");
            }
        }

        return caName;
    }

    private java.security.cert.Certificate getCertificate(final CMPCertificate cmpCert)
            throws CertificateException {
        Certificate bcCert = cmpCert.getX509v3PKCert();
        return (bcCert == null) ? null : X509Util.toX509Cert(bcCert);
    }

    public String confFile() {
        return confFile;
    }

    public void setConfFile(String confFile) {
        this.confFile = ParamUtil.requireNonBlank("confFile", confFile);
    }

    @Override
    public Set<String> caNames() {
        return casMap.keySet();
    }

    @Override
    public byte[] envelope(final CertRequest certRequest, final ProofOfPossession pop,
            final String profileName, final String caName)
            throws CaClientException {
        ParamUtil.requireNonNull("certRequest", certRequest);
        ParamUtil.requireNonNull("pop", pop);
        ParamUtil.requireNonNull("profileName", profileName);

        init0(false);
        String tmpCaName = caName;
        if (tmpCaName == null) {
            // detect the CA name
            tmpCaName = getCaNameForProfile(profileName);
            if (tmpCaName == null) {
                throw new CaClientException("certprofile " + profileName
                        + " is not supported by any CA");
            }
        } else {
            checkCertprofileSupportInCa(profileName, tmpCaName);
        }

        CaConf ca = casMap.get(tmpCaName.trim());
        if (ca == null) {
            throw new CaClientException("could not find CA named " + tmpCaName);
        }

        PKIMessage pkiMessage;
        try {
            pkiMessage = ca.requestor().envelope(certRequest, pop, profileName);
        } catch (CmpRequestorException ex) {
            throw new CaClientException("CmpRequestorException: " + ex.getMessage(), ex);
        }

        try {
            return pkiMessage.getEncoded();
        } catch (IOException ex) {
            throw new CaClientException("IOException: " + ex.getMessage(), ex);
        }
    } // method envelope

    private boolean verify(final java.security.cert.Certificate caCert,
            final java.security.cert.Certificate cert) {
        if (!(caCert instanceof X509Certificate)) {
            return false;
        }
        if (!(cert instanceof X509Certificate)) {
            return false;
        }

        X509Certificate x509caCert = (X509Certificate) caCert;
        X509Certificate x509cert = (X509Certificate) cert;

        if (!x509cert.getIssuerX500Principal().equals(x509caCert.getSubjectX500Principal())) {
            return false;
        }

        boolean inLoadTest = Boolean.getBoolean("org.xipki.loadtest");
        if (inLoadTest) {
            return true;
        }

        PublicKey caPublicKey = x509caCert.getPublicKey();
        try {
            x509cert.verify(caPublicKey);
            return true;
        } catch (SignatureException | InvalidKeyException | CertificateException
                | NoSuchAlgorithmException | NoSuchProviderException ex) {
            LOG.debug("{} while verifying signature: {}", ex.getClass().getName(), ex.getMessage());
            return false;
        }
    } // method verify

    @Override
    public byte[] envelopeRevocation(final X500Name issuer, final BigInteger serial,
            final int reason) throws CaClientException {
        ParamUtil.requireNonNull("issuer", issuer);

        init0(false);
        final String id = "cert-1";
        RevokeCertRequestEntry entry = new RevokeCertRequestEntry(id, issuer, serial, reason, null);
        RevokeCertRequest request = new RevokeCertRequest();
        request.addRequestEntry(entry);

        String caName = getCaNameByIssuer(issuer);
        X509CmpRequestor cmpRequestor = casMap.get(caName).requestor();

        try {
            PKIMessage pkiMessage = cmpRequestor.envelopeRevocation(request);
            return pkiMessage.getEncoded();
        } catch (CmpRequestorException | IOException ex) {
            throw new CaClientException(ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] envelopeRevocation(final X509Certificate cert, final int reason)
            throws CaClientException {
        ParamUtil.requireNonNull("cert", cert);
        X500Name issuer = X500Name.getInstance(cert.getIssuerX500Principal().getEncoded());
        return envelopeRevocation(issuer, cert.getSerialNumber(), reason);
    }

    @Override
    public CertIdOrError unrevokeCert(final String caName, final X509Certificate cert,
            final RequestResponseDebug debug) throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("cert", cert);
        CaConf ca = getCa(caName);
        assertIssuedByCa(cert, ca);
        return unrevokeCert(ca, cert.getSerialNumber(), debug);
    }

    @Override
    public CertIdOrError unrevokeCert(final String caName, final BigInteger serial,
            final RequestResponseDebug debug) throws CaClientException, PkiErrorException {
        CaConf ca = getCa(caName);
        return unrevokeCert(ca, serial, debug);
    }

    private CertIdOrError unrevokeCert(final CaConf ca, final BigInteger serial,
            final RequestResponseDebug debug) throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("ca", ca);
        ParamUtil.requireNonNull("serial", serial);
        final String id = "cert-1";
        UnrevokeOrRemoveCertEntry entry = new UnrevokeOrRemoveCertEntry(id, ca.subject(),
                serial);
        if (ca.cmpControl().isRrAkiRequired()) {
            entry.setAuthorityKeyIdentifier(ca.subjectKeyIdentifier());
        }

        UnrevokeOrRemoveCertRequest request = new UnrevokeOrRemoveCertRequest();
        request.addRequestEntry(entry);
        Map<String, CertIdOrError> result = unrevokeCerts(request, debug);
        return (result == null) ? null : result.get(id);
    }

    @Override
    public Map<String, CertIdOrError> unrevokeCerts(final UnrevokeOrRemoveCertRequest request,
            final RequestResponseDebug debug) throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("request", request);

        init0(false);
        List<UnrevokeOrRemoveCertEntry> requestEntries = request.requestEntries();
        if (CollectionUtil.isEmpty(requestEntries)) {
            return Collections.emptyMap();
        }

        X500Name issuer = requestEntries.get(0).issuer();
        for (int i = 1; i < requestEntries.size(); i++) {
            if (!issuer.equals(requestEntries.get(i).issuer())) {
                throw new PkiErrorException(PKIStatus.REJECTION, PKIFailureInfo.badRequest,
                        "unrevoking certificates issued by more than one CA is not allowed");
            }
        }

        final String caName = getCaNameByIssuer(issuer);
        X509CmpRequestor cmpRequestor = casMap.get(caName).requestor();
        RevokeCertResultType result;
        try {
            result = cmpRequestor.unrevokeCertificate(request, debug);
        } catch (CmpRequestorException ex) {
            throw new CaClientException(ex.getMessage(), ex);
        }

        return parseRevokeCertResult(result);
    } // method unrevokeCerts

    @Override
    public CertIdOrError removeCert(final String caName, final X509Certificate cert,
            final RequestResponseDebug debug)
            throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("cert", cert);
        CaConf ca = getCa(caName);
        assertIssuedByCa(cert, ca);
        return removeCert(ca, cert.getSerialNumber(), debug);
    }

    @Override
    public CertIdOrError removeCert(final String caName, final BigInteger serial,
            final RequestResponseDebug debug)
            throws CaClientException, PkiErrorException {
        CaConf ca = getCa(caName);
        return removeCert(ca, serial, debug);
    }

    private CertIdOrError removeCert(final CaConf ca, final BigInteger serial,
            final RequestResponseDebug debug)
            throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("ca", ca);
        ParamUtil.requireNonNull("serial", serial);
        final String id = "cert-1";
        UnrevokeOrRemoveCertEntry entry = new UnrevokeOrRemoveCertEntry(id, ca.subject(),
                serial);
        if (ca.cmpControl().isRrAkiRequired()) {
            entry.setAuthorityKeyIdentifier(ca.subjectKeyIdentifier());
        }

        UnrevokeOrRemoveCertRequest request = new UnrevokeOrRemoveCertRequest();
        request.addRequestEntry(entry);
        Map<String, CertIdOrError> result = removeCerts(request, debug);
        return (result == null) ? null : result.get(id);
    }

    @Override
    public Map<String, CertIdOrError> removeCerts(final UnrevokeOrRemoveCertRequest request,
            final RequestResponseDebug debug)
            throws CaClientException, PkiErrorException {
        ParamUtil.requireNonNull("request", request);

        init0(false);
        List<UnrevokeOrRemoveCertEntry> requestEntries = request.requestEntries();
        if (CollectionUtil.isEmpty(requestEntries)) {
            return Collections.emptyMap();
        }

        X500Name issuer = requestEntries.get(0).issuer();
        for (int i = 1; i < requestEntries.size(); i++) {
            if (!issuer.equals(requestEntries.get(i).issuer())) {
                throw new PkiErrorException(PKIStatus.REJECTION, PKIFailureInfo.badRequest,
                        "removing certificates issued by more than one CA is not allowed");
            }
        }

        final String caName = getCaNameByIssuer(issuer);
        X509CmpRequestor cmpRequestor = casMap.get(caName).requestor();
        RevokeCertResultType result;
        try {
            result = cmpRequestor.removeCertificate(request, debug);
        } catch (CmpRequestorException ex) {
            throw new CaClientException(ex.getMessage(), ex);
        }

        return parseRevokeCertResult(result);
    }

    @Override
    public Set<CertprofileInfo> getCertprofiles(final String caName) throws CaClientException {
        ParamUtil.requireNonNull("caName", caName);

        init0(false);
        CaConf ca = casMap.get(caName.trim());
        if (ca == null) {
            return Collections.emptySet();
        }

        Set<String> profileNames = ca.profileNames();
        if (CollectionUtil.isEmpty(profileNames)) {
            return Collections.emptySet();
        }

        Set<CertprofileInfo> ret = new HashSet<>(profileNames.size());
        for (String m : profileNames) {
            ret.add(ca.profile(m));
        }
        return ret;
    }

    @Override
    public HealthCheckResult getHealthCheckResult(final String caName) throws CaClientException {
        ParamUtil.requireNonNull("caName", caName);

        String name = "X509CA";
        HealthCheckResult healthCheckResult = new HealthCheckResult(name);

        try {
            init0(false);
        } catch (CaClientException ex) {
            LogUtil.error(LOG, ex, "could not initialize CaCleint");
            healthCheckResult.setHealthy(false);
            return healthCheckResult;
        }

        CaConf ca = casMap.get(caName.trim());
        if (ca == null) {
            throw new IllegalArgumentException("unknown CA " + caName);
        }

        String healthUrlStr = ca.healthUrl();

        URL serverUrl;
        try {
            serverUrl = new URL(healthUrlStr);
        } catch (MalformedURLException ex) {
            throw new CaClientException("invalid URL '" + healthUrlStr + "'");
        }

        try {
            HttpURLConnection httpUrlConnection = IoUtil.openHttpConn(serverUrl);
            InputStream inputStream = httpUrlConnection.getInputStream();
            int responseCode = httpUrlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK
                    && responseCode != HttpURLConnection.HTTP_INTERNAL_ERROR) {
                inputStream.close();
                throw new IOException(String.format("bad response: code='%s', message='%s'",
                        httpUrlConnection.getResponseCode(),
                        httpUrlConnection.getResponseMessage()));
            }

            String responseContentType = httpUrlConnection.getContentType();
            boolean isValidContentType = false;
            if (responseContentType != null) {
                if ("application/json".equalsIgnoreCase(responseContentType)) {
                    isValidContentType = true;
                }
            }
            if (!isValidContentType) {
                inputStream.close();
                throw new IOException("bad response: mime type " + responseContentType
                        + " not supported!");
            }

            byte[] responseBytes = IoUtil.read(inputStream);
            if (responseBytes.length == 0) {
                healthCheckResult.setHealthy(responseCode == HttpURLConnection.HTTP_OK);
            } else {
                String response = new String(responseBytes);
                try {
                    healthCheckResult = HealthCheckResult.getInstanceFromJsonMessage(name,
                            response);
                } catch (IllegalArgumentException ex) {
                    LogUtil.error(LOG, ex, "IOException while parsing the health json message");
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("json message: {}", response);
                    }
                    healthCheckResult.setHealthy(false);
                }
            }
        } catch (IOException ex) {
            LogUtil.error(LOG, ex, "IOException while fetching the URL " + healthUrlStr);
            healthCheckResult.setHealthy(false);
        }

        return healthCheckResult;
    } // method getHealthCheckResult

    private EnrollCertResult parseEnrollCertResult(final EnrollCertResultResp result,
            final String caName) throws CaClientException {
        Map<String, CertOrError> certOrErrors = new HashMap<>();
        for (ResultEntry resultEntry : result.resultEntries()) {
            CertOrError certOrError;
            if (resultEntry instanceof EnrollCertResultEntry) {
                EnrollCertResultEntry entry = (EnrollCertResultEntry) resultEntry;
                try {
                    java.security.cert.Certificate cert = getCertificate(entry.cert());
                    certOrError = new CertOrError(cert);
                } catch (CertificateException ex) {
                    throw new CaClientException(String.format(
                            "CertificateParsingException for request (id=%s): %s",
                            entry.id(), ex.getMessage()));
                }
            } else if (resultEntry instanceof ErrorResultEntry) {
                certOrError = new CertOrError(((ErrorResultEntry) resultEntry).statusInfo());
            } else {
                certOrError = null;
            }

            certOrErrors.put(resultEntry.id(), certOrError);
        }

        List<CMPCertificate> cmpCaPubs = result.caCertificates();

        if (CollectionUtil.isEmpty(cmpCaPubs)) {
            return new EnrollCertResult(null, certOrErrors);
        }

        List<java.security.cert.Certificate> caPubs = new ArrayList<>(cmpCaPubs.size());
        for (CMPCertificate cmpCaPub : cmpCaPubs) {
            try {
                caPubs.add(getCertificate(cmpCaPub));
            } catch (CertificateException ex) {
                LogUtil.error(LOG, ex, "could not extract the caPub from CMPCertificate");
            }
        }

        java.security.cert.Certificate caCert = null;
        for (CertOrError certOrError : certOrErrors.values()) {
            java.security.cert.Certificate cert = certOrError.certificate();
            if (cert == null) {
                continue;
            }

            for (java.security.cert.Certificate caPub : caPubs) {
                if (verify(caPub, cert)) {
                    caCert = caPub;
                    break;
                }
            }

            if (caCert != null) {
                break;
            }
        }

        if (caCert == null) {
            return new EnrollCertResult(null, certOrErrors);
        }

        for (CertOrError certOrError : certOrErrors.values()) {
            java.security.cert.Certificate cert = certOrError.certificate();
            if (cert == null) {
                continue;
            }

            if (!verify(caCert, cert)) {
                LOG.warn(
                    "not all certificates are issued by CA embedded in caPubs, ignore the caPubs");
                return new EnrollCertResult(null, certOrErrors);
            }
        }

        return new EnrollCertResult(caCert, certOrErrors);
    } // method parseEnrollCertResult

    private static CAClientType parse(final InputStream configStream) throws CaClientException {
        Object root;
        synchronized (jaxbUnmarshallerLock) {
            try {
                if (jaxbUnmarshaller == null) {
                    JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
                    jaxbUnmarshaller = context.createUnmarshaller();

                    final SchemaFactory schemaFact = SchemaFactory.newInstance(
                            javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    URL url = CAClientType.class.getResource("/xsd/caclient-conf.xsd");
                    jaxbUnmarshaller.setSchema(schemaFact.newSchema(url));
                }

                root = jaxbUnmarshaller.unmarshal(configStream);
            } catch (SAXException ex) {
                throw new CaClientException("parsing profile failed, message: " + ex.getMessage(),
                        ex);
            } catch (JAXBException ex) {
                throw new CaClientException("parsing profile failed, message: "
                        + XmlUtil.getMessage(ex), ex);
            }
        }

        try {
            configStream.close();
        } catch (IOException ex) {
            LOG.warn("could not close xmlConfStream: {}", ex.getMessage());
        }

        if (root instanceof JAXBElement) {
            return (CAClientType) ((JAXBElement<?>) root).getValue();
        } else {
            throw new CaClientException("invalid root element type");
        }
    } // method parse

    private static byte[] readData(final FileOrValueType fileOrValue) throws IOException {
        byte[] data = fileOrValue.getValue();
        if (data == null) {
            data = IoUtil.read(fileOrValue.getFile());
        }
        return data;
    }

    private CaConf getCa(String caName) throws CaClientException {
        String tmpCaName = caName;
        if (tmpCaName == null) {
            Iterator<String> names = casMap.keySet().iterator();
            if (!names.hasNext()) {
                throw new CaClientException("no CA is configured");
            }
            tmpCaName = names.next();
        }

        CaConf ca = casMap.get(tmpCaName.trim());
        if (ca == null) {
            throw new CaClientException("could not find CA named " + tmpCaName);
        }
        return ca;
    }

    private void assertIssuedByCa(X509Certificate cert, CaConf ca) throws CaClientException {
        boolean issued;
        try {
            issued = X509Util.issues(ca.cert(), cert);
        } catch (CertificateEncodingException ex) {
            LogUtil.error(LOG, ex);
            issued = false;
        }
        if (!issued) {
            throw new CaClientException("the given certificate is not issued by the CA");
        }
    }
}
