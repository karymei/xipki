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

package org.xipki.ca.dbtool.diffdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.dbtool.diffdb.io.CaEntry;
import org.xipki.ca.dbtool.diffdb.io.CertsBundle;
import org.xipki.ca.dbtool.diffdb.io.DbDigestEntry;
import org.xipki.common.util.ParamUtil;
import org.xipki.datasource.springframework.dao.DataAccessException;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class FileDigestReader implements DigestReader {

    private static final Logger LOG = LoggerFactory.getLogger(FileDigestReader.class);

    private final int totalAccount;

    private final String caDirname;

    private final String caSubjectName;

    private final X509Certificate caCert;

    private final BufferedReader certsFilesReader;

    private BufferedReader certsReader;

    private DbDigestEntry next;

    private final int numCertsInOneBlock;

    public FileDigestReader(final String caDirname, int numCertsInOneBlock)
            throws IOException, CertificateException {
        this.caDirname = ParamUtil.requireNonBlank("caDirname", caDirname);
        this.numCertsInOneBlock = ParamUtil.requireMin("numCertsInOneBlock", numCertsInOneBlock, 1);

        this.caCert = X509Util.parseCert(new File(caDirname, "ca.der"));
        Properties props = new Properties();
        props.load(new FileInputStream(new File(caDirname, CaEntry.FILENAME_OVERVIEW)));
        String accoutPropKey = CaEntry.PROPKEY_ACCOUNT;
        String accoutS = props.getProperty(accoutPropKey);
        this.totalAccount = Integer.parseInt(accoutS);

        this.certsFilesReader = new BufferedReader(
                new FileReader(new File(caDirname, "certs.mf")));
        this.caSubjectName = X509Util.getRfc4519Name(this.caCert.getSubjectX500Principal());
        this.next = retrieveNext(true);
    }

    @Override
    public X509Certificate caCert() {
        return caCert;
    }

    @Override
    public String caSubjectName() {
        return this.caSubjectName;
    }

    @Override
    public int totalAccount() {
        return totalAccount;
    }

    @Override
    public synchronized CertsBundle nextCerts()
            throws DataAccessException, InterruptedException {
        if (!hasNext()) {
            return null;
        }

        List<BigInteger> serialNumbers = new ArrayList<>(numCertsInOneBlock);
        Map<BigInteger, DbDigestEntry> certs = new HashMap<>(numCertsInOneBlock);

        int ik = 0;
        while (hasNext()) {
            DbDigestEntry line;
            try {
                line = nextCert();
            } catch (IOException ex) {
                throw new DataAccessException("IOException: " + ex.getMessage());
            }

            serialNumbers.add(line.serialNumber());
            certs.put(line.serialNumber(), line);
            ik++;
            if (ik >= numCertsInOneBlock) {
                break;
            }
        }

        return (ik == 0) ? null : new CertsBundle(certs, serialNumbers);
    } // method nextCerts

    private DbDigestEntry nextCert() throws IOException {
        if (next == null) {
            throw new IllegalStateException("reach end of the stream");
        }

        DbDigestEntry ret = next;
        next = null;
        next = retrieveNext(false);
        return ret;
    }

    private DbDigestEntry retrieveNext(final boolean firstTime) throws IOException {
        String line = firstTime ? null : certsReader.readLine();
        if (line == null) {
            closeReader(certsReader);
            String nextFileName = certsFilesReader.readLine();
            if (nextFileName == null) {
                return null;
            }
            String filePath = "certs" + File.separator + nextFileName;
            certsReader = new BufferedReader(new FileReader(new File(caDirname, filePath)));
            line = certsReader.readLine();
        }

        return (line == null) ? null : DbDigestEntry.decode(line);
    }

    @Override
    public void close() {
        closeReader(certsFilesReader);
        closeReader(certsReader);
    }

    private boolean hasNext() {
        return next != null;
    }

    private static void closeReader(final Reader reader) {
        if (reader == null) {
            return;
        }

        try {
            reader.close();
        } catch (Exception ex) {
            LOG.warn("could not close reader: {}", ex.getMessage());
            LOG.debug("could not close reader", ex);
        }
    }

}
