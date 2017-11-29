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

import java.util.List;

import org.xipki.common.util.ParamUtil;
import org.xipki.ocsp.api.OcspStore;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

class Responder {

    private final ResponderOption responderOption;

    private final RequestOption requestOption;

    private final ResponseOption responseOption;

    private final ResponderSigner signer;

    private final List<OcspStore> stores;

    Responder(final ResponderOption responderOption, final RequestOption requestOption,
            final ResponseOption responseOption, final ResponderSigner signer,
            final List<OcspStore> stores) {
        this.responderOption = ParamUtil.requireNonNull("responderOption", responderOption);
        this.requestOption = ParamUtil.requireNonNull("requestOption", requestOption);
        this.responseOption = ParamUtil.requireNonNull("responseOption", responseOption);
        this.signer = ParamUtil.requireNonNull("signer", signer);
        this.stores = ParamUtil.requireNonEmpty("stores", stores);
    }

    public ResponderOption responderOption() {
        return responderOption;
    }

    public RequestOption requestOption() {
        return requestOption;
    }

    public ResponseOption responseOption() {
        return responseOption;
    }

    public ResponderSigner signer() {
        return signer;
    }

    public List<OcspStore> stores() {
        return stores;
    }

}
