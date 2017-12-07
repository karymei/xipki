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

import java.io.ByteArrayInputStream;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.api.CaManager;
import org.xipki.ca.server.mgmt.api.CmpResponderEntry;
import org.xipki.ca.server.mgmt.shell.completer.ResponderNameCompleter;
import org.xipki.common.util.Base64;
import org.xipki.common.util.IoUtil;
import org.xipki.console.karaf.IllegalCmdParamException;
import org.xipki.console.karaf.completer.FilePathCompleter;
import org.xipki.console.karaf.completer.SignerTypeCompleter;
import org.xipki.password.PasswordResolver;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "responder-up",
        description = "update responder")
@Service
public class ResponderUpdateCmd extends CaCommandSupport {

    @Reference
    protected PasswordResolver passwordResolver;

    @Option(name = "--name", aliases = "-n",
            required = true,
            description = "responder name\n"
                    + "(required)")
    @Completion(ResponderNameCompleter.class)
    protected String name;

    @Option(name = "--signer-type",
            description = "type of the responder signer")
    @Completion(SignerTypeCompleter.class)
    protected String signerType;

    @Option(name = "--cert",
            description = "requestor certificate file or 'NULL'")
    @Completion(FilePathCompleter.class)
    protected String certFile;

    @Option(name = "--signer-conf",
            description = "conf of the responder signer or 'NULL'")
    private String signerConf;

    protected String getSignerConf() throws Exception {
        if (signerConf == null) {
            return signerConf;
        }
        String tmpSignerType = signerType;
        if (tmpSignerType == null) {
            CmpResponderEntry entry = caManager.getResponder(name);
            if (entry == null) {
                throw new IllegalCmdParamException("please specify the signerType");
            }
            tmpSignerType = entry.type();
        }

        return ShellUtil.canonicalizeSignerConf(tmpSignerType, signerConf, passwordResolver,
                securityFactory);
    }

    @Override
    protected Object execute0() throws Exception {
        String cert = null;
        if (CaManager.NULL.equalsIgnoreCase(certFile)) {
            cert = CaManager.NULL;
        } else if (certFile != null) {
            byte[] certBytes = IoUtil.read(certFile);
            X509Util.parseCert(new ByteArrayInputStream(certBytes));
            cert = Base64.encodeToString(certBytes);
        }

        boolean bo = caManager.changeResponder(name, signerType, getSignerConf(), cert);
        output(bo, "updated", "could not update", "CMP responder " + name);
        return null;
    }

}
