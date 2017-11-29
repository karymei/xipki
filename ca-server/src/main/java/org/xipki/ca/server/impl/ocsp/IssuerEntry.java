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

package org.xipki.ca.server.impl.ocsp;

import java.util.Arrays;

import org.xipki.common.util.Base64;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

class IssuerEntry {

    private final int id;

    private final String subject;

    private final byte[] sha1Fp;

    private final byte[] cert;

    IssuerEntry(final int id, final String subject, final String b64Sha1Fp, final String b64Cert) {
        super();
        this.id = id;
        this.subject = subject;
        this.sha1Fp = Base64.decode(b64Sha1Fp);
        this.cert = Base64.decode(b64Cert);
    }

    int id() {
        return id;
    }

    String subject() {
        return subject;
    }

    boolean matchSha1Fp(final byte[] anotherSha1Fp) {
        return Arrays.equals(this.sha1Fp, anotherSha1Fp);
    }

    boolean matchCert(final byte[] encodedCert) {
        return Arrays.equals(this.cert, encodedCert);
    }
}
