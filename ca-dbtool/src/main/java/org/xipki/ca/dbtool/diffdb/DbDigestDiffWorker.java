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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.dbtool.port.DbPortWorker;
import org.xipki.ca.dbtool.port.DbPorter;
import org.xipki.common.util.IoUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.common.util.StringUtil;
import org.xipki.datasource.DataSourceFactory;
import org.xipki.datasource.DataSourceWrapper;
import org.xipki.datasource.springframework.dao.DataAccessException;
import org.xipki.password.PasswordResolver;
import org.xipki.password.PasswordResolverException;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class DbDigestDiffWorker extends DbPortWorker {

    private static final Logger LOG = LoggerFactory.getLogger(DbDigestDiffWorker.class);

    private final boolean revokedOnly;

    private final String refDirname;

    private final DataSourceWrapper refDatasource;

    private final Set<byte[]> includeCaCerts;

    private final DataSourceWrapper datasource;

    private final String reportDir;

    private final int numCertsPerSelect;

    private final NumThreads numThreads;

    public DbDigestDiffWorker(final DataSourceFactory datasourceFactory,
            final PasswordResolver passwordResolver, final boolean revokedOnly,
            final String refDirname, final String refDbConfFile, final String dbConfFile,
            final String reportDirName, final int numCertsPerSelect, final NumThreads numThreads,
            final Set<byte[]> includeCaCerts)
            throws DataAccessException, PasswordResolverException, IOException, JAXBException {
        ParamUtil.requireNonNull("datasourceFactory", datasourceFactory);
        this.reportDir = ParamUtil.requireNonBlank("reportDirName", reportDirName);
        this.numThreads = ParamUtil.requireNonNull("numThreads", numThreads);
        this.numCertsPerSelect = numCertsPerSelect;
        boolean validRef = (refDirname == null) ? (refDbConfFile != null) : (refDbConfFile == null);

        if (!validRef) {
            throw new IllegalArgumentException(
                    "Exactly one of refDirname and refDbConffile must be not null");
        }

        this.includeCaCerts = includeCaCerts;

        File file = new File(reportDirName);
        if (!file.exists()) {
            file.mkdirs();
        } else {
            if (!file.isDirectory()) {
                throw new IOException(reportDirName + " is not a folder");
            }

            if (!file.canWrite()) {
                throw new IOException(reportDirName + " is not writable");
            }
        }

        String[] children = file.list();
        if (children != null && children.length > 0) {
            throw new IOException(reportDirName + " is not empty");
        }

        Properties props = DbPorter.getDbConfProperties(
                new FileInputStream(IoUtil.expandFilepath(dbConfFile)));
        this.datasource = datasourceFactory.createDataSource("ds-" + dbConfFile, props,
                passwordResolver);

        this.revokedOnly = revokedOnly;
        if (refDirname != null) {
            this.refDirname = refDirname;
            this.refDatasource = null;
        } else {
            this.refDirname = null;
            Properties refProps = DbPorter.getDbConfProperties(
                    new FileInputStream(IoUtil.expandFilepath(refDbConfFile)));
            this.refDatasource = datasourceFactory.createDataSource(
                    "ds-" + refDbConfFile, refProps, passwordResolver);
        }
    } // constructor DbDigestDiffWorker

    @Override
    protected void run0() throws Exception {
        long start = System.currentTimeMillis();

        try {
            DbDigestDiff diff = (refDirname != null)
                ? DbDigestDiff.getInstanceForDirRef(refDirname, datasource, reportDir,
                        revokedOnly, stopMe, numCertsPerSelect, numThreads)
                : DbDigestDiff.getInstanceForDbRef(refDatasource, datasource, reportDir,
                        revokedOnly, stopMe, numCertsPerSelect, numThreads);
            diff.setIncludeCaCerts(includeCaCerts);
            diff.diff();
        } finally {
            if (refDatasource != null) {
                try {
                    refDatasource.close();
                } catch (Throwable th) {
                    LOG.error("refDatasource.close()", th);
                }
            }

            try {
                datasource.close();
            } catch (Throwable th) {
                LOG.error("datasource.close()", th);
            }
            long end = System.currentTimeMillis();
            System.out.println("finished in " + StringUtil.formatTime((end - start) / 1000, false));
        }
    } // method run0

}
