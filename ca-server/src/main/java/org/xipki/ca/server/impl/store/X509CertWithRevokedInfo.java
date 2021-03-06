/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ca.server.impl.store;

import org.xipki.ca.api.X509CertWithDbId;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class X509CertWithRevokedInfo {

    private final X509CertWithDbId cert;

    private final boolean revoked;

    public X509CertWithRevokedInfo(X509CertWithDbId cert, boolean revoked) {
        this.cert = ParamUtil.requireNonNull("cert", cert);
        this.revoked = revoked;
    }

    public X509CertWithDbId cert() {
        return cert;
    }

    public boolean isRevoked() {
        return revoked;
    }

}
