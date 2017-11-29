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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xipki.ca.dbtool.xmlio.InvalidDataObjectException;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class CaEntryContainer {

    private final Map<Integer, CaEntry> caEntryMap;

    public CaEntryContainer(final Set<CaEntry> caEntries) {
        ParamUtil.requireNonEmpty("caEntries", caEntries);
        caEntryMap = new HashMap<>(caEntries.size());
        for (CaEntry m : caEntries) {
            caEntryMap.put(m.caId(), m);
        }
    }

    public void addDigestEntry(final int caId, final long id, final DbDigestEntry reportEntry)
            throws IOException, InvalidDataObjectException {
        CaEntry ce = caEntryMap.get(caId);
        if (ce == null) {
            throw new IllegalArgumentException("unknown caId '" + caId + "'");
        }
        ce.addDigestEntry(id, reportEntry);
    }

    public void close() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (CaEntry m : caEntryMap.values()) {
            try {
                m.close();
            } catch (IOException ex) {
                sb.append("could not close CAEntry '").append(m.caId());
                sb.append("': ").append(ex.getMessage()).append(", ");
            }
        }

        int len = sb.length();
        if (len > 0) {
            sb.delete(len - 2, len);
            throw new IOException(sb.toString());
        }
    }

}
