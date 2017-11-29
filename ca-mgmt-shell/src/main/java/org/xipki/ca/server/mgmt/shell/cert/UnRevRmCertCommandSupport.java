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

package org.xipki.ca.server.mgmt.shell.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.xipki.ca.server.mgmt.api.CaEntry;
import org.xipki.ca.server.mgmt.api.x509.X509CaEntry;
import org.xipki.ca.server.mgmt.shell.CaCommandSupport;
import org.xipki.ca.server.mgmt.shell.completer.CaNameCompleter;
import org.xipki.common.util.IoUtil;
import org.xipki.console.karaf.CmdFailure;
import org.xipki.console.karaf.IllegalCmdParamException;
import org.xipki.console.karaf.completer.FilePathCompleter;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public abstract class UnRevRmCertCommandSupport extends CaCommandSupport {

    @Option(name = "--ca",
            required = true,
            description = "CA name\n"
                    + "(required)")
    @Completion(CaNameCompleter.class)
    protected String caName;

    @Option(name = "--cert", aliases = "-c",
            description = "certificate file"
                    + "(either cert or serial must be specified)")
    @Completion(FilePathCompleter.class)
    protected String certFile;

    @Option(name = "--serial", aliases = "-s",
            description = "serial number\n"
                    + "(either cert or serial must be specified)")
    private String serialNumberS;

    protected BigInteger getSerialNumber()
            throws CmdFailure, IllegalCmdParamException, CertificateException, IOException {
        CaEntry ca = caManager.getCa(caName);
        if (ca == null) {
            throw new CmdFailure("CA " + caName + " not available");
        }

        if (!(ca instanceof X509CaEntry)) {
            throw new CmdFailure("CA " + caName + " is not an X.509-CA");
        }

        BigInteger serialNumber;
        if (serialNumberS != null) {
            serialNumber = toBigInt(serialNumberS);
        } else if (certFile != null) {
            X509Certificate caCert = ((X509CaEntry) ca).certificate();
            X509Certificate cert = X509Util.parseCert(IoUtil.read(certFile));
            if (!X509Util.issues(caCert, cert)) {
                throw new CmdFailure(
                        "certificate '" + certFile + "' is not issued by CA " + caName);
            }
            serialNumber = cert.getSerialNumber();
        } else {
            throw new IllegalCmdParamException("neither serialNumber nor certFile is specified");
        }

        return serialNumber;
    }

}
