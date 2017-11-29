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

package org.xipki.ca.dbtool.xmlio.ocsp;

import javax.xml.stream.XMLStreamException;

import org.xipki.ca.dbtool.xmlio.DbiXmlWriter;
import org.xipki.ca.dbtool.xmlio.IdentifidDbObjectType;
import org.xipki.ca.dbtool.xmlio.InvalidDataObjectException;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class OcspCertType extends IdentifidDbObjectType {

    public static final String TAG_PARENT = "certs";

    public static final String TAG_ROOT = "cert";

    /**
     * issuer id.
     */
    public static final String TAG_IID = "iid";

    /**
     * certificate serial number.
     */
    public static final String TAG_SN = "sn";

    public static final String TAG_UPDATE = "update";

    /**
     * whether revoked.
     */
    public static final String TAG_REV = "rev";

    /**
     * revocation reason.
     */
    public static final String TAG_RR = "rr";

    /**
     * revocation time.
     */
    public static final String TAG_RT = "rt";

    /**
     * revocation invalidity time.
     */
    public static final String TAG_RIT = "rit";

    /**
     * certificate profile name.
     */
    public static final String TAG_PROFILE = "profile";

    private String file;

    private String profile;

    private Integer iid;

    private String sn;

    private Long update;

    private Boolean rev;

    private Integer rr;

    private Long rt;

    private Long rit;

    public Integer iid() {
        return iid;
    }

    public void setIid(final Integer iid) {
        this.iid = iid;
    }

    public String sn() {
        return sn;
    }

    public void setSn(final String sn) {
        this.sn = sn;
    }

    public String profile() {
        return profile;
    }

    public void setProfile(final String profile) {
        this.profile = profile;
    }

    public Long update() {
        return update;
    }

    public void setUpdate(final Long update) {
        this.update = update;
    }

    public Boolean rev() {
        return rev;
    }

    public void setRev(final Boolean rev) {
        this.rev = rev;
    }

    public Integer rr() {
        return rr;
    }

    public void setRr(final Integer rr) {
        this.rr = rr;
    }

    public Long rt() {
        return rt;
    }

    public void setRt(final Long rt) {
        this.rt = rt;
    }

    public Long rit() {
        return rit;
    }

    public void setRit(final Long rit) {
        this.rit = rit;
    }

    public String file() {
        return file;
    }

    public void setFile(final String file) {
        this.file = file;
    }

    @Override
    public void validate() throws InvalidDataObjectException {
        super.validate();
        assertNotNull(TAG_IID, iid);
        assertNotBlank(TAG_SN, sn);
        assertNotNull(TAG_UPDATE, update);
        assertNotNull(TAG_REV, rev);
        if (rev) {
            assertNotNull(TAG_RR, rr);
            assertNotNull(TAG_RT, rt);
        }

        assertNotBlank(TAG_FILE, file);
    }

    @Override
    public void writeTo(final DbiXmlWriter writer)
            throws InvalidDataObjectException, XMLStreamException {
        ParamUtil.requireNonNull("writer", writer);

        validate();

        writer.writeStartElement(TAG_ROOT);
        writeIfNotNull(writer, TAG_ID, id());
        writeIfNotNull(writer, TAG_IID, iid);
        writeIfNotNull(writer, TAG_SN, sn);
        writeIfNotNull(writer, TAG_UPDATE, update);
        writeIfNotNull(writer, TAG_REV, rev);
        writeIfNotNull(writer, TAG_RR, rr);
        writeIfNotNull(writer, TAG_RT, rt);
        writeIfNotNull(writer, TAG_RIT, rit);
        writeIfNotNull(writer, TAG_PROFILE, profile);
        writeIfNotNull(writer, TAG_FILE, file);
        writer.writeEndElement();
        writer.writeNewline();
    }

}
