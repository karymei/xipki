/*
 *
 * Copyright (c) 2013 - 2017 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ca.server.mgmt.shell;

import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.shell.completer.CaNameCompleter;
import org.xipki.ca.server.mgmt.shell.completer.PublisherNamePlusAllCompleter;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "republish",
        description = "republish certificates")
@Service
public class RepublishCmd extends CaAction {

    @Option(name = "--thread",
            description = "number of threads")
    private Integer numThreads = 5;

    @Option(name = "--ca",
            required = true,
            description = "CA name\n"
                    + "(required)")
    @Completion(CaNameCompleter.class)
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

        boolean bo = caManager.republishCertificates(caName, publisherNames, numThreads);
        output(bo, "republished", "could not republish", "certificates");
        return null;
    }

}
