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
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.xipki.ca.api.OperationException;
import org.xipki.ca.api.OperationException.ErrorCode;
import org.xipki.ca.server.mgmt.api.x509.CrlControl;
import org.xipki.ca.server.mgmt.api.x509.X509CrlSignerEntry;
import org.xipki.common.InvalidConfException;
import org.xipki.common.ObjectCreationException;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.ConcurrentContentSigner;
import org.xipki.security.KeyUsage;
import org.xipki.security.SecurityFactory;
import org.xipki.security.SignerConf;
import org.xipki.security.exception.XiSecurityException;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

class X509CrlSignerEntryWrapper {

    private X509CrlSignerEntry dbEntry;

    private CrlControl crlControl;

    private ConcurrentContentSigner signer;

    private byte[] subjectKeyIdentifier;

    X509CrlSignerEntryWrapper() {
    }

    public void setDbEntry(final X509CrlSignerEntry dbEntry) throws InvalidConfException {
        this.dbEntry = dbEntry;
        this.crlControl = new CrlControl(dbEntry.crlControl());
    }

    public CrlControl crlControl() {
        return crlControl;
    }

    public void initSigner(final SecurityFactory securityFactory)
            throws XiSecurityException, OperationException, InvalidConfException {
        ParamUtil.requireNonNull("securityFactory", securityFactory);
        if (signer != null) {
            return;
        }

        if (dbEntry == null) {
            throw new XiSecurityException("dbEntry is null");
        }

        if ("CA".equals(dbEntry.type())) {
            return;
        }

        dbEntry.setConfFaulty(true);

        X509Certificate responderCert = dbEntry.certificate();
        try {
            signer = securityFactory.createSigner(dbEntry.type(),
                    new SignerConf(dbEntry.conf()), responderCert);
        } catch (ObjectCreationException ex1) {
            throw new XiSecurityException("signer without certificate is not allowed");
        }

        X509Certificate signerCert = signer.getCertificate();
        if (signerCert == null) {
            throw new XiSecurityException("signer without certificate is not allowed");
        }

        if (dbEntry.base64Cert() == null) {
            dbEntry.setCertificate(signerCert);
        }

        byte[] encodedSkiValue = signerCert.getExtensionValue(
                Extension.subjectKeyIdentifier.getId());
        if (encodedSkiValue == null) {
            throw new OperationException(ErrorCode.INVALID_EXTENSION,
                    "CA certificate does not have required extension SubjectKeyIdentifier");
        }

        ASN1OctetString ski;
        try {
            ski = (ASN1OctetString) X509ExtensionUtil.fromExtensionValue(encodedSkiValue);
        } catch (IOException ex) {
            throw new OperationException(ErrorCode.INVALID_EXTENSION, ex);
        }
        this.subjectKeyIdentifier = ski.getOctets();

        if (!X509Util.hasKeyusage(signerCert, KeyUsage.cRLSign)) {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE,
                    "CRL signer does not have keyusage cRLSign");
        }
        dbEntry.setConfFaulty(false);
    } // method initSigner

    public X509CrlSignerEntry dbEntry() {
        return dbEntry;
    }

    public X509Certificate cert() {
        return (signer == null) ? dbEntry.certificate() : signer.getCertificate();
    }

    public byte[] subjectKeyIdentifier() {
        return (subjectKeyIdentifier == null) ? null
                : Arrays.copyOf(subjectKeyIdentifier, subjectKeyIdentifier.length);
    }

    public ConcurrentContentSigner signer() {
        return signer;
    }

}
