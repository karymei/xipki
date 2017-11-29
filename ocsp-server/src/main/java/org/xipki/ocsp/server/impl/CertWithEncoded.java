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

package org.xipki.ocsp.server.impl;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class CertWithEncoded {

    private final X509Certificate certificate;

    private final String className;

    private final byte[] encoded;

    public CertWithEncoded(final X509Certificate cert) throws CertificateEncodingException {
        this.certificate = ParamUtil.requireNonNull("cert", cert);
        this.className = cert.getClass().getName();
        this.encoded = cert.getEncoded();
    }

    public X509Certificate certificate() {
        return certificate;
    }

    public boolean equalsCert(final X509Certificate cert) {
        if (cert == null) {
            return false;
        }
        if (certificate == cert) {
            return true;
        }

        if (className.equals(cert.getClass().getName())) {
            return certificate.equals(cert);
        } else if (certificate.equals(cert)) {
            return true;
        } else {
            byte[] encodedCert;
            try {
                encodedCert = cert.getEncoded();
            } catch (CertificateEncodingException ex) {
                return false;
            }
            return Arrays.equals(encoded, encodedCert);
        }
    }

}
