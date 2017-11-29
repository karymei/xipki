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

package org.xipki.ocsp.server.impl.store.crl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import org.xipki.common.util.StringUtil;
import org.xipki.ocsp.api.OcspStoreException;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

class StoreConf {

    /*
     * required
     */
    private static final String KEY_crl_file = "crl.file";

    /*
     * optional
     */
    private static final String KEY_crl_url = "crl.url";

    /*
     * Whether thisUpdate and nextUpdate of CRL are used in the corresponding fields
     * of OCSP response. The default value is true.
     *
     * optional
     */
    private static final String KEY_useUpdateDatesFromCrl = "useUpdateDatesFromCrl";

    /*
     * required
     */
    private static final String KEY_caCert_file = "caCert.file";

    /*
     * required for indirect CRL
     */
    private static final String KEY_issuerCert_file = "issuerCert.file";

    /*
     * Folder containing the DER-encoded certificates suffixed with ".der" and ".crt"
     * optional.
     */
    private static final String KEY_certs_dir = "certs.dir";

    private String crlFile;

    /*
     * optional, can be null
     */
    private String crlUrl;

    private boolean useUpdateDatesFromCrl = true;

    private String caCertFile;

    /*
     * optional, can be null, but required for indirect CRL
     */
    private String issuerCertFile;

    /*
     * optional, can be null
     */
    private String certsDir;

    StoreConf(final String propsConf) throws OcspStoreException {
        Properties props = new Properties();
        try {
            props.load(new ByteArrayInputStream(propsConf.getBytes()));
        } catch (IOException ex) {
            throw new OcspStoreException("could not load properties: " + ex.getMessage(), ex);
        }

        this.crlFile = getRequiredProperty(props, KEY_crl_file);
        this.crlUrl = getOptionalProperty(props, KEY_crl_url);
        this.caCertFile = getRequiredProperty(props, KEY_caCert_file);
        this.issuerCertFile = getOptionalProperty(props, KEY_issuerCert_file);
        this.certsDir = getOptionalProperty(props, KEY_certs_dir);

        String propKey = KEY_useUpdateDatesFromCrl;
        String propValue = props.getProperty(propKey);
        if (propValue != null) {
            propValue = propValue.trim();
            if ("true".equalsIgnoreCase(propValue)) {
                this.useUpdateDatesFromCrl = true;
            } else if ("false".equalsIgnoreCase(propValue)) {
                this.useUpdateDatesFromCrl = false;
            } else {
                throw new OcspStoreException("invalid property " + propKey + ": '"
                        + propValue + "'");
            }
        } else {
            this.useUpdateDatesFromCrl = true;
        }
    }

    String crlFile() {
        return crlFile;
    }

    String crlUrl() {
        return crlUrl;
    }

    boolean isUseUpdateDatesFromCrl() {
        return useUpdateDatesFromCrl;
    }

    String caCertFile() {
        return caCertFile;
    }

    String issuerCertFile() {
        return issuerCertFile;
    }

    String certsDir() {
        return certsDir;
    }

    private String getRequiredProperty(final Properties props, final String propKey)
            throws OcspStoreException {
        String str = props.getProperty(propKey);
        if (str == null) {
            throw new OcspStoreException("missing required property " + propKey);
        }
        String ret = str.trim();
        if (StringUtil.isBlank(ret)) {
            throw new OcspStoreException("property " + propKey + " must not be blank");
        }
        return str.trim();
    }

    private String getOptionalProperty(final Properties props, final String propKey)
            throws OcspStoreException {
        String str = props.getProperty(propKey);
        if (str == null) {
            return null;
        }
        return str.trim();
    }

}
