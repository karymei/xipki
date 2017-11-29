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

package org.xipki.ocsp.api;

import java.util.Date;

import org.bouncycastle.asn1.ocsp.CrlID;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.CertRevocationInfo;
import org.xipki.security.HashAlgoType;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class CertStatusInfo {

    private final CertStatus certStatus;

    private CertRevocationInfo revocationInfo;

    private HashAlgoType certHashAlgo;

    private byte[] certHash;

    private final Date thisUpdate;

    private final Date nextUpdate;

    private final String certprofile;

    private CrlID crlId;

    private Date archiveCutOff;

    private CertStatusInfo(final CertStatus certStatus, final Date thisUpdate,
            final Date nextUpdate, final String certprofile) {
        this.certStatus = ParamUtil.requireNonNull("certStatus", certStatus);
        this.thisUpdate = ParamUtil.requireNonNull("thisUpdate", thisUpdate);
        this.nextUpdate = nextUpdate;
        this.certprofile = certprofile;
    }

    public Date thisUpdate() {
        return thisUpdate;
    }

    public Date nextUpdate() {
        return nextUpdate;
    }

    public CertStatus certStatus() {
        return certStatus;
    }

    public CertRevocationInfo revocationInfo() {
        return revocationInfo;
    }

    public HashAlgoType certHashAlgo() {
        return certHashAlgo;
    }

    public byte[] certHash() {
        return certHash;
    }

    public String certprofile() {
        return certprofile;
    }

    public CrlID crlId() {
        return crlId;
    }

    public void setCrlId(final CrlID crlId) {
        this.crlId = crlId;
    }

    public Date archiveCutOff() {
        return archiveCutOff;
    }

    public void setArchiveCutOff(final Date archiveCutOff) {
        this.archiveCutOff = archiveCutOff;
    }

    public static CertStatusInfo getUnknownCertStatusInfo(final Date thisUpdate,
            final Date nextUpdate) {
        return new CertStatusInfo(CertStatus.UNKNOWN, thisUpdate, nextUpdate, null);
    }

    public static CertStatusInfo getIgnoreCertStatusInfo(final Date thisUpdate,
            final Date nextUpdate) {
        return new CertStatusInfo(CertStatus.IGNORE, thisUpdate, nextUpdate, null);
    }

    public static CertStatusInfo getIssuerUnknownCertStatusInfo(final Date thisUpdate,
            final Date nextUpdate) {
        return new CertStatusInfo(CertStatus.ISSUER_UNKNOWN, thisUpdate, nextUpdate, null);
    }

    public static CertStatusInfo getGoodCertStatusInfo(final HashAlgoType certHashAlgo,
            final byte[] certHash, final Date thisUpdate, final Date nextUpdate,
            final String certprofile) {
        CertStatusInfo ret = new CertStatusInfo(CertStatus.GOOD, thisUpdate, nextUpdate,
                certprofile);
        ret.certHashAlgo = certHashAlgo;
        ret.certHash = certHash;
        return ret;
    }

    public static CertStatusInfo getRevokedCertStatusInfo(final CertRevocationInfo revocationInfo,
            final HashAlgoType certHashAlgo, final byte[] certHash, final Date thisUpdate,
            final Date nextUpdate, final String certprofile) {
        if (revocationInfo == null) {
            throw new IllegalArgumentException("revocationInfo must not be null");
        }
        CertStatusInfo ret = new CertStatusInfo(CertStatus.REVOKED, thisUpdate, nextUpdate,
                certprofile);
        ret.revocationInfo = revocationInfo;
        ret.certHashAlgo = certHashAlgo;
        ret.certHash = certHash;
        return ret;
    }

}
