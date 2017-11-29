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

import org.xipki.common.InvalidConfException;
import org.xipki.common.util.ParamUtil;
import org.xipki.common.util.StringUtil;
import org.xipki.ocsp.server.impl.jaxb.CacheType;
import org.xipki.ocsp.server.impl.jaxb.EmbedCertsMode;
import org.xipki.ocsp.server.impl.jaxb.ResponseOptionType;
import org.xipki.security.HashAlgoType;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

class ResponseOption {

    private final boolean responderIdByName;

    private final boolean includeInvalidityDate;

    private final boolean includeRevReason;

    private final EmbedCertsMode embedCertsMode;

    private final boolean includeCerthash;

    private final HashAlgoType certHashAlgo;

    private final Long cacheMaxAge;

    ResponseOption(final ResponseOptionType conf) throws InvalidConfException {
        ParamUtil.requireNonNull("conf", conf);
        this.responderIdByName = getBoolean(conf.isResponderIdByName(), true);
        this.includeInvalidityDate = getBoolean(conf.isIncludeInvalidityDate(), true);
        this.includeRevReason = getBoolean(conf.isIncludeRevReason(), true);
        this.embedCertsMode = (conf.getEmbedCertsMode() == null) ?
                EmbedCertsMode.SIGNER : conf.getEmbedCertsMode();
        this.includeCerthash = getBoolean(conf.isIncludeCertHash(), false);
        CacheType cacheConf = conf.getCache();
        if (cacheConf != null && cacheConf.getCacheMaxAge() != null) {
            this.cacheMaxAge = cacheConf.getCacheMaxAge().longValue();
        } else {
            this.cacheMaxAge = null;
        }

        HashAlgoType tmpCertHashAlgo = null;
        String str = conf.getCerthashAlgorithm();
        if (str != null) {
            String token = str.trim();
            if (StringUtil.isNotBlank(token)) {
                HashAlgoType algo = HashAlgoType.getHashAlgoType(token);
                if (algo != null && RequestOption.SUPPORTED_HASH_ALGORITHMS.contains(algo)) {
                    tmpCertHashAlgo = algo;
                } else {
                    throw new InvalidConfException("hash algorithm " + token + " is unsupported");
                }
            }
        }
        this.certHashAlgo = tmpCertHashAlgo;
    }

    public boolean isResponderIdByName() {
        return responderIdByName;
    }

    public boolean isIncludeInvalidityDate() {
        return includeInvalidityDate;
    }

    public boolean isIncludeRevReason() {
        return includeRevReason;
    }

    public boolean isIncludeCerthash() {
        return includeCerthash;
    }

    public Long cacheMaxAge() {
        return cacheMaxAge;
    }

    public EmbedCertsMode embedCertsMode() {
        return embedCertsMode;
    }

    public HashAlgoType certHashAlgo() {
        return certHashAlgo;
    }

    private static boolean getBoolean(final Boolean bo, final boolean dflt) {
        return (bo == null) ? dflt : bo.booleanValue();
    }

}
