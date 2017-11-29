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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.dbtool.diffdb.io.DbDigestEntry;
import org.xipki.common.util.IoUtil;
import org.xipki.common.util.LogUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.common.util.StringUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class DbDigestReporter {

    private static final Logger LOG = LoggerFactory.getLogger(DbDigestReporter.class);

    private final String reportDirname;

    private final BufferedWriter goodWriter;

    private final BufferedWriter diffWriter;

    private final BufferedWriter missingWriter;

    private final BufferedWriter unexpectedWriter;

    private final BufferedWriter errorWriter;

    private Date startTime;

    private AtomicInteger numGood = new AtomicInteger(0);

    private AtomicInteger numDiff = new AtomicInteger(0);

    private AtomicInteger numMissing = new AtomicInteger(0);

    private AtomicInteger numUnexpected = new AtomicInteger(0);

    private AtomicInteger numError = new AtomicInteger(0);

    public DbDigestReporter(final String reportDirname, final byte[] caCertBytes)
            throws IOException {
        this.reportDirname = ParamUtil.requireNonBlank("reportDirname", reportDirname);
        File dir = new File(reportDirname);
        dir.mkdirs();
        IoUtil.save(new File(dir, "ca.der"), caCertBytes);

        this.missingWriter = new BufferedWriter(
                new FileWriter(reportDirname + File.separator + "missing"));
        this.unexpectedWriter = new BufferedWriter(
                new FileWriter(reportDirname + File.separator + "unexpected"));
        this.diffWriter = new BufferedWriter(
                new FileWriter(reportDirname + File.separator + "diff"));
        this.goodWriter = new BufferedWriter(
                new FileWriter(reportDirname + File.separator + "good"));
        this.errorWriter = new BufferedWriter(
                new FileWriter(reportDirname + File.separator + "error"));

        start();
    }

    public void start() {
        startTime = new Date();
    }

    public String reportDirname() {
        return reportDirname;
    }

    public void addMissing(final BigInteger serialNumber) throws IOException {
        numMissing.incrementAndGet();
        writeSerialNumberLine(missingWriter, serialNumber);
    }

    public void addGood(final BigInteger serialNumber) throws IOException {
        numGood.incrementAndGet();
        writeSerialNumberLine(goodWriter, serialNumber);
    }

    public void addUnexpected(final BigInteger serialNumber) throws IOException {
        numUnexpected.incrementAndGet();
        writeSerialNumberLine(unexpectedWriter, serialNumber);
    }

    public void addDiff(final DbDigestEntry refCert, final DbDigestEntry targetCert)
            throws IOException {
        ParamUtil.requireNonNull("refCert", refCert);
        ParamUtil.requireNonNull("targetCert", targetCert);

        if (refCert.serialNumber().equals(targetCert.serialNumber())) {
            throw new IllegalArgumentException(
                    "refCert and targetCert do not have the same serialNumber");
        }

        numDiff.incrementAndGet();
        StringBuilder sb = new StringBuilder(140);
        sb.append(refCert.serialNumber().toString(16)).append('\t');
        sb.append(refCert.encodedOmitSeriaNumber()).append('\t');
        sb.append(targetCert.encodedOmitSeriaNumber()).append('\n');
        String msg = sb.toString();
        synchronized (diffWriter) {
            diffWriter.write(msg);
        }
    }

    public void addError(final String errorMessage) throws IOException {
        ParamUtil.requireNonNull("errorMessage", errorMessage);

        numError.incrementAndGet();
        StringBuilder sb = new StringBuilder(errorMessage);
        sb.append('\n');
        String msg = sb.toString();
        synchronized (errorWriter) {
            errorWriter.write(msg);
        }
    }

    public void addNoCaMatch() throws IOException {
        synchronized (errorWriter) {
            errorWriter.write("could not find corresponding CA in target to diff\n");
        }
    }

    public void close() {
        closeWriter(missingWriter);
        closeWriter(unexpectedWriter);
        closeWriter(diffWriter);
        closeWriter(goodWriter);
        closeWriter(errorWriter);

        final int sum = numGood.get() + numDiff.get() + numMissing.get() + numUnexpected.get()
                + numError.get();
        Date now = new Date();
        int durationSec = (int) ((now.getTime() - startTime.getTime()) / 1000);

        StringBuilder sb = new StringBuilder(200);
        sb.append("      sum : ")
            .append(StringUtil.formatAccount(sum, false)).append("\n");
        sb.append("      good: ")
            .append(StringUtil.formatAccount(numGood.get(), false)).append("\n");
        sb.append("      diff: ")
            .append(StringUtil.formatAccount(numDiff.get(), false)).append("\n");
        sb.append("   missing: ")
            .append(StringUtil.formatAccount(numMissing.get(), false)).append("\n");
        sb.append("unexpected: ")
        .append(StringUtil.formatAccount(numUnexpected.get(), false)).append("\n");
        sb.append("     error: ")
            .append(StringUtil.formatAccount(numError.get(), false)).append("\n");
        sb.append("  duration: ")
            .append(StringUtil.formatTime(durationSec, false)).append("\n");
        sb.append("start time: ").append(startTime).append("\n");
        sb.append("  end time: ").append(now).append("\n");
        sb.append("     speed: ");
        if (durationSec > 0) {
            sb.append(StringUtil.formatAccount(sum / durationSec, false)).append(" /s");
        } else {
            sb.append("--");
        }
        sb.append("\n");

        try {
            IoUtil.save(reportDirname + File.separator + "overview.txt", sb.toString().getBytes());
        } catch (IOException ex) {
            System.out.println("Could not write overview.txt with following content\n"
                    + sb.toString());
        }
    } // method close

    private static void writeSerialNumberLine(final BufferedWriter writer,
            final BigInteger serialNumber)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(serialNumber.toString(16)).append('\n');
        String msg = sb.toString();
        synchronized (writer) {
            writer.write(msg);
        }
    }

    private static void closeWriter(final Writer writer) {
        try {
            writer.close();
        } catch (Exception ex) {
            LogUtil.warn(LOG, ex, "could not close writer");
        }
    }

}
