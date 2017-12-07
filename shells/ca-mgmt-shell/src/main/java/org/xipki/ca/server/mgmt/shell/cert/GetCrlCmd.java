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

import java.io.File;
import java.math.BigInteger;
import java.security.cert.X509CRL;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.xipki.ca.server.mgmt.api.CaEntry;
import org.xipki.console.karaf.CmdFailure;
import org.xipki.console.karaf.completer.FilePathCompleter;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "getcrl",
        description = "download CRL")
@Service
public class GetCrlCmd extends CrlCommandSupport {

    @Option(name = "--with-basecrl",
            description = "whether to retrieve the baseCRL if the current CRL is a delta CRL")
    private Boolean withBaseCrl = Boolean.FALSE;

    @Option(name = "--basecrl-out",
            description = "where to save the baseCRL\n"
                    + "(defaults to <out>-baseCRL)")
    @Completion(FilePathCompleter.class)
    private String baseCrlOut;

    @Override
    protected X509CRL retrieveCrl() throws Exception {
        return caManager.getCurrentCrl(caName);
    }

    @Override
    protected Object execute0() throws Exception {
        CaEntry ca = caManager.getCa(caName);
        if (ca == null) {
            throw new CmdFailure("CA " + caName + " not available");
        }

        X509CRL crl = null;
        try {
            crl = retrieveCrl();
        } catch (Exception ex) {
            throw new CmdFailure("received no CRL from server: " + ex.getMessage());
        }

        if (crl == null) {
            throw new CmdFailure("received no CRL from server");
        }

        saveVerbose("saved CRL to file", new File(outFile), crl.getEncoded());

        if (withBaseCrl.booleanValue()) {
            byte[] octetString = crl.getExtensionValue(Extension.deltaCRLIndicator.getId());
            if (octetString != null) {
                if (baseCrlOut == null) {
                    baseCrlOut = outFile + "-baseCRL";
                }

                byte[] extnValue = DEROctetString.getInstance(octetString).getOctets();
                BigInteger baseCrlNumber = ASN1Integer.getInstance(extnValue).getPositiveValue();

                try {
                    crl = caManager.getCrl(caName, baseCrlNumber);
                } catch (Exception ex) {
                    throw new CmdFailure("received no baseCRL from server: " + ex.getMessage());
                }

                if (crl == null) {
                    throw new CmdFailure("received no baseCRL from server");
                } else {
                    saveVerbose("saved baseCRL to file", new File(baseCrlOut), crl.getEncoded());
                }
            }
        }

        return null;
    } // method execute0

}
