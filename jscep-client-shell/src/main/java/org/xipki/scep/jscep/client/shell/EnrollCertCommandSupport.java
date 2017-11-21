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

package org.xipki.scep.jscep.client.shell;

import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.jscep.client.Client;
import org.jscep.client.ClientException;
import org.jscep.client.EnrollmentResponse;
import org.jscep.transaction.TransactionException;
import org.xipki.common.util.IoUtil;
import org.xipki.console.karaf.CmdFailure;
import org.xipki.console.karaf.completer.FilePathCompleter;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public abstract class EnrollCertCommandSupport extends ClientCommandSupport {

    @Option(name = "--csr",
            required = true,
            description = "CSR file\n"
                    + "(required)")
    @Completion(FilePathCompleter.class)
    private String csrFile;

    @Option(name = "--out", aliases = "-o",
            required = true,
            description = "where to save the certificate\n"
                    + "(required)")
    @Completion(FilePathCompleter.class)
    private String outputFile;

    /**
     *
     * @param client
     *          Client. Must not be {@code null}.
     * @param csr
     *          CSR. Must not be {@code null}.
     * @param identityKey
     *          Identity key. Must not be {@code null}.
     * @param identityCert
     *          Identity certificate. Must not be {@code null}.
     */
    protected abstract EnrollmentResponse requestCertificate(Client client,
            PKCS10CertificationRequest csr, PrivateKey identityKey,
            X509Certificate identityCertÔ) throws ClientException, TransactionException;

    @Override
    protected Object execute0() throws Exception {
        Client client = getScepClient();

        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(IoUtil.read(csrFile));

        EnrollmentResponse resp = requestCertificate(client, csr, getIdentityKey(),
                getIdentityCert());
        if (resp.isFailure()) {
            throw new CmdFailure("server returned 'failure'");
        }

        if (resp.isPending()) {
            throw new CmdFailure("server returned 'pending'");
        }

        X509Certificate cert = extractEeCerts(resp.getCertStore());

        if (cert == null) {
            throw new Exception("received no certificate");
        }

        saveVerbose("saved enrolled certificate to file", new File(outputFile), cert.getEncoded());
        return null;
    }

}
