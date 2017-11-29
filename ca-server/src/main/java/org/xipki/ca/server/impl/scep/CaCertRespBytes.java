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

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSAbsentContent;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.xipki.common.util.ParamUtil;

/**
 *
 * @author Lijun Liao
 * @since 2.0.0
 */

class CaCertRespBytes {

    private final byte[] bytes;

    CaCertRespBytes(final X509Certificate caCert, final X509Certificate responderCert)
            throws CMSException, CertificateException {
        ParamUtil.requireNonNull("caCert", caCert);
        ParamUtil.requireNonNull("responderCert", responderCert);

        CMSSignedDataGenerator cmsSignedDataGen = new CMSSignedDataGenerator();
        try {
            cmsSignedDataGen.addCertificate(new X509CertificateHolder(caCert.getEncoded()));
            cmsSignedDataGen.addCertificate(new X509CertificateHolder(responderCert.getEncoded()));
            CMSSignedData degenerateSignedData = cmsSignedDataGen.generate(new CMSAbsentContent());
            bytes = degenerateSignedData.getEncoded();
        } catch (IOException ex) {
            throw new CMSException("could not build CMS SignedDta");
        }
    }

    byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

}
