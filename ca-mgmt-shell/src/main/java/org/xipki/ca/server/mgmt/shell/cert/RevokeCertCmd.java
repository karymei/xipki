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

import java.util.Date;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.common.InvalidConfException;
import org.xipki.common.util.DateUtil;
import org.xipki.console.karaf.completer.ClientCrlReasonCompleter;
import org.xipki.security.CrlReason;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "xipki-ca", name = "revoke-cert",
        description = "revoke certificate")
@Service
public class RevokeCertCmd extends UnRevRmCertCommandSupport {

    @Option(name = "--reason", aliases = "-r",
            required = true,
            description = "CRL reason\n"
                    + "(required)")
    @Completion(ClientCrlReasonCompleter.class)
    private String reason;

    @Option(name = "--inv-date",
            description = "invalidity date, UTC time of format yyyyMMddHHmmss")
    private String invalidityDateS;

    @Override
    protected Object execute0() throws Exception {
        CrlReason crlReason = CrlReason.forNameOrText(reason);

        if (!CrlReason.PERMITTED_CLIENT_CRLREASONS.contains(crlReason)) {
            throw new InvalidConfException("reason " + reason + " is not permitted");
        }

        Date invalidityDate = null;
        if (isNotBlank(invalidityDateS)) {
            invalidityDate = DateUtil.parseUtcTimeyyyyMMddhhmmss(invalidityDateS);
        }

        boolean successful = caManager.revokeCertificate(caName, getSerialNumber(), crlReason,
                invalidityDate);
        output(successful, "revoked", "could not revoke", "certificate");

        return null;
    }

}
