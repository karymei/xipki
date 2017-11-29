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

package org.xipki.ca.server.impl.scep;

import org.xipki.common.util.ParamUtil;
import org.xipki.scep.transaction.FailInfo;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class FailInfoException extends Exception {

    public static final FailInfoException BAD_ALG = new FailInfoException(FailInfo.badAlg);

    public static final FailInfoException BAD_CERTID = new FailInfoException(FailInfo.badCertId);

    public static final FailInfoException BAD_MESSAGE_CHECK
            = new FailInfoException(FailInfo.badMessageCheck);

    public static final FailInfoException BAD_REQUEST = new FailInfoException(FailInfo.badRequest);

    public static final FailInfoException BAD_TIME = new FailInfoException(FailInfo.badTime);

    private static final long serialVersionUID = 1L;

    private final FailInfo failInfo;

    private FailInfoException(final FailInfo failInfo) {
        this.failInfo = ParamUtil.requireNonNull("failInfo", failInfo);
    }

    public FailInfo failInfo() {
        return failInfo;
    }

}
