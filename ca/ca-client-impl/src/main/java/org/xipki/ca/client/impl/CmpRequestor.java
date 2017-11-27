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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.cmp.CMPObjectIdentifiers;
import org.bouncycastle.asn1.cmp.ErrorMsgContent;
import org.bouncycastle.asn1.cmp.GenMsgContent;
import org.bouncycastle.asn1.cmp.GenRepContent;
import org.bouncycastle.asn1.cmp.InfoTypeAndValue;
import org.bouncycastle.asn1.cmp.PKIBody;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.cmp.PKIHeader;
import org.bouncycastle.asn1.cmp.PKIHeaderBuilder;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.cmp.CMPException;
import org.bouncycastle.cert.cmp.GeneralPKIMessage;
import org.bouncycastle.cert.cmp.ProtectedPKIMessage;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.client.api.PkiErrorException;
import org.xipki.ca.common.cmp.CmpUtf8Pairs;
import org.xipki.ca.common.cmp.CmpUtil;
import org.xipki.ca.common.cmp.PkiResponse;
import org.xipki.ca.common.cmp.ProtectionResult;
import org.xipki.ca.common.cmp.ProtectionVerificationResult;
import org.xipki.common.RequestResponseDebug;
import org.xipki.common.RequestResponsePair;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.ConcurrentContentSigner;
import org.xipki.security.ObjectIdentifiers;
import org.xipki.security.SecurityFactory;
import org.xipki.security.exception.NoIdleSignerException;
import org.xipki.security.util.AlgorithmUtil;
import org.xipki.security.util.CmpFailureUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

abstract class CmpRequestor {

    private static final Logger LOG = LoggerFactory.getLogger(CmpRequestor.class);

    protected final SecurityFactory securityFactory;

    private final Random random = new Random();

    private final ConcurrentContentSigner requestor;

    private final GeneralName sender;

    private final CmpResponder responder;

    private final GeneralName recipient;

    private final X500Name recipientName;

    private boolean signRequest;

    private boolean sendRequestorCert;

    public CmpRequestor(final X509Certificate requestorCert, final CmpResponder responder,
            final SecurityFactory securityFactory) {
        ParamUtil.requireNonNull("requestorCert", requestorCert);
        this.responder = ParamUtil.requireNonNull("responder", responder);
        this.securityFactory = ParamUtil.requireNonNull("securityFactory", securityFactory);
        this.requestor = null;

        X500Name x500Name = X500Name.getInstance(
                requestorCert.getSubjectX500Principal().getEncoded());
        this.sender = new GeneralName(x500Name);

        X500Name subject = X500Name.getInstance(
                responder.cert().getSubjectX500Principal().getEncoded());
        this.recipient = new GeneralName(subject);
        this.recipientName = subject;
    }

    public CmpRequestor(final ConcurrentContentSigner requestor,
            final CmpResponder responder, final SecurityFactory securityFactory) {
        this.requestor = ParamUtil.requireNonNull("requestor", requestor);
        if (requestor.getCertificate() == null) {
            throw new IllegalArgumentException("requestor without certificate is not allowed");
        }
        this.responder = ParamUtil.requireNonNull("responder", responder);
        this.securityFactory = ParamUtil.requireNonNull("securityFactory", securityFactory);

        X500Name x500Name = X500Name.getInstance(
                requestor.getCertificate().getSubjectX500Principal().getEncoded());
        this.sender = new GeneralName(x500Name);

        X500Name subject = X500Name.getInstance(
                responder.cert().getSubjectX500Principal().getEncoded());
        this.recipient = new GeneralName(subject);
        this.recipientName = subject;
    }

    protected abstract byte[] send(final byte[] request) throws IOException;

    protected PKIMessage sign(final PKIMessage request) throws CmpRequestorException {
        ParamUtil.requireNonNull("request", request);
        if (requestor == null) {
            throw new CmpRequestorException("no request signer is configured");
        }

        try {
            return CmpUtil.addProtection(request, requestor, sender, sendRequestorCert);
        } catch (CMPException | NoIdleSignerException ex) {
            throw new CmpRequestorException("could not sign the request", ex);
        }
    }

    protected PkiResponse signAndSend(final PKIMessage request, final RequestResponseDebug debug)
            throws CmpRequestorException {
        ParamUtil.requireNonNull("request", request);

        PKIMessage tmpRequest = (signRequest) ? sign(request) : request;

        byte[] encodedRequest;
        try {
            encodedRequest = tmpRequest.getEncoded();
        } catch (IOException ex) {
            LOG.error("could not encode the PKI request {}", tmpRequest);
            throw new CmpRequestorException(ex.getMessage(), ex);
        }

        RequestResponsePair reqResp = null;
        if (debug != null) {
            reqResp = new RequestResponsePair();
            debug.add(reqResp);
            reqResp.setRequest(encodedRequest);
        }

        byte[] encodedResponse;
        try {
            encodedResponse = send(encodedRequest);
        } catch (IOException ex) {
            LOG.error("could not send the PKI request {} to server", tmpRequest);
            throw new CmpRequestorException("TRANSPORT_ERROR", ex);
        }

        if (reqResp != null) {
            reqResp.setResponse(encodedResponse);
        }

        GeneralPKIMessage response;
        try {
            response = new GeneralPKIMessage(encodedResponse);
        } catch (IOException ex) {
            LOG.error("could not decode the received PKI message: {}",
                    Hex.toHexString(encodedResponse));
            throw new CmpRequestorException(ex.getMessage(), ex);
        }

        PKIHeader reqHeader = request.getHeader();
        PKIHeader respHeader = response.getHeader();

        ASN1OctetString tid = reqHeader.getTransactionID();
        ASN1OctetString respTid = respHeader.getTransactionID();
        if (!tid.equals(respTid)) {
            LOG.warn("Response contains different tid ({}) than requested {}", respTid, tid);
            throw new CmpRequestorException("Response contains differnt tid than the request");
        }

        ASN1OctetString senderNonce = reqHeader.getSenderNonce();
        ASN1OctetString respRecipientNonce = respHeader.getRecipNonce();
        if (!senderNonce.equals(respRecipientNonce)) {
            LOG.warn("tid {}: response.recipientNonce ({}) != request.senderNonce ({})",
                    tid, respRecipientNonce, senderNonce);
            throw new CmpRequestorException("Response contains differnt tid than the request");
        }

        GeneralName rec = respHeader.getRecipient();
        if (!sender.equals(rec)) {
            LOG.warn("tid={}: unknown CMP requestor '{}'", tid, rec);
        }

        PkiResponse ret = new PkiResponse(response);
        if (response.hasProtection()) {
            try {
                ProtectionVerificationResult verifyProtection = verifyProtection(
                        Hex.toHexString(tid.getOctets()), response);
                ret.setProtectionVerificationResult(verifyProtection);
            } catch (InvalidKeyException | OperatorCreationException | CMPException ex) {
                throw new CmpRequestorException(ex.getMessage(), ex);
            }
        } else if (signRequest) {
            PKIBody respBody = response.getBody();
            int bodyType = respBody.getType();
            if (bodyType != PKIBody.TYPE_ERROR) {
                throw new CmpRequestorException("response is not signed");
            }
        }

        return ret;
    } // method signAndSend

    protected ASN1Encodable extractGeneralRepContent(final PkiResponse response,
            final String expectedType) throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("response", response);
        ParamUtil.requireNonNull("expectedType", expectedType);
        return extractGeneralRepContent(response, expectedType, true);
    }

    private ASN1Encodable extractGeneralRepContent(final PkiResponse response,
            final String expectedType, final boolean requireProtectionCheck)
            throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("response", response);
        ParamUtil.requireNonNull("expectedType", expectedType);
        if (requireProtectionCheck) {
            checkProtection(response);
        }

        PKIBody respBody = response.pkiMessage().getBody();
        int bodyType = respBody.getType();

        if (PKIBody.TYPE_ERROR == bodyType) {
            ErrorMsgContent content = ErrorMsgContent.getInstance(respBody.getContent());
            throw new CmpRequestorException(CmpFailureUtil.formatPkiStatusInfo(
                    content.getPKIStatusInfo()));
        } else if (PKIBody.TYPE_GEN_REP != bodyType) {
            throw new CmpRequestorException(String.format(
                        "unknown PKI body type %s instead the expected [%s, %s]", bodyType,
                        PKIBody.TYPE_GEN_REP, PKIBody.TYPE_ERROR));
        }

        GenRepContent genRep = GenRepContent.getInstance(respBody.getContent());

        InfoTypeAndValue[] itvs = genRep.toInfoTypeAndValueArray();
        InfoTypeAndValue itv = null;
        if (itvs != null && itvs.length > 0) {
            for (InfoTypeAndValue entry : itvs) {
                if (expectedType.equals(entry.getInfoType().getId())) {
                    itv = entry;
                    break;
                }
            }
        }
        if (itv == null) {
            throw new CmpRequestorException("the response does not contain InfoTypeAndValue "
                    + expectedType);
        }

        return itv.getInfoValue();
    } // method extractGeneralRepContent

    protected ASN1Encodable extractXipkiActionRepContent(final PkiResponse response,
            final int action) throws CmpRequestorException, PkiErrorException {
        ParamUtil.requireNonNull("response", response);
        ASN1Encodable itvValue = extractGeneralRepContent(response,
                ObjectIdentifiers.id_xipki_cmp_cmpGenmsg.getId(), true);
        return extractXipkiActionContent(itvValue, action);
    }

    protected ASN1Encodable extractXipkiActionContent(final ASN1Encodable itvValue,
            final int action) throws CmpRequestorException {
        ParamUtil.requireNonNull("itvValue", itvValue);
        ASN1Sequence seq;
        try {
            seq = ASN1Sequence.getInstance(itvValue);
        } catch (IllegalArgumentException ex) {
            throw new CmpRequestorException("invalid syntax of the response");
        }
        int size = seq.size();
        if (size != 1 && size != 2) {
            throw new CmpRequestorException("invalid syntax of the response");
        }

        int tmpAction;
        try {
            tmpAction = ASN1Integer.getInstance(seq.getObjectAt(0)).getPositiveValue().intValue();
        } catch (IllegalArgumentException ex) {
            throw new CmpRequestorException("invalid syntax of the response");
        }

        if (action != tmpAction) {
            throw new CmpRequestorException("received XiPKI action '" + tmpAction
                    + "' instead the expected '" + action + "'");
        }

        return (size == 1) ? null : seq.getObjectAt(1);
    } // method extractXipkiActionContent

    protected PKIHeader buildPkiHeader(final ASN1OctetString tid) {
        return buildPkiHeader(false, tid, (CmpUtf8Pairs) null, (InfoTypeAndValue[]) null);
    }

    protected PKIHeader buildPkiHeader(final boolean addImplictConfirm, final ASN1OctetString tid) {
        return buildPkiHeader(addImplictConfirm, tid, (CmpUtf8Pairs) null,
                (InfoTypeAndValue[]) null);
    }

    protected PKIHeader buildPkiHeader(final boolean addImplictConfirm, final ASN1OctetString tid,
            final CmpUtf8Pairs utf8Pairs, final InfoTypeAndValue... additionalGeneralInfos) {
        if (additionalGeneralInfos != null) {
            for (InfoTypeAndValue itv : additionalGeneralInfos) {
                ASN1ObjectIdentifier type = itv.getInfoType();
                if (CMPObjectIdentifiers.it_implicitConfirm.equals(type)) {
                    throw new IllegalArgumentException(
                            "additionGeneralInfos contains not-permitted ITV implicitConfirm");
                }

                if (CMPObjectIdentifiers.regInfo_utf8Pairs.equals(type)) {
                    throw new IllegalArgumentException(
                            "additionGeneralInfos contains not-permitted ITV utf8Pairs");
                }
            }
        }

        PKIHeaderBuilder hdrBuilder = new PKIHeaderBuilder(PKIHeader.CMP_2000, sender, recipient);
        hdrBuilder.setMessageTime(new ASN1GeneralizedTime(new Date()));

        ASN1OctetString tmpTid = (tid == null) ? new DEROctetString(randomTransactionId()) : tid;
        hdrBuilder.setTransactionID(tmpTid);

        hdrBuilder.setSenderNonce(randomSenderNonce());

        List<InfoTypeAndValue> itvs = new ArrayList<>(2);
        if (addImplictConfirm) {
            itvs.add(CmpUtil.getImplictConfirmGeneralInfo());
        }

        if (utf8Pairs != null) {
            itvs.add(CmpUtil.buildInfoTypeAndValue(utf8Pairs));
        }

        if (additionalGeneralInfos != null) {
            for (InfoTypeAndValue itv : additionalGeneralInfos) {
                if (itv != null) {
                    itvs.add(itv);
                }
            }
        }

        if (CollectionUtil.isNonEmpty(itvs)) {
            hdrBuilder.setGeneralInfo(itvs.toArray(new InfoTypeAndValue[0]));
        }

        return hdrBuilder.build();
    } // method buildPkiHeader

    protected PkiErrorException buildErrorResult(final ErrorMsgContent bodyContent) {
        ParamUtil.requireNonNull("bodyContent", bodyContent);

        org.xipki.ca.common.cmp.PkiStatusInfo statusInfo =
                new org.xipki.ca.common.cmp.PkiStatusInfo(bodyContent.getPKIStatusInfo());
        return new PkiErrorException(statusInfo.status(), statusInfo.pkiFailureInfo(),
                statusInfo.statusMessage());
    }

    private byte[] randomTransactionId() {
        byte[] tid = new byte[20];
        random.nextBytes(tid);
        return tid;
    }

    private byte[] randomSenderNonce() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return bytes;
    }

    private ProtectionVerificationResult verifyProtection(final String tid,
            final GeneralPKIMessage pkiMessage)
            throws CMPException, InvalidKeyException, OperatorCreationException {
        ProtectedPKIMessage protectedMsg = new ProtectedPKIMessage(pkiMessage);

        if (protectedMsg.hasPasswordBasedMacProtection()) {
            LOG.warn("NOT_SIGNAUTRE_BASED: "
                    + pkiMessage.getHeader().getProtectionAlg().getAlgorithm().getId());
            return new ProtectionVerificationResult(null, ProtectionResult.NOT_SIGNATURE_BASED);
        }

        PKIHeader header = protectedMsg.getHeader();

        if (recipientName != null) {
            boolean authorizedResponder = true;
            if (header.getSender().getTagNo() != GeneralName.directoryName) {
                authorizedResponder = false;
            } else {
                X500Name msgSender = X500Name.getInstance(header.getSender().getName());
                authorizedResponder = recipientName.equals(msgSender);
            }

            if (!authorizedResponder) {
                LOG.warn("tid={}: not authorized responder '{}'", tid, header.getSender());
                return new ProtectionVerificationResult(null,
                        ProtectionResult.SENDER_NOT_AUTHORIZED);
            }
        }

        AlgorithmIdentifier protectionAlgo = protectedMsg.getHeader().getProtectionAlg();
        if (!responder.sigAlgoValidator().isAlgorithmPermitted(protectionAlgo)) {
            String algoName;
            try {
                algoName = AlgorithmUtil.getSignatureAlgoName(protectionAlgo);
            } catch (NoSuchAlgorithmException ex) {
                algoName = protectionAlgo.getAlgorithm().getId();
            }
            LOG.warn("tid={}: response protected by untrusted protection algorithm '{}'", tid,
                    algoName);
            return new ProtectionVerificationResult(null, ProtectionResult.INVALID);
        }

        X509Certificate cert = responder.cert();
        ContentVerifierProvider verifierProvider = securityFactory.getContentVerifierProvider(cert);
        if (verifierProvider == null) {
            LOG.warn("tid={}: not authorized responder '{}'", tid, header.getSender());
            return new ProtectionVerificationResult(cert, ProtectionResult.SENDER_NOT_AUTHORIZED);
        }

        boolean signatureValid = protectedMsg.verify(verifierProvider);
        ProtectionResult protRes = signatureValid ? ProtectionResult.VALID
                : ProtectionResult.INVALID;
        return new ProtectionVerificationResult(cert, protRes);
    } // method verifyProtection

    protected PKIMessage buildMessageWithXipkAction(final int action, final ASN1Encodable value)
            throws CmpRequestorException {
        PKIHeader header = buildPkiHeader(null);

        ASN1EncodableVector vec = new ASN1EncodableVector();
        vec.add(new ASN1Integer(action));
        if (value != null) {
            vec.add(value);
        }
        InfoTypeAndValue itv = new InfoTypeAndValue(ObjectIdentifiers.id_xipki_cmp_cmpGenmsg,
                new DERSequence(vec));
        GenMsgContent genMsgContent = new GenMsgContent(itv);
        PKIBody body = new PKIBody(PKIBody.TYPE_GEN_MSG, genMsgContent);
        return new PKIMessage(header, body);
    }

    protected PKIMessage buildMessageWithGeneralMsgContent(final ASN1ObjectIdentifier type,
            final ASN1Encodable value) throws CmpRequestorException {
        ParamUtil.requireNonNull("type", type);

        PKIHeader header = buildPkiHeader(null);
        InfoTypeAndValue itv = (value != null) ? new InfoTypeAndValue(type, value)
                : new InfoTypeAndValue(type);
        GenMsgContent genMsgContent = new GenMsgContent(itv);
        PKIBody body = new PKIBody(PKIBody.TYPE_GEN_MSG, genMsgContent);
        return new PKIMessage(header, body);
    }

    protected void checkProtection(final PkiResponse response) throws PkiErrorException {
        ParamUtil.requireNonNull("response", response);

        if (!response.hasProtection()) {
            return;
        }

        ProtectionVerificationResult protectionVerificationResult =
                response.protectionVerificationResult();

        if (protectionVerificationResult == null
            || protectionVerificationResult.protectionResult() != ProtectionResult.VALID) {
            throw new PkiErrorException(ClientErrorCode.PKISTATUS_RESPONSE_ERROR,
                        PKIFailureInfo.badMessageCheck, "message check of the response failed");
        }
    }

    public boolean isSendRequestorCert() {
        return sendRequestorCert;
    }

    public void setSendRequestorCert(final boolean sendRequestorCert) {
        this.sendRequestorCert = sendRequestorCert;
    }

    public boolean isSignRequest() {
        return signRequest;
    }

    public void setSignRequest(boolean signRequest) {
        this.signRequest = signRequest;
    }

}
