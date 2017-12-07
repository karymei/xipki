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

package org.xipki.ca.server.mgmt.shell;

import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.shell.completer.CaNamePlusAllCompleter;
import org.xipki.ca.server.mgmt.shell.completer.PublisherNamePlusAllCompleter;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "clear-publishqueue",
        description = "clear publish queue")
@Service
public class ClearPublishQueueCmd extends CaCommandSupport {

    @Option(name = "--ca",
            required = true,
            description = "CA name or 'ALL' for all CAs\n"
                    + "(required)")
    @Completion(CaNamePlusAllCompleter.class)
    private String caName;

    @Option(name = "--publisher",
            required = true, multiValued = true,
            description = "publisher name or 'ALL' for all publishers\n"
                        + "(required, multi-valued)")
    @Completion(PublisherNamePlusAllCompleter.class)
    private List<String> publisherNames;

    @Override
    protected Object execute0() throws Exception {
        if (publisherNames == null) {
            throw new RuntimeException("should not reach here");
        }
        boolean allPublishers = false;
        for (String publisherName : publisherNames) {
            if ("ALL".equalsIgnoreCase(publisherName)) {
                allPublishers = true;
                break;
            }
        }

        if (allPublishers) {
            publisherNames = null;
        }

        if ("ALL".equalsIgnoreCase(caName)) {
            caName = null;
        }

        boolean bo = caManager.clearPublishQueue(caName, publisherNames);
        output(bo, "cleared", "could not clear",
                "publish queue of CA " + caName + " for publishers " + toString(publisherNames));
        return null;
    }

}
