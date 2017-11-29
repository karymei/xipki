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

package org.xipki.ca.server.mgmt.api;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.xipki.common.util.Base64;
import org.xipki.common.util.CompareUtil;
import org.xipki.common.util.LogUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.SignerConf;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class CmpResponderEntry {

    private final String name;

    private final String type;

    private final String conf;

    private boolean certFaulty;

    private boolean confFaulty;

    private final String base64Cert;

    private X509Certificate certificate;

    public CmpResponderEntry(final String name, final String type, final String conf,
            final String base64Cert) {
        this.name = ParamUtil.requireNonBlank("name", name).toUpperCase();
        this.type = ParamUtil.requireNonBlank("type", type);
        this.conf = conf;
        this.base64Cert = base64Cert;

        if (base64Cert == null) {
            return;
        }

        try {
            this.certificate = X509Util.parseBase64EncodedCert(base64Cert);
        } catch (Throwable th) {
            this.certFaulty = true;
        }
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public String conf() {
        return conf;
    }

    public X509Certificate certificate() {
        return certificate;
    }

    public void setCertificate(final X509Certificate certificate) {
        if (base64Cert != null) {
            throw new IllegalStateException("certificate is already specified by base64Cert");
        }
        this.certificate = certificate;
    }

    public String base64Cert() {
        return base64Cert;
    }

    public boolean isFaulty() {
        return confFaulty || certFaulty;
    }

    public void setConfFaulty(final boolean confFaulty) {
        this.confFaulty = confFaulty;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(final boolean verbose) {
        return toString(verbose, true);
    }

    public String toString(final boolean verbose, final boolean ignoreSensitiveInfo) {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("name: ").append(name).append('\n');
        sb.append("faulty: ").append(isFaulty()).append('\n');
        sb.append("type: ").append(type).append('\n');
        sb.append("conf: ");
        if (conf == null) {
            sb.append("null");
        } else {
            sb.append(SignerConf.toString(conf, verbose, ignoreSensitiveInfo));
        }
        sb.append('\n');
        sb.append("certificate: ").append("\n");
        if (certificate != null || base64Cert != null) {
            if (certificate != null) {
                sb.append("\tissuer: ").append(X509Util.getRfc4519Name(
                        certificate.getIssuerX500Principal())).append('\n');
                sb.append("\tserialNumber: ")
                        .append(LogUtil.formatCsn(certificate.getSerialNumber())).append('\n');
                sb.append("\tsubject: ").append(X509Util.getRfc4519Name(
                        certificate.getSubjectX500Principal())).append('\n');
            }
            if (verbose) {
                sb.append("\tencoded: ");
                try {
                    sb.append(Base64.encodeToString(certificate.getEncoded()));
                } catch (CertificateEncodingException ex) {
                    sb.append("ERROR");
                }
            }
        } else {
            sb.append("null");
        }
        return sb.toString();
    } // method toString

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CmpResponderEntry)) {
            return false;
        }

        CmpResponderEntry objB = (CmpResponderEntry) obj;
        if (!name.equals(objB.name)) {
            return false;
        }

        if (!type.equals(objB.type)) {
            return false;
        }

        if (!CompareUtil.equalsObject(conf, objB.conf)) {
            return false;
        }

        if (!CompareUtil.equalsObject(base64Cert, objB.base64Cert)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
