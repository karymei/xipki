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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.console.karaf.CmdFailure;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "restart",
        description = "restart CA system")
@Service
public class CaSystemRestartCmd extends CaCommandSupport {

    @Override
    protected Object execute0() throws Exception {
        boolean successful = caManager.restartCaSystem();
        if (!successful) {
            throw new CmdFailure("could not restart CA system");
        }

        StringBuilder sb = new StringBuilder("restarted CA system\n");

        sb.append("  successful CAs:\n");
        String prefix = "    ";
        printCaNames(sb, caManager.getSuccessfulCaNames(), prefix);

        sb.append("  failed CAs:\n");
        printCaNames(sb, caManager.getFailedCaNames(), prefix);

        sb.append("  inactive CAs:\n");
        printCaNames(sb, caManager.getInactiveCaNames(), prefix);

        println(sb.toString());
        return null;
    } // method execute0

}
