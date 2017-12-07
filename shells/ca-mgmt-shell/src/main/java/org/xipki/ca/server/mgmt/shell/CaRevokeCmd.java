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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.shell.completer.CaCrlReasonCompleter;
import org.xipki.ca.server.mgmt.shell.completer.CaNameCompleter;
import org.xipki.common.util.DateUtil;
import org.xipki.console.karaf.IllegalCmdParamException;
import org.xipki.security.CertRevocationInfo;
import org.xipki.security.CrlReason;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "ca-revoke",
        description = "revoke CA")
@Service
public class CaRevokeCmd extends CaCommandSupport {

    public static final List<CrlReason> PERMITTED_REASONS = Collections.unmodifiableList(
            Arrays.asList(new CrlReason[] {
                CrlReason.UNSPECIFIED, CrlReason.KEY_COMPROMISE, CrlReason.CA_COMPROMISE,
                CrlReason.AFFILIATION_CHANGED, CrlReason.SUPERSEDED,
                CrlReason.CESSATION_OF_OPERATION,
                CrlReason.CERTIFICATE_HOLD, CrlReason.PRIVILEGE_WITHDRAWN}));

    @Argument(index = 0, name = "name", description = "CA name", required = true)
    @Completion(CaNameCompleter.class)
    private String caName;

    @Option(name = "--reason",
            required = true,
            description = "CRL reason\n"
                    + "(required)")
    @Completion(CaCrlReasonCompleter.class)
    private String reason;

    @Option(name = "--rev-date",
            description = "revocation date, UTC time of format yyyyMMddHHmmss\n"
                    + "(defaults to current time)")
    private String revocationDateS;

    @Option(name = "--inv-date",
            description = "invalidity date, UTC time of format yyyyMMddHHmmss")
    private String invalidityDateS;

    @Override
    protected Object execute0() throws Exception {
        CrlReason crlReason = CrlReason.forNameOrText(reason);

        if (!PERMITTED_REASONS.contains(crlReason)) {
            throw new IllegalCmdParamException("reason " + reason + " is not permitted");
        }

        if (!caManager.getCaNames().contains(caName)) {
            throw new IllegalCmdParamException("invalid CA name " + caName);
        }

        Date revocationDate = null;
        revocationDate = isNotBlank(revocationDateS)
                ? DateUtil.parseUtcTimeyyyyMMddhhmmss(revocationDateS) : new Date();

        Date invalidityDate = null;
        if (isNotBlank(invalidityDateS)) {
            invalidityDate = DateUtil.parseUtcTimeyyyyMMddhhmmss(invalidityDateS);
        }

        CertRevocationInfo revInfo = new CertRevocationInfo(crlReason, revocationDate,
                invalidityDate);
        boolean bo = caManager.revokeCa(caName, revInfo);
        output(bo, "revoked", "could not revoke", "CA " + caName);
        return null;
    } // method execute0

}
