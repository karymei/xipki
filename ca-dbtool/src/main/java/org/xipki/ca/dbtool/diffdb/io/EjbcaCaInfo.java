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

package org.xipki.ca.dbtool.diffdb.io;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.HashAlgoType;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class EjbcaCaInfo {

    private final int caId;

    private final X500Name subject;

    private final String hexSha1;

    private final String caDirname;

    public EjbcaCaInfo(final int caId, final byte[] certBytes, final String caDirname) {
        ParamUtil.requireNonNull("certBytes", certBytes);

        this.caId = caId;
        this.hexSha1 = HashAlgoType.SHA1.hexHash(certBytes).toLowerCase();
        this.subject = Certificate.getInstance(certBytes).getSubject();
        this.caDirname = ParamUtil.requireNonNull("caDirname", caDirname);
    }

    public int caId() {
        return caId;
    }

    public X500Name subject() {
        return subject;
    }

    public String hexSha1() {
        return hexSha1;
    }

    public String caDirname() {
        return caDirname;
    }

}
