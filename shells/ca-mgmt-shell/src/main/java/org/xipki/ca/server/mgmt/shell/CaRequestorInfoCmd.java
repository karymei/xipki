/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
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

import java.util.Set;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.api.CaHasRequestorEntry;
import org.xipki.ca.server.mgmt.shell.completer.CaNameCompleter;
import org.xipki.console.karaf.CmdFailure;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "careq-info",
        description = "show information of requestor in CA")
@Service
public class CaRequestorInfoCmd extends CaAction {

    @Option(name = "--ca", required = true,
            description = "CA name\n(required)")
    @Completion(CaNameCompleter.class)
    private String caName;

    @Override
    protected Object execute0() throws Exception {
        if (caManager.getCa(caName) == null) {
            throw new CmdFailure("could not find CA '" + caName + "'");
        }

        StringBuilder sb = new StringBuilder();

        Set<CaHasRequestorEntry> entries = caManager.getRequestorsForCa(caName);
        if (isNotEmpty(entries)) {
            sb.append("requestors trusted by CA " + caName).append("\n");
            for (CaHasRequestorEntry entry : entries) {
                sb.append("\t").append(entry).append("\n");
            }
        } else {
            sb.append("\tno requestor for CA " + caName + " is configured");
        }
        println(sb.toString());
        return null;
    }

}
