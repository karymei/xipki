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

package org.xipki.ca.server.mgmt.api.x509;

import java.security.cert.X509Certificate;
import java.util.List;

import org.xipki.ca.api.NameId;
import org.xipki.ca.server.mgmt.api.CaMgmtException;
import org.xipki.ca.server.mgmt.api.ChangeCaEntry;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class X509ChangeCaEntry extends ChangeCaEntry {

    private List<String> crlUris;

    private List<String> deltaCrlUris;

    private List<String> ocspUris;

    private List<String> caCertUris;

    private X509Certificate cert;

    private String crlSignerName;

    private Integer numCrls;

    private Integer serialNoBitLen;

    public X509ChangeCaEntry(final NameId ident) throws CaMgmtException {
        super(ident);
    }

    public Integer serialNoBitLen() {
        return serialNoBitLen;
    }

    public void setSerialNoBitLen(final Integer serialNoBitLen) {
        if (serialNoBitLen != null) {
            ParamUtil.requireRange("serialNoBitLen", serialNoBitLen, 63, 159);
        }
        this.serialNoBitLen = serialNoBitLen;
    }

    public List<String> crlUris() {
        return crlUris;
    }

    public void setCrlUris(final List<String> crlUris) {
        this.crlUris = crlUris;
    }

    public List<String> deltaCrlUris() {
        return deltaCrlUris;
    }

    public void setDeltaCrlUris(final List<String> deltaCrlUris) {
        this.deltaCrlUris = deltaCrlUris;
    }

    public List<String> ocspUris() {
        return ocspUris;
    }

    public void setOcspUris(final List<String> ocspUris) {
        this.ocspUris = ocspUris;
    }

    public List<String> caCertUris() {
        return caCertUris;
    }

    public void setCaCertUris(final List<String> caCertUris) {
        this.caCertUris = caCertUris;
    }

    public X509Certificate cert() {
        return cert;
    }

    public void setCert(final X509Certificate cert) {
        this.cert = cert;
    }

    public String crlSignerName() {
        return crlSignerName;
    }

    public void setCrlSignerName(final String crlSignerName) {
        this.crlSignerName = (crlSignerName == null) ? null : crlSignerName.toUpperCase();
    }

    public Integer numCrls() {
        return numCrls;
    }

    public void setNumCrls(final Integer numCrls) {
        this.numCrls = numCrls;
    }

}
