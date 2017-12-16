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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.api.CmpControlEntry;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "cmpcontrol-add",
        description = "add CMP control")
@Service
public class CmpControlAddCmd extends CaCommandSupport {

    @Option(name = "--name", aliases = "-n",
            required = true,
            description = "CMP control name\n"
                    + "(required)")
    private String name;

    @Option(name = "--conf",
            required = true,
            description = "CMP control configuration\n"
                    + "(required)")
    private String conf;

    @Override
    protected Object execute0() throws Exception {
        CmpControlEntry entry = new CmpControlEntry(name, conf);
        boolean bo = caManager.addCmpControl(entry);
        output(bo, "added", "could not add", "CMP control " + name);
        return null;
    }

}
