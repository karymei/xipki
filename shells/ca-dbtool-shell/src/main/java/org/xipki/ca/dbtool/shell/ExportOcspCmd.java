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

package org.xipki.ca.dbtool.shell;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.dbtool.port.DbPortWorker;
import org.xipki.ca.dbtool.port.ocsp.OcspDbExportWorker;
import org.xipki.console.karaf.completer.DirPathCompleter;
import org.xipki.console.karaf.completer.FilePathCompleter;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "export-ocsp",
        description = "export OCSP database")
@Service
public class ExportOcspCmd extends DbPortCommandSupport {

    private static final String DFLT_DBCONF_FILE = "xipki/ca-config/ocsp-db.properties";

    @Option(name = "--db-conf",
            description = "database configuration file.")
    @Completion(FilePathCompleter.class)
    private String dbconfFile = DFLT_DBCONF_FILE;

    @Option(name = "--out-dir",
            required = true,
            description = "output directory\n"
                    + "(required)")
    @Completion(DirPathCompleter.class)
    private String outdir;

    @Option(name = "-n",
            description = "number of certificates in one zip file")
    private Integer numCertsInBundle = 10000;

    @Option(name = "-k",
            description = "number of certificates per SELECT")
    private Integer numCertsPerSelect = 100;

    @Option(name = "--resume")
    private Boolean resume = Boolean.FALSE;

    @Option(name = "--test",
            description = "just test the export, no real export")
    private Boolean testOnly = Boolean.FALSE;

    @Override
    protected DbPortWorker getDbPortWorker() throws Exception {
        return new OcspDbExportWorker(datasourceFactory, passwordResolver, dbconfFile, outdir,
                resume, numCertsInBundle, numCertsPerSelect, testOnly);
    }

}
