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

import java.util.LinkedList;
import java.util.List;

import org.xipki.common.QueueEntry;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class DigestDbEntrySet implements QueueEntry, Comparable<DigestDbEntrySet> {

    private final long startId;

    private Exception exception;

    private List<IdentifiedDbDigestEntry> entries = new LinkedList<>();

    public DigestDbEntrySet(final long startId) {
        this.startId = startId;
    }

    public void setException(final Exception exception) {
        this.exception = exception;
    }

    public Exception exception() {
        return exception;
    }

    public void addEntry(final IdentifiedDbDigestEntry entry) {
        entries.add(entry);
    }

    public long startId() {
        return startId;
    }

    public List<IdentifiedDbDigestEntry> entries() {
        return entries;
    }

    @Override
    public int compareTo(DigestDbEntrySet obj) {
        if (startId < obj.startId) {
            return -1;
        } else if (startId == obj.startId) {
            return 0;
        } else {
            return 1;
        }
    }

}
