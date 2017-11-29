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

import org.xipki.ca.server.mgmt.api.PermissionConstants;
import org.xipki.common.ConfPairs;
import org.xipki.common.util.Base64;
import org.xipki.common.util.IoUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.common.util.StringUtil;
import org.xipki.console.karaf.IllegalCmdParamException;
import org.xipki.password.PasswordResolver;
import org.xipki.security.SecurityFactory;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class ShellUtil {

    private ShellUtil() {
    }

    public static String canonicalizeSignerConf(final String keystoreType, final String signerConf,
            final PasswordResolver passwordResolver, final SecurityFactory securityFactory)
            throws Exception {
        ParamUtil.requireNonBlank("keystoreType", keystoreType);
        ParamUtil.requireNonBlank("signerConf", signerConf);
        ParamUtil.requireNonNull("securityFactory", securityFactory);

        if (!signerConf.contains("file:") && !signerConf.contains("base64:")
                && !signerConf.contains("FILE:") && !signerConf.contains("BASE64:")) {
            return signerConf;
        }

        ConfPairs pairs = new ConfPairs(signerConf);
        String keystoreConf = pairs.value("keystore");
        String passwordHint = pairs.value("password");

        if (passwordHint == null) {
            throw new IllegalArgumentException("password is not set in " + signerConf);
        }

        byte[] keystoreBytes;
        if (StringUtil.startsWithIgnoreCase(keystoreConf, "file:")) {
            String keystoreFile = keystoreConf.substring("file:".length());
            keystoreBytes = IoUtil.read(keystoreFile);
        } else if (StringUtil.startsWithIgnoreCase(keystoreConf, "base64:")) {
            keystoreBytes = Base64.decode(keystoreConf.substring("base64:".length()));
        } else {
            return signerConf;
        }

        char[] password;
        if (passwordResolver == null) {
            password = passwordHint.toCharArray();
        } else {
            password = passwordResolver.resolvePassword(passwordHint);
        }

        String keyLabel = pairs.value("key-label");
        keystoreBytes = securityFactory.extractMinimalKeyStore(keystoreType, keystoreBytes,
                keyLabel, password, null);

        pairs.putPair("keystore", "base64:" + Base64.encodeToString(keystoreBytes));
        return pairs.getEncoded();
    } // method execute0

    public static int getPermission(Set<String> permissions) throws IllegalCmdParamException {
        int ret = 0;
        for (String permission : permissions) {
            Integer code = PermissionConstants.getPermissionForText(permission);
            if (code == null) {
                throw new IllegalCmdParamException("invalid permission '" + permission + "'");
            }
            ret |= code;
        }
        return ret;
    }

}
