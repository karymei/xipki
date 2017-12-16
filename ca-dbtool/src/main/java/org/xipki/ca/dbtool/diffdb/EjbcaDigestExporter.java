/*
 *
 * Copyright (c) 2013 - 2017 Lijun Liao
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

package org.xipki.ca.dbtool.diffdb;

import java.io.File;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.dbtool.DbToolBase;
import org.xipki.ca.dbtool.IdRange;
import org.xipki.ca.dbtool.diffdb.io.CaEntry;
import org.xipki.ca.dbtool.diffdb.io.CaEntryContainer;
import org.xipki.ca.dbtool.diffdb.io.DbDigestEntry;
import org.xipki.ca.dbtool.diffdb.io.DbSchemaType;
import org.xipki.ca.dbtool.diffdb.io.EjbcaCaCertExtractor;
import org.xipki.ca.dbtool.diffdb.io.EjbcaCaInfo;
import org.xipki.ca.dbtool.diffdb.io.EjbcaDigestExportReader;
import org.xipki.ca.dbtool.diffdb.io.IdentifiedDbDigestEntry;
import org.xipki.common.ProcessLog;
import org.xipki.common.util.Base64;
import org.xipki.common.util.IoUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.datasource.DataSourceWrapper;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class EjbcaDigestExporter extends DbToolBase implements DbDigestExporter {

    private static final Logger LOG = LoggerFactory.getLogger(EjbcaDigestExporter.class);

    private final int numCertsPerSelect;

    private final boolean tblCertHasId;

    private final String sql;

    private final String certSql;

    private final int numThreads;

    public EjbcaDigestExporter(final DataSourceWrapper datasource, final String baseDir,
            final AtomicBoolean stopMe, final int numCertsPerSelect,
            final DbSchemaType dbSchemaType, final int numThreads) throws Exception {
        super(datasource, baseDir, stopMe);
        this.numCertsPerSelect = ParamUtil.requireMin("numCertsPerSelect", numCertsPerSelect, 1);

        if (dbSchemaType != DbSchemaType.EJBCA_CA_v3) {
            throw new IllegalArgumentException("unsupported DbSchemaType " + dbSchemaType);
        }

        // detect whether the table CertificateData has the column id
        if (datasource.tableHasColumn(connection, "CertificateData", "id")) {
            tblCertHasId = true;
            sql = null;
            certSql = null;
            this.numThreads = Math.min(numThreads, datasource.maximumPoolSize() - 1);
        } else {
            String lang = System.getenv("LANG");
            if (lang == null) {
                throw new Exception("no environment LANG is set");
            }

            String loLang = lang.toLowerCase();
            if (!loLang.startsWith("en_") || !loLang.endsWith(".utf-8")) {
                throw new Exception(String.format(
                        "The environment LANG does not satisfy the pattern 'en_*.UTF-8': '%s'",
                        lang));
            }

            String osName = System.getProperty("os.name");
            if (!osName.toLowerCase().contains("linux")) {
                throw new Exception(String.format(
                        "Exporting EJBCA database is only possible in Linux, but not '%s'",
                        osName));
            }

            tblCertHasId = false;
            String coreSql = "fingerprint,serialNumber,cAFingerprint,status,revocationReason, "
                    + "revocationDate FROM CertificateData WHERE fingerprint>?";
            sql = datasource.buildSelectFirstSql(numCertsPerSelect, "fingerprint ASC", coreSql);
            certSql = "SELECT base64Cert FROM CertificateData WHERE fingerprint=?";

            this.numThreads = 1;
        }

        if (this.numThreads != numThreads) {
            LOG.info("adapted the numThreads from {} to {}", numThreads, this.numThreads);
        }
    } // constructor

    @Override
    public void digest() throws Exception {
        System.out.println("digesting database");

        final long total = count("CertificateData");
        ProcessLog processLog = new ProcessLog(total);

        Map<String, EjbcaCaInfo> cas = getCas();
        Set<CaEntry> caEntries = new HashSet<>(cas.size());

        for (EjbcaCaInfo caInfo : cas.values()) {
            CaEntry caEntry = new CaEntry(caInfo.caId(),
                    baseDir + File.separator + caInfo.caDirname());
            caEntries.add(caEntry);
        }

        CaEntryContainer caEntryContainer = new CaEntryContainer(caEntries);

        Exception exception = null;
        try {
            if (tblCertHasId) {
                EjbcaDigestExportReader certsReader = new EjbcaDigestExportReader(datasource, cas,
                        numThreads);
                digestWithTableId(certsReader, processLog, caEntryContainer, cas);
            } else {
                digestNoTableId(processLog, caEntryContainer, cas);
            }
        } catch (Exception ex) {
            // delete the temporary files
            deleteTmpFiles(baseDir, "tmp-");
            System.err.println("\ndigesting process has been cancelled due to error");
            LOG.error("Exception", ex);
            exception = ex;
        } finally {
            caEntryContainer.close();
        }

        if (exception == null) {
            System.out.println(" digested database");
        } else {
            throw exception;
        }
    } // method digest

    private Map<String, EjbcaCaInfo> getCas() throws Exception {
        Map<String, EjbcaCaInfo> cas = new HashMap<>();
        final String selectSql = "SELECT NAME,DATA FROM CAData";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(selectSql);
            int caId = 0;

            while (rs.next()) {
                String name = rs.getString("NAME");
                String data = rs.getString("DATA");
                if (name == null || name.isEmpty()) {
                    continue;
                }

                X509Certificate cert = EjbcaCaCertExtractor.extractCaCert(data);
                String commonName = X509Util.getCommonName(cert.getSubjectX500Principal());
                String fn = XipkiDigestExporter.toAsciiFilename("ca-" + commonName);
                File caDir = new File(baseDir, fn);
                int idx = 2;
                while (caDir.exists()) {
                    caDir = new File(baseDir, fn + "." + (idx++));
                }

                // find out the id
                caId++;
                File caCertFile = new File(caDir, "ca.der");
                caDir.mkdirs();
                byte[] certBytes = cert.getEncoded();
                IoUtil.save(caCertFile, certBytes);
                EjbcaCaInfo caInfo = new EjbcaCaInfo(caId, certBytes, caDir.getName());
                cas.put(caInfo.hexSha1(), caInfo);
            }
        } catch (SQLException ex) {
            throw translate(selectSql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        return cas;
    } // method getCas

    private void digestNoTableId(final ProcessLog processLog,
            final CaEntryContainer caEntryContainer, final Map<String, EjbcaCaInfo> caInfos)
            throws Exception {
        int skippedAccount = 0;
        String lastProcessedHexCertFp;

        lastProcessedHexCertFp = Hex.toHexString(new byte[20]); // 40 zeros
        System.out.println("digesting certificates from fingerprint (exclusive)\n\t"
                + lastProcessedHexCertFp);

        PreparedStatement ps = prepareStatement(sql);
        PreparedStatement rawCertPs = prepareStatement(certSql);

        processLog.printHeader();

        String tmpSql = null;
        int id = 0;

        try {
            boolean interrupted = false;
            String hexCertFp = lastProcessedHexCertFp;

            while (true) {
                if (stopMe.get()) {
                    interrupted = true;
                    break;
                }

                ps.setString(1, hexCertFp);
                ResultSet rs = ps.executeQuery();

                int countEntriesInResultSet = 0;
                while (rs.next()) {
                    id++;
                    countEntriesInResultSet++;
                    String hexCaFp = rs.getString("cAFingerprint");
                    hexCertFp = rs.getString("fingerprint");

                    EjbcaCaInfo caInfo = null;

                    if (!hexCaFp.equals(hexCertFp)) {
                        caInfo = caInfos.get(hexCaFp);
                    }

                    if (caInfo == null) {
                        LOG.debug("Found no CA by cAFingerprint, try to resolve by issuer");
                        rawCertPs.setString(1, hexCertFp);

                        ResultSet certRs = rawCertPs.executeQuery();

                        if (certRs.next()) {
                            String b64Cert = certRs.getString("base64Cert");
                            Certificate cert = Certificate.getInstance(Base64.decode(b64Cert));
                            for (EjbcaCaInfo entry : caInfos.values()) {
                                if (entry.subject().equals(cert.getIssuer())) {
                                    caInfo = entry;
                                    break;
                                }
                            }
                        }
                        certRs.close();
                    }

                    if (caInfo == null) {
                        LOG.error("found no CA for Cert with fingerprint '{}'", hexCertFp);
                        skippedAccount++;
                        processLog.addNumProcessed(1);
                        continue;
                    }

                    String hash = Base64.encodeToString(Hex.decode(hexCertFp));

                    String str = rs.getString("serialNumber");
                    BigInteger serial = new BigInteger(str);

                    int status = rs.getInt("status");
                    boolean revoked = (status == EjbcaConstants.CERT_REVOKED
                            || status == EjbcaConstants.CERT_TEMP_REVOKED);

                    Integer revReason = null;
                    Long revTime = null;
                    Long revInvTime = null;

                    if (revoked) {
                        revReason = rs.getInt("revocationReason");
                        long revTimeInMs = rs.getLong("revocationDate");
                        // rev_time is milliseconds, convert it to seconds
                        revTime = revTimeInMs / 1000;
                    }

                    DbDigestEntry cert = new DbDigestEntry(serial, revoked, revReason, revTime,
                            revInvTime, hash);

                    caEntryContainer.addDigestEntry(caInfo.caId(), id, cert);

                    processLog.addNumProcessed(1);
                    processLog.printStatus();
                } // end while (rs.next())
                rs.close();

                if (countEntriesInResultSet == 0) {
                    break;
                }
            } // end while (true)

            if (interrupted) {
                throw new InterruptedException("interrupted by the user");
            }
        } catch (SQLException ex) {
            throw translate(tmpSql, ex);
        } finally {
            releaseResources(ps, null);
            releaseResources(rawCertPs, null);
        }

        processLog.printTrailer();

        StringBuilder sb = new StringBuilder(200);
        sb.append(" digested ").append((processLog.numProcessed() - skippedAccount))
            .append(" certificates");
        if (skippedAccount > 0) {
            sb.append(", ignored ").append(skippedAccount)
                .append(" certificates (see log for details)");
        }
        System.out.println(sb.toString());
    } // method digestNoTableId

    private void digestWithTableId(final EjbcaDigestExportReader certsReader,
            final ProcessLog processLog, final CaEntryContainer caEntryContainer,
            final Map<String, EjbcaCaInfo> caInfos) throws Exception {
        final int minCertId = (int) min("CertificateData", "id");
        final int maxCertId = (int) max("CertificateData", "id");
        System.out.println("digesting certificates from id " + minCertId);

        processLog.printHeader();

        List<IdRange> idRanges = new ArrayList<>(numThreads);
        boolean interrupted = false;

        for (int i = minCertId; i <= maxCertId;) {

            if (stopMe.get()) {
                interrupted = true;
                break;
            }

            idRanges.clear();
            for (int j = 0; j < numThreads; j++) {
                int to = i + numCertsPerSelect - 1;
                idRanges.add(new IdRange(i, to));
                i = to + 1;
                if (i > maxCertId) {
                    break; // break for (int j; ...)
                }
            }

            List<IdentifiedDbDigestEntry> certs = certsReader.readCerts(idRanges);
            for (IdentifiedDbDigestEntry cert : certs) {
                caEntryContainer.addDigestEntry(cert.caId().intValue(), cert.id(),
                        cert.content());
            }
            processLog.addNumProcessed(certs.size());
            processLog.printStatus();

            if (interrupted) {
                throw new InterruptedException("interrupted by the user");
            }
        }

        processLog.printTrailer();

        StringBuilder sb = new StringBuilder(200);
        sb.append(" digested ").append((processLog.numProcessed())).append(" certificates");

        int skippedAccount = certsReader.numSkippedCerts();
        if (skippedAccount > 0) {
            sb.append(", ignored ").append(skippedAccount)
                .append(" certificates (see log for details)");
        }
        System.out.println(sb.toString());
    } // method digestWithTableId

}
