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

package org.xipki.ca.server.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSAUtil;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.api.BadCertTemplateException;
import org.xipki.ca.api.OperationException;
import org.xipki.ca.api.OperationException.ErrorCode;
import org.xipki.ca.api.profile.CertValidity;
import org.xipki.ca.api.profile.CertprofileException;
import org.xipki.ca.api.profile.ExtensionValue;
import org.xipki.ca.api.profile.ExtensionValues;
import org.xipki.ca.api.profile.x509.SubjectInfo;
import org.xipki.ca.api.profile.x509.X509CertLevel;
import org.xipki.ca.server.mgmt.api.CaEntry;
import org.xipki.common.ConfPairs;
import org.xipki.common.InvalidConfException;
import org.xipki.common.ObjectCreationException;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.ConcurrentBagEntrySigner;
import org.xipki.security.ConcurrentContentSigner;
import org.xipki.security.SecurityFactory;
import org.xipki.security.SignerConf;
import org.xipki.security.exception.NoIdleSignerException;
import org.xipki.security.exception.XiSecurityException;
import org.xipki.security.util.KeyUtil;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

class X509SelfSignedCertBuilder {

    static class GenerateSelfSignedResult {

        private final String signerConf;

        private final X509Certificate cert;

        GenerateSelfSignedResult(final String signerConf, final X509Certificate cert) {
            this.signerConf = signerConf;
            this.cert = cert;
        }

        String getSignerConf() {
            return signerConf;
        }

        X509Certificate getCert() {
            return cert;
        }

    } // class GenerateSelfSignedResult

    private static final Logger LOG = LoggerFactory.getLogger(X509SelfSignedCertBuilder.class);

    private X509SelfSignedCertBuilder() {
    }

    public static GenerateSelfSignedResult generateSelfSigned(final SecurityFactory securityFactory,
            final String signerType, final String signerConf,
            final IdentifiedX509Certprofile certprofile, final CertificationRequest csr,
            final BigInteger serialNumber, final List<String> cacertUris,
            final List<String> ocspUris, final List<String> crlUris,
            final List<String> deltaCrlUris) throws OperationException, InvalidConfException {
        ParamUtil.requireNonNull("securityFactory", securityFactory);
        ParamUtil.requireNonBlank("signerType", signerType);
        ParamUtil.requireNonNull("certprofile", certprofile);
        ParamUtil.requireNonNull("csr", csr);
        ParamUtil.requireNonNull("serialNumber", serialNumber);
        if (serialNumber.compareTo(BigInteger.ZERO) != 1) {
            throw new IllegalArgumentException(
                    "serialNumber must not be non-positive: " + serialNumber);
        }

        X509CertLevel level = certprofile.certLevel();
        if (X509CertLevel.RootCA != level) {
            throw new IllegalArgumentException("certprofile is not of level "
                    + X509CertLevel.RootCA);
        }

        if (!securityFactory.verifyPopo(csr, null)) {
            throw new InvalidConfException("could not validate POP for the CSR");
        }

        if ("pkcs12".equalsIgnoreCase(signerType) || "jks".equalsIgnoreCase(signerType)) {
            ConfPairs keyValues = new ConfPairs(signerConf);
            String keystoreConf = keyValues.value("keystore");
            if (keystoreConf == null) {
                throw new InvalidConfException(
                    "required parameter 'keystore' for types PKCS12 and JKS, is not specified");
            }
        }

        ConcurrentContentSigner signer;
        try {
            List<String[]> signerConfs = CaEntry.splitCaSignerConfs(signerConf);
            List<String> restrictedSigAlgos = certprofile.signatureAlgorithms();

            String thisSignerConf = null;
            if (CollectionUtil.isEmpty(restrictedSigAlgos)) {
                thisSignerConf = signerConfs.get(0)[1];
            } else {
                for (String algo : restrictedSigAlgos) {
                    for (String[] m : signerConfs) {
                        if (m[0].equals(algo)) {
                            thisSignerConf = m[1];
                            break;
                        }
                    }

                    if (thisSignerConf != null) {
                        break;
                    }
                }
            }

            if (thisSignerConf == null) {
                throw new OperationException(ErrorCode.SYSTEM_FAILURE,
                    "CA does not support any signature algorithm restricted by the cert profile");
            }

            signer = securityFactory.createSigner(signerType, new SignerConf(thisSignerConf),
                    (X509Certificate[]) null);
        } catch (XiSecurityException | ObjectCreationException ex) {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, ex);
        }

        SubjectPublicKeyInfo publicKeyInfo;
        if (signer.getCertificate() != null) {
            // this cert is the dummy one which can be considered only as public key container
            Certificate bcCert;
            try {
                bcCert = Certificate.getInstance(signer.getCertificate().getEncoded());
            } catch (Exception ex) {
                throw new OperationException(ErrorCode.SYSTEM_FAILURE,
                        "could not reparse certificate: " + ex.getMessage());
            }
            publicKeyInfo = bcCert.getSubjectPublicKeyInfo();
        } else {
            PublicKey signerPublicKey = signer.getPublicKey();
            try {
                publicKeyInfo = KeyUtil.createSubjectPublicKeyInfo(signerPublicKey);
            } catch (InvalidKeyException ex) {
                throw new OperationException(ErrorCode.SYSTEM_FAILURE,
                        "cannot generate SubjectPublicKeyInfo from publicKey: " + ex.getMessage());
            }
        }

        X509Certificate newCert = generateCertificate(signer, certprofile, csr, serialNumber,
                publicKeyInfo, cacertUris, ocspUris, crlUris, deltaCrlUris);

        return new GenerateSelfSignedResult(signerConf, newCert);
    } // method generateSelfSigned

    private static X509Certificate generateCertificate(final ConcurrentContentSigner signer,
            final IdentifiedX509Certprofile certprofile, final CertificationRequest csr,
            final BigInteger serialNumber, final SubjectPublicKeyInfo publicKeyInfo,
            final List<String> cacertUris, final List<String> ocspUris, final List<String> crlUris,
            final List<String> deltaCrlUris) throws OperationException {

        SubjectPublicKeyInfo tmpPublicKeyInfo;
        try {
            tmpPublicKeyInfo = X509Util.toRfc3279Style(publicKeyInfo);
        } catch (InvalidKeySpecException ex) {
            LOG.warn("SecurityUtil.toRfc3279Style", ex);
            throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE, ex);
        }

        try {
            certprofile.checkPublicKey(tmpPublicKeyInfo);
        } catch (BadCertTemplateException ex) {
            LOG.warn("certprofile.checkPublicKey", ex);
            throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE, ex);
        }

        X500Name requestedSubject = csr.getCertificationRequestInfo().getSubject();

        SubjectInfo subjectInfo;
        // subject
        try {
            subjectInfo = certprofile.getSubject(requestedSubject);
        } catch (CertprofileException ex) {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE,
                    "exception in cert profile " + certprofile.ident());
        } catch (BadCertTemplateException ex) {
            LOG.warn("certprofile.getSubject", ex);
            throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE, ex);
        }

        Date notBefore = certprofile.notBefore(null);
        if (notBefore == null) {
            notBefore = new Date();
        }

        CertValidity validity = certprofile.validity();
        if (validity == null) {
            throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE,
                    "no validity specified in the profile " + certprofile.ident());
        }

        Date notAfter = validity.add(notBefore);

        X500Name grantedSubject = subjectInfo.grantedSubject();

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(grantedSubject,
                serialNumber, notBefore, notAfter, grantedSubject, tmpPublicKeyInfo);

        PublicCaInfo publicCaInfo = new PublicCaInfo(grantedSubject, serialNumber, null, null,
                cacertUris, ocspUris, crlUris, deltaCrlUris);

        Extensions extensions = null;
        ASN1Set attrs = csr.getCertificationRequestInfo().getAttributes();
        for (int i = 0; i < attrs.size(); i++) {
            Attribute attr = Attribute.getInstance(attrs.getObjectAt(i));
            if (PKCSObjectIdentifiers.pkcs_9_at_extensionRequest.equals(attr.getAttrType())) {
                extensions = Extensions.getInstance(attr.getAttributeValues()[0]);
            }
        }

        try {
            addExtensions(certBuilder, certprofile, requestedSubject, grantedSubject, extensions,
                    tmpPublicKeyInfo, publicCaInfo, notBefore, notAfter);

            ConcurrentBagEntrySigner signer0 = signer.borrowContentSigner();
            X509CertificateHolder certHolder;
            try {
                certHolder = certBuilder.build(signer0.value());
            } finally {
                signer.requiteContentSigner(signer0);
            }
            Certificate bcCert = certHolder.toASN1Structure();
            return X509Util.parseCert(bcCert.getEncoded());
        } catch (BadCertTemplateException ex) {
            throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE, ex);
        } catch (NoIdleSignerException | CertificateException | IOException
                | CertprofileException | NoSuchAlgorithmException ex) {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, ex);
        }
    } // method generateCertificate

    private static void addExtensions(final X509v3CertificateBuilder certBuilder,
            final IdentifiedX509Certprofile profile, final X500Name requestedSubject,
            final X500Name grantedSubject, final Extensions extensions,
            final SubjectPublicKeyInfo requestedPublicKeyInfo, final PublicCaInfo publicCaInfo,
            final Date notBefore, final Date notAfter)
            throws CertprofileException, IOException, BadCertTemplateException,
                NoSuchAlgorithmException {
        ExtensionValues extensionTuples = profile.getExtensions(requestedSubject, grantedSubject,
                extensions, requestedPublicKeyInfo, publicCaInfo, null, notBefore, notAfter);
        if (extensionTuples == null) {
            return;
        }

        for (ASN1ObjectIdentifier extType : extensionTuples.extensionTypes()) {
            ExtensionValue extValue = extensionTuples.getExtensionValue(extType);
            certBuilder.addExtension(extType, extValue.isCritical(), extValue.value());
        }
    } // method addExtensions

    public static AsymmetricKeyParameter generatePublicKeyParameter(final PublicKey key)
            throws InvalidKeyException {
        ParamUtil.requireNonNull("key", key);
        if (key instanceof RSAPublicKey) {
            RSAPublicKey rsakey = (RSAPublicKey) key;
            return new RSAKeyParameters(false, rsakey.getModulus(), rsakey.getPublicExponent());
        } else if (key instanceof ECPublicKey) {
            return ECUtil.generatePublicKeyParameter(key);
        } else if (key instanceof DSAPublicKey) {
            return DSAUtil.generatePublicKeyParameter(key);
        } else {
            throw new InvalidKeyException("unknown key " + key.getClass().getName());
        }
    } // method generatePublicKeyParameter

}
