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

package org.xipki.ca.api.profile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class ExtensionValues {

    private final Map<ASN1ObjectIdentifier, ExtensionValue> extensions = new HashMap<>();

    public boolean addExtension(final ASN1ObjectIdentifier type, final boolean critical,
            final ASN1Encodable value) {
        ParamUtil.requireNonNull("type", type);
        ParamUtil.requireNonNull("value", value);

        if (extensions.containsKey(type)) {
            return false;
        }
        extensions.put(type, new ExtensionValue(critical, value));
        return true;
    }

    public boolean addExtension(final ASN1ObjectIdentifier type, final ExtensionValue value) {
        ParamUtil.requireNonNull("type", type);
        ParamUtil.requireNonNull("value", value);

        if (extensions.containsKey(type)) {
            return false;
        }
        extensions.put(type, value);
        return true;
    }

    public Set<ASN1ObjectIdentifier> extensionTypes() {
        return Collections.unmodifiableSet(extensions.keySet());
    }

    public ExtensionValue getExtensionValue(final ASN1ObjectIdentifier type) {
        ParamUtil.requireNonNull("type", type);
        return extensions.get(type);
    }

    public boolean removeExtensionTuple(final ASN1ObjectIdentifier type) {
        ParamUtil.requireNonNull("type", type);
        return extensions.remove(type) != null;
    }

    public boolean containsExtension(final ASN1ObjectIdentifier type) {
        ParamUtil.requireNonNull("type", type);
        return extensions.containsKey(type);
    }

}
