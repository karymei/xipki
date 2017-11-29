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

package org.xipki.ca.dbtool.port;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.common.util.ParamUtil;
import org.xipki.common.util.StringUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class DbPortFileNameIterator implements Iterator<String> {

    private static final Logger LOG = LoggerFactory.getLogger(DbPortFileNameIterator.class);

    private BufferedReader reader;

    private String nextFilename;

    public DbPortFileNameIterator(final String filename) throws IOException {
        ParamUtil.requireNonNull("filename", filename);

        this.reader = new BufferedReader(new FileReader(filename));
        this.nextFilename = readNextFilenameLine();
    }

    @Override
    public boolean hasNext() {
        return nextFilename != null;
    }

    @Override
    public String next() {
        String str = nextFilename;
        nextFilename = null;
        try {
            nextFilename = readNextFilenameLine();
        } catch (IOException ex) {
            throw new RuntimeException("could not read next file name");
        }
        return str;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove is not supported");
    }

    public void close() {
        try {
            reader.close();
        } catch (Throwable th) {
            LOG.error("could not close reader: {}", th.getMessage());
            LOG.error("could not close reader", th);
        }
    }

    private String readNextFilenameLine() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (StringUtil.isBlank(line) || line.startsWith("#") || !line.endsWith(".zip")) {
                continue;
            }
            return line;
        }

        return null;
    }

}
