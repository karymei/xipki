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

import java.security.cert.X509Certificate;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.api.x509.X509CaEntry;
import org.xipki.console.karaf.completer.FilePathCompleter;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "ca-add",
        description = "add CA")
@Service
public class CaAddCmd extends CaAddOrGenAction {

    @Option(name = "--cert",
            description = "CA certificate file")
    @Completion(FilePathCompleter.class)
    private String certFile;

    @Override
    protected Object execute0() throws Exception {
        X509CaEntry caEntry = getCaEntry();
        if (certFile != null) {
            X509Certificate caCert = X509Util.parseCert(certFile);
            caEntry.setCertificate(caCert);
        }

        boolean bo = caManager.addCa(caEntry);
        output(bo, "added", "could not add", "CA " + caEntry.ident().name());
        return null;
    }

}
