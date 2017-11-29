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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.cmp.CMPCertificate;
import org.bouncycastle.asn1.cmp.CMPObjectIdentifiers;
import org.bouncycastle.asn1.cmp.CertRepMessage;
import org.bouncycastle.asn1.cmp.CertResponse;
import org.bouncycastle.asn1.cmp.CertifiedKeyPair;
import org.bouncycastle.asn1.cmp.ErrorMsgContent;
import org.bouncycastle.asn1.cmp.GenRepContent;
import org.bouncycastle.asn1.cmp.InfoTypeAndValue;
import org.bouncycastle.asn1.cmp.PKIBody;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.cmp.PKIFreeText;
import org.bouncycastle.asn1.cmp.PKIHeader;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.cmp.PKIStatusInfo;
import org.bouncycastle.asn1.cmp.RevDetails;
import org.bouncycastle.asn1.cmp.RevRepContent;
import org.bouncycastle.asn1.cmp.RevReqContent;
import org.bouncycastle.asn1.crmf.AttributeTypeAndValue;
import org.bouncycastle.asn1.crmf.CertId;
import org.bouncycastle.asn1.crmf.CertReqMessages;
import org.bouncycastle.asn1.crmf.CertReqMsg;
import org.bouncycastle.asn1.crmf.CertRequest;
import org.bouncycastle.asn1.crmf.CertTemplateBuilder;
import org.bouncycastle.asn1.crmf.ProofOfPossession;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.cmp.CMPException;
import org.bouncycastle.cert.cmp.CertificateConfirmationContent;
import org.bouncycastle.cert.cmp.CertificateConfirmationContentBuilder;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xipki.ca.client.api.CertprofileInfo;
import org.xipki.ca.client.api.PkiErrorException;
import org.xipki.ca.client.api.dto.CsrEnrollCertRequest;
import org.xipki.ca.client.api.dto.EnrollCertRequest;
import org.xipki.ca.client.api.dto.EnrollCertRequestEntry;
import org.xipki.ca.client.api.dto.EnrollCertResultEntry;
import org.xipki.ca.client.api.dto.EnrollCertResultResp;
import org.xipki.ca.client.api.dto.ErrorResultEntry;
import org.xipki.ca.client.api.dto.IssuerSerialEntry;
import org.xipki.ca.client.api.dto.ResultEntry;
import org.xipki.ca.client.api.dto.RevokeCertRequest;
import org.xipki.ca.client.api.dto.RevokeCertRequestEntry;
import org.xipki.ca.client.api.dto.RevokeCertResultEntry;
import org.xipki.ca.client.api.dto.RevokeCertResultType;
import org.xipki.ca.client.api.dto.UnrevokeOrRemoveCertEntry;
import org.xipki.ca.client.api.dto.UnrevokeOrRemoveCertRequest;
import org.xipki.cmp.CmpUtf8Pairs;
import org.xipki.cmp.CmpUtil;
import org.xipki.cmp.PkiResponse;
import org.xipki.common.RequestResponseDebug;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.DateUtil;
import org.xipki.common.util.LogUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.common.util.StringUtil;
import org.xipki.common.util.XmlUtil;
import org.xipki.security.ConcurrentContentSigner;
import org.xipki.security.CrlReason;
import org.xipki.security.ObjectIdentifiers;
import org.xipki.security.SecurityFactory;
import org.xipki.security.XiSecurityConstants;
import org.xipki.security.util.X509Util;
import org.xml.sax.SAXException;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

abstract class X509CmpRequestor extends CmpRequestor {

    private static final DigestCalculatorProvider DIGEST_CALCULATOR_PROVIDER
            = new BcDigestCalculatorProvider();

    private static final BigInteger MINUS_ONE = BigInteger.valueOf(-1);

    private static final Logger LOG = LoggerFactory.getLogger(X509CmpRequestor.class);

    private final DocumentBuilder xmlDocBuilder;

    private boolean implicitConfirm = true;

    X509CmpRequestor(final X509Certificate requestorCert, final CmpResponder responder,
            final SecurityFactory securityFactory) {
        super(requestorCert, responder, securityFactory);
        xmlDocBuilder = newDocumentBuilder();
    }

    X509CmpRequestor(final ConcurrentContentSigner requestor, final CmpResponder responder,
            final SecurityFactory securityFactory) {
        super(requestor, responder, securityFactory);
        xmlDocBuilder = newDocumentBuilder();
    }

    public X509CRL generateCrl(final RequestResponseDebug debug)
            throws CmpRequestorException, PkiErrorException {
        int action = XiSecurityConstants.CMP_ACTION_GEN_CRL;
        PKIMessage request = buildMessageWithXipkAction(action, null);
        PkiResponse response = signAndSend(request, debug);
        return evaluateCrlResponse(response, action);
    }

    public X509CRL downloadCurrentCrl(final RequestResponseDebug debug)
            throws CmpRequestorException, PkiErrorException {
        return downloadCrl((BigInteger) null, debug);
    }

    public X509CRL downloadCrl(final BigInteger crlNumber, final RequestResponseDebug debug)
            throws CmpRequestorException, PkiErrorException {
        Integer action = null;
        PKIMessage request;
        if (crlNumber == null) {
            ASN1ObjectIdentifier type = CMPObjectIdentifiers.it_currentCRL;
            request = buildMessageWithGeneralMsgContent(type, null);
        } else {
            action = XiSecurityConstants.CMP_ACTION_GET_CRL_WITH_SN;
            request = buildMessageWithXipkAction(action, new ASN1Integer(crlNumber));
        }

        PkiResponse response = signAndSend(request, debug);
        return evaluateCrlResponse(response, action);
    }

    private X509CRL evaluateCrlResponse(final PkiResponse response, final Integer xipkiAction)
            throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("response", response);

        checkProtection(response);

        PKIBody respBody = response.pkiMessage().getBody();
        int bodyType = respBody.getType();

        if (PKIBody.TYPE_ERROR == bodyType) {
            ErrorMsgContent content = ErrorMsgContent.getInstance(respBody.getContent());
            throw new PkiErrorException(content.getPKIStatusInfo());
        } else if (PKIBody.TYPE_GEN_REP != bodyType) {
            throw new CmpRequestorException(String.format(
                    "unknown PKI body type %s instead the expected [%s, %s]",
                    bodyType, PKIBody.TYPE_GEN_REP, PKIBody.TYPE_ERROR));
        }

        ASN1ObjectIdentifier expectedType = (xipkiAction == null)
                ? CMPObjectIdentifiers.it_currentCRL : ObjectIdentifiers.id_xipki_cmp_cmpGenmsg;

        GenRepContent genRep = GenRepContent.getInstance(respBody.getContent());

        InfoTypeAndValue[] itvs = genRep.toInfoTypeAndValueArray();
        InfoTypeAndValue itv = null;
        if (itvs != null && itvs.length > 0) {
            for (InfoTypeAndValue m : itvs) {
                if (expectedType.equals(m.getInfoType())) {
                    itv = m;
                    break;
                }
            }
        }

        if (itv == null) {
            throw new CmpRequestorException("the response does not contain InfoTypeAndValue "
                    + expectedType);
        }

        ASN1Encodable certListAsn1Object = (xipkiAction == null) ? itv.getInfoValue()
                : extractXipkiActionContent(itv.getInfoValue(), xipkiAction);

        CertificateList certList = CertificateList.getInstance(certListAsn1Object);

        X509CRL crl;
        try {
            crl = X509Util.toX509Crl(certList);
        } catch (CRLException | CertificateException ex) {
            throw new CmpRequestorException("returned CRL is invalid: " + ex.getMessage());
        }

        return crl;
    } // method evaluateCrlResponse

    public RevokeCertResultType revokeCertificate(final RevokeCertRequest request,
            final RequestResponseDebug debug) throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("request", request);

        PKIMessage reqMessage = buildRevokeCertRequest(request);
        PkiResponse response = signAndSend(reqMessage, debug);
        return parse(response, request.requestEntries());
    }

    public RevokeCertResultType unrevokeCertificate(final UnrevokeOrRemoveCertRequest request,
            final RequestResponseDebug debug) throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("request", request);

        PKIMessage reqMessage = buildUnrevokeOrRemoveCertRequest(request,
                CrlReason.REMOVE_FROM_CRL.code());
        PkiResponse response = signAndSend(reqMessage, debug);
        return parse(response, request.requestEntries());
    }

    public RevokeCertResultType removeCertificate(final UnrevokeOrRemoveCertRequest request,
            final RequestResponseDebug debug) throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("request", request);

        PKIMessage reqMessage = buildUnrevokeOrRemoveCertRequest(request,
                XiSecurityConstants.CMP_CRL_REASON_REMOVE);
        PkiResponse response = signAndSend(reqMessage, debug);
        return parse(response, request.requestEntries());
    }

    private RevokeCertResultType parse(final PkiResponse response,
            final List<? extends IssuerSerialEntry> reqEntries)
            throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("response", response);

        checkProtection(response);

        PKIBody respBody = response.pkiMessage().getBody();
        int bodyType = respBody.getType();

        if (PKIBody.TYPE_ERROR == bodyType) {
            ErrorMsgContent content = ErrorMsgContent.getInstance(respBody.getContent());
            throw new PkiErrorException(content.getPKIStatusInfo());
        } else if (PKIBody.TYPE_REVOCATION_REP != bodyType) {
            throw new CmpRequestorException(String.format(
                    "unknown PKI body type %s instead the expected [%s, %s]", bodyType,
                    PKIBody.TYPE_REVOCATION_REP, PKIBody.TYPE_ERROR));
        }

        RevRepContent content = RevRepContent.getInstance(respBody.getContent());
        PKIStatusInfo[] statuses = content.getStatus();
        if (statuses == null || statuses.length != reqEntries.size()) {
            int statusesLen = 0;
            if (statuses != null) {
                statusesLen = statuses.length;
            }

            throw new CmpRequestorException(String.format(
                "incorrect number of status entries in response '%s' instead the expected '%s'",
                statusesLen, reqEntries.size()));
        }

        CertId[] revCerts = content.getRevCerts();

        RevokeCertResultType result = new RevokeCertResultType();
        for (int i = 0; i < statuses.length; i++) {
            PKIStatusInfo statusInfo = statuses[i];
            int status = statusInfo.getStatus().intValue();
            IssuerSerialEntry re = reqEntries.get(i);

            if (status != PKIStatus.GRANTED && status != PKIStatus.GRANTED_WITH_MODS) {
                PKIFreeText text = statusInfo.getStatusString();
                String statusString = (text == null) ? null : text.getStringAt(0).getString();

                ResultEntry resultEntry = new ErrorResultEntry(re.id(), status,
                        statusInfo.getFailInfo().intValue(), statusString);
                result.addResultEntry(resultEntry);
                continue;
            }

            CertId certId = null;
            if (revCerts != null) {
                for (CertId entry : revCerts) {
                    if (re.issuer().equals(entry.getIssuer().getName())
                            && re.serialNumber().equals(entry.getSerialNumber().getValue())) {
                        certId = entry;
                        break;
                    }
                }
            }

            if (certId == null) {
                LOG.warn("certId is not present in response for (issuer='{}', serialNumber={})",
                        X509Util.getRfc4519Name(re.issuer()),
                        LogUtil.formatCsn(re.serialNumber()));
                certId = new CertId(new GeneralName(re.issuer()), re.serialNumber());
                continue;
            }

            ResultEntry resultEntry = new RevokeCertResultEntry(re.id(), certId);
            result.addResultEntry(resultEntry);
        }

        return result;
    } // method parse

    public EnrollCertResultResp requestCertificate(final CsrEnrollCertRequest csr,
            final Date notBefore, final Date notAfter,
            final RequestResponseDebug debug) throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("csr", csr);

        PKIMessage request = buildPkiMessage(csr, notBefore, notAfter);
        Map<BigInteger, String> reqIdIdMap = new HashMap<>();
        reqIdIdMap.put(MINUS_ONE, csr.id());
        return requestCertificate0(request, reqIdIdMap, PKIBody.TYPE_CERT_REP, debug);
    }

    public EnrollCertResultResp requestCertificate(final EnrollCertRequest req,
            final RequestResponseDebug debug)
            throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("req", req);

        PKIMessage request = buildPkiMessage(req);
        Map<BigInteger, String> reqIdIdMap = new HashMap<>();
        List<EnrollCertRequestEntry> reqEntries = req.requestEntries();

        for (EnrollCertRequestEntry reqEntry : reqEntries) {
            reqIdIdMap.put(reqEntry.certReq().getCertReqId().getValue(), reqEntry.id());
        }

        int exptectedBodyType;
        switch (req.type()) {
        case CERT_REQ:
            exptectedBodyType = PKIBody.TYPE_CERT_REP;
            break;
        case KEY_UPDATE:
            exptectedBodyType = PKIBody.TYPE_KEY_UPDATE_REP;
            break;
        default:
            exptectedBodyType = PKIBody.TYPE_CROSS_CERT_REP;
        }

        return requestCertificate0(request, reqIdIdMap, exptectedBodyType, debug);
    }

    private EnrollCertResultResp requestCertificate0(final PKIMessage reqMessage,
            final Map<BigInteger, String> reqIdIdMap, final int expectedBodyType,
            final RequestResponseDebug debug) throws CmpRequestorException, PkiErrorException {
        PkiResponse response = signAndSend(reqMessage, debug);
        checkProtection(response);

        PKIBody respBody = response.pkiMessage().getBody();
        final int bodyType = respBody.getType();

        if (PKIBody.TYPE_ERROR == bodyType) {
            ErrorMsgContent content = ErrorMsgContent.getInstance(respBody.getContent());
            throw new PkiErrorException(content.getPKIStatusInfo());
        } else if (expectedBodyType != bodyType) {
            throw new CmpRequestorException(String.format(
                    "unknown PKI body type %s instead the expected [%s, %s]", bodyType,
                    expectedBodyType, PKIBody.TYPE_ERROR));
        }

        CertRepMessage certRep = CertRepMessage.getInstance(respBody.getContent());
        CertResponse[] certResponses = certRep.getResponse();

        EnrollCertResultResp result = new EnrollCertResultResp();

        // CA certificates
        CMPCertificate[] caPubs = certRep.getCaPubs();
        if (caPubs != null && caPubs.length > 0) {
            for (int i = 0; i < caPubs.length; i++) {
                if (caPubs[i] != null) {
                    result.addCaCertificate(caPubs[i]);
                }
            }
        }

        CertificateConfirmationContentBuilder certConfirmBuilder = null;
        if (!CmpUtil.isImplictConfirm(response.pkiMessage().getHeader())) {
            certConfirmBuilder = new CertificateConfirmationContentBuilder();
        }
        boolean requireConfirm = false;

        // We only accept the certificates which are requested.
        for (CertResponse certResp : certResponses) {
            PKIStatusInfo statusInfo = certResp.getStatus();
            int status = statusInfo.getStatus().intValue();
            BigInteger certReqId = certResp.getCertReqId().getValue();
            String thisId = reqIdIdMap.get(certReqId);
            if (thisId != null) {
                reqIdIdMap.remove(certReqId);
            } else if (reqIdIdMap.size() == 1) {
                thisId = reqIdIdMap.values().iterator().next();
                reqIdIdMap.clear();
            }

            if (thisId == null) {
                continue; // ignore it. this cert is not requested by me
            }

            ResultEntry resultEntry;
            if (status == PKIStatus.GRANTED || status == PKIStatus.GRANTED_WITH_MODS) {
                CertifiedKeyPair cvk = certResp.getCertifiedKeyPair();
                if (cvk == null) {
                    return null;
                }

                CMPCertificate cmpCert = cvk.getCertOrEncCert().getCertificate();
                if (cmpCert == null) {
                    return null;
                }

                resultEntry = new EnrollCertResultEntry(thisId, cmpCert, status);

                if (certConfirmBuilder != null) {
                    requireConfirm = true;
                    X509CertificateHolder certHolder = null;
                    try {
                        certHolder = new X509CertificateHolder(cmpCert.getEncoded());
                    } catch (IOException ex) {
                        resultEntry = new ErrorResultEntry(thisId,
                                ClientErrorCode.PKISTATUS_RESPONSE_ERROR,
                                PKIFailureInfo.systemFailure, "could not decode the certificate");
                    }

                    if (certHolder != null) {
                        certConfirmBuilder.addAcceptedCertificate(certHolder, certReqId);
                    }
                }
            } else {
                PKIFreeText statusString = statusInfo.getStatusString();
                String errorMessage = (statusString == null) ? null
                        : statusString.getStringAt(0).getString();
                int failureInfo = statusInfo.getFailInfo().intValue();

                resultEntry = new ErrorResultEntry(thisId, status, failureInfo, errorMessage);
            }
            result.addResultEntry(resultEntry);
        }

        if (CollectionUtil.isNonEmpty(reqIdIdMap)) {
            for (BigInteger reqId : reqIdIdMap.keySet()) {
                ErrorResultEntry ere = new ErrorResultEntry(reqIdIdMap.get(reqId),
                        ClientErrorCode.PKISTATUS_NO_ANSWER);
                result.addResultEntry(ere);
            }
        }

        if (!requireConfirm) {
            return result;
        }

        PKIMessage confirmRequest = buildCertConfirmRequest(
                response.pkiMessage().getHeader().getTransactionID(), certConfirmBuilder);

        response = signAndSend(confirmRequest, debug);
        checkProtection(response);

        return result;
    } // method requestCertificate0

    private PKIMessage buildCertConfirmRequest(final ASN1OctetString tid,
            final CertificateConfirmationContentBuilder certConfirmBuilder)
            throws CmpRequestorException {
        PKIHeader header = buildPkiHeader(implicitConfirm, tid, null, (InfoTypeAndValue[]) null);
        CertificateConfirmationContent certConfirm;
        try {
            certConfirm = certConfirmBuilder.build(DIGEST_CALCULATOR_PROVIDER);
        } catch (CMPException ex) {
            throw new CmpRequestorException(ex.getMessage(), ex);
        }
        PKIBody body = new PKIBody(PKIBody.TYPE_CERT_CONFIRM, certConfirm.toASN1Structure());
        return new PKIMessage(header, body);
    }

    private PKIMessage buildRevokeCertRequest(final RevokeCertRequest request)
            throws CmpRequestorException {
        PKIHeader header = buildPkiHeader(null);

        List<RevokeCertRequestEntry> requestEntries = request.requestEntries();
        List<RevDetails> revDetailsArray = new ArrayList<>(requestEntries.size());
        for (RevokeCertRequestEntry requestEntry : requestEntries) {
            CertTemplateBuilder certTempBuilder = new CertTemplateBuilder();
            certTempBuilder.setIssuer(requestEntry.issuer());
            certTempBuilder.setSerialNumber(new ASN1Integer(requestEntry.serialNumber()));
            byte[] aki = requestEntry.authorityKeyIdentifier();
            if (aki != null) {
                Extensions certTempExts = getCertTempExtensions(aki);
                certTempBuilder.setExtensions(certTempExts);
            }

            Date invalidityDate = requestEntry.invalidityDate();
            int idx = (invalidityDate == null) ? 1 : 2;
            Extension[] extensions = new Extension[idx];

            try {
                ASN1Enumerated reason = new ASN1Enumerated(requestEntry.reason());
                extensions[0] = new Extension(Extension.reasonCode, true,
                        new DEROctetString(reason.getEncoded()));

                if (invalidityDate != null) {
                    ASN1GeneralizedTime time = new ASN1GeneralizedTime(invalidityDate);
                    extensions[1] = new Extension(Extension.invalidityDate, true,
                            new DEROctetString(time.getEncoded()));
                }
            } catch (IOException ex) {
                throw new CmpRequestorException(ex.getMessage(), ex);
            }
            Extensions exts = new Extensions(extensions);

            RevDetails revDetails = new RevDetails(certTempBuilder.build(), exts);
            revDetailsArray.add(revDetails);
        }

        RevReqContent content = new RevReqContent(revDetailsArray.toArray(new RevDetails[0]));
        PKIBody body = new PKIBody(PKIBody.TYPE_REVOCATION_REQ, content);
        return new PKIMessage(header, body);
    } // method buildRevokeCertRequest

    private PKIMessage buildUnrevokeOrRemoveCertRequest(final UnrevokeOrRemoveCertRequest request,
            final int reasonCode) throws CmpRequestorException {
        PKIHeader header = buildPkiHeader(null);

        List<UnrevokeOrRemoveCertEntry> requestEntries = request.requestEntries();
        List<RevDetails> revDetailsArray = new ArrayList<>(requestEntries.size());
        for (UnrevokeOrRemoveCertEntry requestEntry : requestEntries) {
            CertTemplateBuilder certTempBuilder = new CertTemplateBuilder();
            certTempBuilder.setIssuer(requestEntry.issuer());
            certTempBuilder.setSerialNumber(new ASN1Integer(requestEntry.serialNumber()));
            byte[] aki = requestEntry.authorityKeyIdentifier();
            if (aki != null) {
                Extensions certTempExts = getCertTempExtensions(aki);
                certTempBuilder.setExtensions(certTempExts);
            }

            Extension[] extensions = new Extension[1];

            try {
                ASN1Enumerated reason = new ASN1Enumerated(reasonCode);
                extensions[0] = new Extension(Extension.reasonCode, true,
                        new DEROctetString(reason.getEncoded()));
            } catch (IOException ex) {
                throw new CmpRequestorException(ex.getMessage(), ex);
            }
            Extensions exts = new Extensions(extensions);

            RevDetails revDetails = new RevDetails(certTempBuilder.build(), exts);
            revDetailsArray.add(revDetails);
        }

        RevReqContent content = new RevReqContent(revDetailsArray.toArray(new RevDetails[0]));
        PKIBody body = new PKIBody(PKIBody.TYPE_REVOCATION_REQ, content);
        return new PKIMessage(header, body);
    } // method buildUnrevokeOrRemoveCertRequest

    private PKIMessage buildPkiMessage(final CsrEnrollCertRequest csr,
            final Date notBefore, final Date notAfter) {
        CmpUtf8Pairs utf8Pairs = new CmpUtf8Pairs(CmpUtf8Pairs.KEY_CERT_PROFILE,
                csr.certprofile());

        if (notBefore != null) {
            utf8Pairs.putUtf8Pair(CmpUtf8Pairs.KEY_NOT_BEFORE,
                    DateUtil.toUtcTimeyyyyMMddhhmmss(notBefore));
        }
        if (notAfter != null) {
            utf8Pairs.putUtf8Pair(CmpUtf8Pairs.KEY_NOT_AFTER,
                    DateUtil.toUtcTimeyyyyMMddhhmmss(notAfter));
        }

        PKIHeader header = buildPkiHeader(implicitConfirm, null, utf8Pairs);
        PKIBody body = new PKIBody(PKIBody.TYPE_P10_CERT_REQ, csr.csr());

        return new PKIMessage(header, body);
    }

    private PKIMessage buildPkiMessage(final EnrollCertRequest req) {
        PKIHeader header = buildPkiHeader(implicitConfirm, null);

        List<EnrollCertRequestEntry> reqEntries = req.requestEntries();
        CertReqMsg[] certReqMsgs = new CertReqMsg[reqEntries.size()];

        for (int i = 0; i < reqEntries.size(); i++) {
            EnrollCertRequestEntry reqEntry = reqEntries.get(i);
            CmpUtf8Pairs utf8Pairs = new CmpUtf8Pairs(CmpUtf8Pairs.KEY_CERT_PROFILE,
                    reqEntry.certprofile());
            AttributeTypeAndValue certprofileInfo = CmpUtil.buildAttributeTypeAndValue(utf8Pairs);

            AttributeTypeAndValue[] atvs = (certprofileInfo == null) ? null
                    : new AttributeTypeAndValue[]{certprofileInfo};
            certReqMsgs[i] = new CertReqMsg(reqEntry.certReq(), reqEntry.popo(), atvs);
        }

        int bodyType;
        switch (req.type()) {
        case CERT_REQ:
            bodyType = PKIBody.TYPE_CERT_REQ;
            break;
        case KEY_UPDATE:
            bodyType = PKIBody.TYPE_KEY_UPDATE_REQ;
            break;
        default:
            bodyType = PKIBody.TYPE_CROSS_CERT_REQ;
        }

        PKIBody body = new PKIBody(bodyType, new CertReqMessages(certReqMsgs));
        return new PKIMessage(header, body);
    } // method buildPkiMessage

    private PKIMessage buildPkiMessage(final CertRequest req, final ProofOfPossession pop,
            final String profileName) {
        PKIHeader header = buildPkiHeader(implicitConfirm, null);

        CmpUtf8Pairs utf8Pairs = new CmpUtf8Pairs(CmpUtf8Pairs.KEY_CERT_PROFILE, profileName);
        AttributeTypeAndValue certprofileInfo = CmpUtil.buildAttributeTypeAndValue(utf8Pairs);
        CertReqMsg[] certReqMsgs = new CertReqMsg[1];
        certReqMsgs[0] = new CertReqMsg(req, pop, new AttributeTypeAndValue[]{certprofileInfo});

        PKIBody body = new PKIBody(PKIBody.TYPE_CERT_REQ, new CertReqMessages(certReqMsgs));
        return new PKIMessage(header, body);
    }

    public PKIMessage envelope(final CertRequest req, final ProofOfPossession pop,
            final String profileName) throws CmpRequestorException {
        ParamUtil.requireNonNull("req", req);
        ParamUtil.requireNonNull("pop", pop);
        ParamUtil.requireNonNull("profileName", profileName);

        PKIMessage request = buildPkiMessage(req, pop, profileName);
        return sign(request);
    }

    public PKIMessage envelopeRevocation(final RevokeCertRequest request)
            throws CmpRequestorException {
        ParamUtil.requireNonNull("request", request);

        PKIMessage reqMessage = buildRevokeCertRequest(request);
        reqMessage = sign(reqMessage);
        return reqMessage;
    }

    public CaInfo retrieveCaInfo(final String caName, final RequestResponseDebug debug)
            throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonBlank("caName", caName);

        ASN1EncodableVector vec = new ASN1EncodableVector();
        vec.add(new ASN1Integer(2));
        ASN1Sequence acceptVersions = new DERSequence(vec);

        int action = XiSecurityConstants.CMP_ACTION_GET_CAINFO;
        PKIMessage request = buildMessageWithXipkAction(action, acceptVersions);
        PkiResponse response = signAndSend(request, debug);
        ASN1Encodable itvValue = extractXipkiActionRepContent(response, action);
        DERUTF8String utf8Str = DERUTF8String.getInstance(itvValue);
        String systemInfoStr = utf8Str.getString();

        LOG.debug("CAInfo for CA {}: {}", caName, systemInfoStr);
        Document doc;
        try {
            doc = xmlDocBuilder.parse(new ByteArrayInputStream(systemInfoStr.getBytes("UTF-8")));
        } catch (SAXException | IOException ex) {
            throw new CmpRequestorException("could not parse the returned systemInfo for CA "
                    + caName + ": " + ex.getMessage(), ex);
        }

        final String namespace = null;
        Element root = doc.getDocumentElement();
        String str = root.getAttribute("version");
        if (StringUtil.isBlank(str)) {
            str = root.getAttributeNS(namespace, "version");
        }

        int version = StringUtil.isBlank(str) ? 1 : Integer.parseInt(str);

        if (version == 2) {
            // CACert
            X509Certificate caCert;
            String b64CaCert = XmlUtil.getValueOfFirstElementChild(root, namespace, "CACert");
            try {
                caCert = X509Util.parseBase64EncodedCert(b64CaCert);
            } catch (CertificateException ex) {
                throw new CmpRequestorException("could no parse the CA certificate", ex);
            }

            // CmpControl
            ClientCmpControl cmpControl = null;
            Element cmpCtrlElement = XmlUtil.getFirstElementChild(root, namespace, "cmpControl");
            if (cmpCtrlElement != null) {
                String tmpStr = XmlUtil.getValueOfFirstElementChild(cmpCtrlElement, namespace,
                        "rrAkiRequired");
                boolean required = (tmpStr == null) ? false : Boolean.parseBoolean(tmpStr);
                cmpControl = new ClientCmpControl(required);
            }

            // certprofiles
            Set<String> profileNames = new HashSet<>();
            Element profilesElement = XmlUtil.getFirstElementChild(root, namespace, "certprofiles");
            Set<CertprofileInfo> profiles = new HashSet<>();
            if (profilesElement != null) {
                List<Element> profileElements = XmlUtil.getElementChilden(profilesElement,
                        namespace, "certprofile");

                for (Element element : profileElements) {
                    String name = XmlUtil.getValueOfFirstElementChild(element, namespace, "name");
                    String type = XmlUtil.getValueOfFirstElementChild(element, namespace, "type");
                    String conf = XmlUtil.getValueOfFirstElementChild(element, namespace, "conf");
                    CertprofileInfo profile = new CertprofileInfo(name, type, conf);
                    profiles.add(profile);
                    profileNames.add(name);
                    if (LOG.isDebugEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("configured for CA ").append(caName).append(" certprofile (");
                        sb.append("name=").append(name).append(", ");
                        sb.append("type=").append(type).append(", ");
                        sb.append("conf=").append(conf).append(")");
                        LOG.debug(sb.toString());
                    }
                }
            }

            LOG.info("CA {} supports profiles {}", caName, profileNames);
            return new CaInfo(caCert, cmpControl, profiles);
        } else {
            throw new CmpRequestorException("unknown CAInfo version " + version);
        }
    } // method retrieveCaInfo

    private static DocumentBuilder newDocumentBuilder() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("could not create XML document builder", ex);
        }
    }

    private static Extensions getCertTempExtensions(byte[] authorityKeyIdentifier)
            throws CmpRequestorException {
        AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(authorityKeyIdentifier);
        byte[] encodedAki;
        try {
            encodedAki = aki.getEncoded();
        } catch (IOException ex) {
            throw new CmpRequestorException("could not encoded AuthorityKeyIdentifier", ex);
        }
        Extension extAki = new Extension(Extension.authorityKeyIdentifier, false, encodedAki);
        Extensions certTempExts = new Extensions(extAki);
        return certTempExts;
    }

}
