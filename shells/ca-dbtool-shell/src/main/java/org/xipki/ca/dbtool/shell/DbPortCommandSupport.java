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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.xipki.ca.dbtool.port.DbPortWorker;
import org.xipki.common.util.StringUtil;
import org.xipki.console.karaf.XipkiCommandSupport;
import org.xipki.datasource.DataSourceFactory;
import org.xipki.password.PasswordResolver;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public abstract class DbPortCommandSupport extends XipkiCommandSupport {

    protected DataSourceFactory datasourceFactory;

    @Reference
    protected PasswordResolver passwordResolver;

    public DbPortCommandSupport() {
        datasourceFactory = new DataSourceFactory();
    }

    protected abstract DbPortWorker getDbPortWorker() throws Exception;

    protected Object execute0() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        DbPortWorker myRun = getDbPortWorker();
        executor.execute(myRun);

        executor.shutdown();
        while (true) {
            try {
                boolean terminated = executor.awaitTermination(1, TimeUnit.SECONDS);
                if (terminated) {
                    break;
                }
            } catch (InterruptedException ex) {
                myRun.setStopMe(true);
            }
        }

        Exception ex = myRun.exception();
        if (ex != null) {
            String errMsg = ex.getMessage();
            if (StringUtil.isBlank(errMsg)) {
                errMsg = "ERROR";
            }

            System.err.println(errMsg);
        }

        return null;
    }

}
