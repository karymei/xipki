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

import java.util.Set;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.api.NameId;
import org.xipki.ca.server.mgmt.api.x509.ScepEntry;
import org.xipki.ca.server.mgmt.shell.completer.ProfileNameAndAllCompleter;
import org.xipki.ca.server.mgmt.shell.completer.ScepNameCompleter;
import org.xipki.common.InvalidConfException;
import org.xipki.common.util.IoUtil;
import org.xipki.console.karaf.completer.FilePathCompleter;
import org.xipki.console.karaf.completer.SignerTypeCompleter;
import org.xipki.password.PasswordResolver;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "xipki-ca", name = "scep-add",
        description = "add SCEP")
@Service
public class ScepAddCmd extends CaCommandSupport {

    @Option(name = "--name",
            required = true,
            description = "name\n"
                    + "(required)")
    private String name;

    @Option(name = "--ca",
            required = true,
            description = "CA name\n"
                    + "(required)")
    @Completion(ScepNameCompleter.class)
    private String caName;

    @Option(name = "--inactive",
            description = "do not activate this SCEP")
    private Boolean inactive = Boolean.FALSE;

    @Option(name = "--resp-type",
            required = true,
            description = "type of the responder\n"
                    + "(required)")
    @Completion(SignerTypeCompleter.class)
    private String responderType;

    @Option(name = "--resp-conf",
            required = true,
            description = "conf of the responder\n"
                    + "(required)")
    private String responderConf;

    @Option(name = "--resp-cert",
            description = "responder certificate file")
    @Completion(FilePathCompleter.class)
    private String certFile;

    @Option(name = "--control",
            required = false,
            description = "SCEP control")
    private String scepControl;

    @Option(name = "--profile",
            required = true, multiValued = true,
            description = "profile name or 'ALL' for all profiles\n"
                    + "(required, multi-valued)")
    @Completion(ProfileNameAndAllCompleter.class)
    private Set<String> profiles;

    @Reference
    private PasswordResolver passwordResolver;

    @Override
    protected Object execute0() throws Exception {
        String base64Cert = null;
        if (certFile != null) {
            base64Cert = IoUtil.base64Encode(IoUtil.read(certFile), false);
        }

        if ("PKCS12".equalsIgnoreCase(responderType) || "JKS".equalsIgnoreCase(responderType)) {
            responderConf = ShellUtil.canonicalizeSignerConf(responderType, responderConf,
                    passwordResolver, securityFactory);
        }

        ScepEntry entry = new ScepEntry(name, new NameId(null, caName), !inactive, responderType,
                responderConf, base64Cert, profiles, scepControl);
        if (entry.isFaulty()) {
            throw new InvalidConfException("certificate is invalid");
        }

        boolean bo = caManager.addScep(entry);
        output(bo, "added", "could not add", "SCEP responder " + name);
        return null;
    }

}
