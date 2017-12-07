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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.api.NameId;
import org.xipki.ca.api.profile.CertValidity;
import org.xipki.ca.server.mgmt.api.CaEntry;
import org.xipki.ca.server.mgmt.api.CaManager;
import org.xipki.ca.server.mgmt.api.CaStatus;
import org.xipki.ca.server.mgmt.api.ValidityMode;
import org.xipki.ca.server.mgmt.api.x509.X509ChangeCaEntry;
import org.xipki.ca.server.mgmt.shell.completer.CaNameCompleter;
import org.xipki.ca.server.mgmt.shell.completer.CaStatusCompleter;
import org.xipki.ca.server.mgmt.shell.completer.CmpControlNamePlusNullCompleter;
import org.xipki.ca.server.mgmt.shell.completer.CrlSignerNamePlusNullCompleter;
import org.xipki.ca.server.mgmt.shell.completer.PermissionCompleter;
import org.xipki.ca.server.mgmt.shell.completer.ResponderNamePlusNullCompleter;
import org.xipki.ca.server.mgmt.shell.completer.ValidityModeCompleter;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.console.karaf.IllegalCmdParamException;
import org.xipki.console.karaf.completer.FilePathCompleter;
import org.xipki.console.karaf.completer.SignerTypeCompleter;
import org.xipki.console.karaf.completer.YesNoCompleter;
import org.xipki.password.PasswordResolver;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "ca-up",
        description = "update CA")
@Service
public class CaUpdateCmd extends CaCommandSupport {

    @Option(name = "--name", aliases = "-n",
            required = true,
            description = "CA name\n"
                    + "(required)")
    @Completion(CaNameCompleter.class)
    private String caName;

    @Option(name = "--sn-bitlen",
            description = "number of bits of the serial number, between 63 and 159")
    private Integer snBitLen;

    @Option(name = "--status",
            description = "CA status")
    @Completion(CaStatusCompleter.class)
    private String caStatus;

    @Option(name = "--ca-cert-uri",
            multiValued = true,
            description = "CA certificate URI\n"
                    + "(multi-valued)")
    private List<String> caCertUris;

    @Option(name = "--ocsp-uri",
            multiValued = true,
            description = "OCSP URI or 'NULL'\n"
                    + "(multi-valued)")
    private List<String> ocspUris;

    @Option(name = "--crl-uri",
            multiValued = true,
            description = "CRL distribution point URI or 'NULL'\n"
                    + "(multi-valued)")
    private List<String> crlUris;

    @Option(name = "--deltacrl-uri",
            multiValued = true,
            description = "delta CRL distribution point URI or 'NULL'\n"
                    + "(multi-valued)")
    private List<String> deltaCrlUris;

    @Option(name = "--permission",
            multiValued = true,
            description = "permission\n"
                    + "(multi-valued)")
    @Completion(PermissionCompleter.class)
    private Set<String> permissions;

    @Option(name = "--max-validity",
            description = "maximal validity")
    private String maxValidity;

    @Option(name = "--expiration-period",
            description = "days before expiration time of CA to issue certificates")
    private Integer expirationPeriod;

    @Option(name = "--keep-expired-certs",
            description = "days to keep expired certificates")
    private Integer keepExpiredCertInDays;

    @Option(name = "--crl-signer",
            description = "CRL signer name or 'NULL'")
    @Completion(CrlSignerNamePlusNullCompleter.class)
    private String crlSignerName;

    @Option(name = "--responder",
            description = "responder name or 'NULL'")
    @Completion(ResponderNamePlusNullCompleter.class)
    private String responderName;

    @Option(name = "--cmp-control",
            description = "CMP control name or 'NULL'")
    @Completion(CmpControlNamePlusNullCompleter.class)
    private String cmpControlName;

    @Option(name = "--num-crls",
            description = "number of CRLs to be kept in database")
    private Integer numCrls;

    @Option(name = "--cert",
            description = "CA certificate file")
    @Completion(FilePathCompleter.class)
    private String certFile;

    @Option(name = "--signer-type",
            description = "CA signer type")
    @Completion(SignerTypeCompleter.class)
    private String signerType;

    @Option(name = "--signer-conf",
            description = "CA signer configuration or 'NULL'")
    private String signerConf;

    @Option(name = "--duplicate-key",
            description = "whether duplicate key is permitted")
    @Completion(YesNoCompleter.class)
    private String duplicateKeyS;

    @Option(name = "--duplicate-subject",
            description = "whether duplicate subject is permitted")
    @Completion(YesNoCompleter.class)
    private String duplicateSubjectS;

    @Option(name = "--save-req",
            description = "whether the request is saved")
    @Completion(YesNoCompleter.class)
    private String saveReqS = "yes";

    @Option(name = "--validity-mode",
            description = "mode of valditity")
    @Completion(ValidityModeCompleter.class)
    private String validityModeS;

    @Option(name = "--extra-control",
            description = "extra control")
    private String extraControl;

    @Reference
    private PasswordResolver passwordResolver;

    protected X509ChangeCaEntry getChangeCaEntry() throws Exception {
        X509ChangeCaEntry entry = new X509ChangeCaEntry(new NameId(null, caName));

        if (snBitLen != null) {
            ParamUtil.requireRange("sn-bitlen", snBitLen, 63, 159);
            entry.setSerialNoBitLen(snBitLen);
        }

        if (caStatus != null) {
            entry.setStatus(CaStatus.forName(caStatus));
        }

        if (expirationPeriod != null && expirationPeriod < 0) {
            throw new IllegalCmdParamException("invalid expirationPeriod: " + expirationPeriod);
        } else {
            entry.setExpirationPeriod(expirationPeriod);
        }

        if (keepExpiredCertInDays != null) {
            entry.setKeepExpiredCertInDays(keepExpiredCertInDays);
        }

        if (certFile != null) {
            entry.setCert(X509Util.parseCert(certFile));
        }

        if (signerConf != null) {
            String tmpSignerType = signerType;
            if (tmpSignerType == null) {
                CaEntry caEntry = caManager.getCa(caName);
                if (caEntry == null) {
                    throw new IllegalCmdParamException("please specify the signerType");
                }
                tmpSignerType = caEntry.signerType();
            }

            signerConf = ShellUtil.canonicalizeSignerConf(tmpSignerType, signerConf,
                    passwordResolver, securityFactory);
            entry.setSignerConf(signerConf);
        }

        if (duplicateKeyS != null) {
            boolean permitted = isEnabled(duplicateKeyS, true, "duplicate-key");
            entry.setDuplicateKeyPermitted(permitted);
        }

        if (duplicateSubjectS != null) {
            boolean permitted = isEnabled(duplicateSubjectS, true, "duplicate-subject");
            entry.setDuplicateSubjectPermitted(permitted);
        }

        if (saveReqS != null) {
            boolean saveReq = isEnabled(saveReqS, true, "save-req");
            entry.setSaveRequest(saveReq);
        }

        if (CollectionUtil.isNonEmpty(permissions)) {
            int intPermission = ShellUtil.getPermission(permissions);
            entry.setPermission(intPermission);
        }

        entry.setCrlUris(getUris(crlUris));
        entry.setDeltaCrlUris(getUris(deltaCrlUris));
        entry.setOcspUris(getUris(ocspUris));
        entry.setCaCertUris(getUris(caCertUris));

        if (validityModeS != null) {
            ValidityMode validityMode = ValidityMode.forName(validityModeS);
            entry.setValidityMode(validityMode);
        }

        if (maxValidity != null) {
            entry.setMaxValidity(CertValidity.getInstance(maxValidity));
        }

        if (crlSignerName != null) {
            entry.setCrlSignerName(crlSignerName);
        }

        if (cmpControlName != null) {
            entry.setCmpControlName(cmpControlName);
        }

        if (responderName != null) {
            entry.setResponderName(responderName);
        }

        if (extraControl != null) {
            entry.setExtraControl(extraControl);
        }

        if (numCrls != null) {
            entry.setNumCrls(numCrls);
        }

        return entry;
    } // method getChangeCaEntry

    @Override
    protected Object execute0() throws Exception {
        boolean bo = caManager.changeCa(getChangeCaEntry());
        output(bo, "updated", "could not update", "CA " + caName);
        return null;
    }

    private static List<String> getUris(final List<String> uris) {
        if (uris == null) {
            return null;
        }

        boolean clearUris = false;
        for (String uri : uris) {
            if (CaManager.NULL.equalsIgnoreCase(uri)) {
                clearUris = true;
                break;
            }
        }

        return clearUris ? Collections.emptyList() : new ArrayList<>(uris);
    }

}
