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

package org.xipki.ocsp.server.impl;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bouncycastle.crypto.Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.common.InvalidConfException;
import org.xipki.common.concurrent.ConcurrentBag;
import org.xipki.common.concurrent.ConcurrentBagEntry;
import org.xipki.common.util.Base64;
import org.xipki.common.util.LogUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.datasource.DataSourceWrapper;
import org.xipki.datasource.springframework.dao.DataAccessException;
import org.xipki.datasource.springframework.dao.DataIntegrityViolationException;
import org.xipki.datasource.springframework.jdbc.DuplicateKeyException;
import org.xipki.ocsp.api.RequestIssuer;
import org.xipki.ocsp.server.impl.OcspRespWithCacheInfo.ResponseCacheInfo;
import org.xipki.ocsp.server.impl.store.db.IssuerEntry;
import org.xipki.ocsp.server.impl.store.db.IssuerStore;
import org.xipki.security.AlgorithmCode;
import org.xipki.security.HashAlgoType;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.2.0
 */

class ResponseCacher {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseCacher.class);

    private static final String SQL_ADD_ISSUER = "INSERT INTO ISSUER (ID,S1C,CERT) VALUES (?,?,?)";

    private static final String SQL_SELECT_ISSUER_ID = "SELECT ID FROM ISSUER";

    private static final String SQL_DELETE_ISSUER= "DELETE FROM ISSUER WHERE ID=?";

    private static final String SQL_SELECT_ISSUER = "SELECT ID,CERT FROM ISSUER";

    private static final String SQL_DELETE_EXPIRED_RESP = "DELETE FROM OCSP WHERE THIS_UPDATE<?";

    private static final String SQL_ADD_RESP = "INSERT INTO OCSP (ID,IID,IDENT,"
            + "THIS_UPDATE,NEXT_UPDATE,RESP) VALUES (?,?,?,?,?,?)";

    private static final String SQL_UPDATE_RESP = "UPDATE OCSP SET THIS_UPDATE=?,"
            + "NEXT_UPDATE=?,RESP=? WHERE ID=?";

    private final ConcurrentBag<ConcurrentBagEntry<Digest>> idDigesters;

    private class IssuerUpdater implements Runnable {

        @Override
        public void run() {
            try {
                updateCacheStore();
            } catch (Throwable th) {
                LogUtil.error(LOG, th, "error while calling updateCacheStore()");
            }
        }

    } // class StoreUpdateService

    private class ExpiredResponsesCleaner implements Runnable {

        private boolean inProcess;

        @Override
        public void run() {
            if (inProcess) {
                return;
            }

            inProcess = true;
            long maxThisUpdate = System.currentTimeMillis() / 1000 - validity;
            try {
                int num = removeExpiredResponses(maxThisUpdate);
                LOG.info("removed {} response with thisUpdate < {}", num, maxThisUpdate);
            } catch (Throwable th) {
                LogUtil.error(LOG, th, "could not remove expired responses");
            } finally {
                inProcess = false;
            }
        } // method run

    } // class ExpiredResponsesCleaner

    private final String sqlSelectIssuerCert;

    private final String sqlSelectOcsp;

    private final boolean master;

    private final int validity;

    private final AtomicBoolean onService;

    private DataSourceWrapper datasource;

    private IssuerStore issuerStore;

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    private ScheduledFuture<?> responseCleaner;

    private ScheduledFuture<?> issuerUpdater;

    ResponseCacher(DataSourceWrapper datasource, boolean master, int validity) {
        this.datasource = ParamUtil.requireNonNull("datasource", datasource);
        this.master = master;
        this.validity = ParamUtil.requireMin("validity", validity, 1);
        this.sqlSelectIssuerCert = datasource.buildSelectFirstSql(1,
                "CERT FROM ISSUER WHERE ID=?");
        this.sqlSelectOcsp = datasource.buildSelectFirstSql(1,
                "IID,IDENT,THIS_UPDATE,NEXT_UPDATE,RESP FROM OCSP WHERE ID=?");
        this.onService = new AtomicBoolean(false);

        this.idDigesters = new ConcurrentBag<>();
        for (int i = 0; i < 20; i++) {
            Digest md = HashAlgoType.SHA1.createDigest();
            idDigesters.add(new ConcurrentBagEntry<Digest>(md));
        }
    }

    boolean isOnService() {
        return onService.get() && issuerStore != null;
    }

    void init() {
        updateCacheStore();

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);

        // check every 600 seconds (10 minutes)
        this.responseCleaner = scheduledThreadPoolExecutor.scheduleAtFixedRate(
                new ExpiredResponsesCleaner(), 348, 600, TimeUnit.SECONDS);

        // check every 600 seconds (10 minutes)
        this.issuerUpdater = scheduledThreadPoolExecutor.scheduleAtFixedRate(
                new IssuerUpdater(), 448, 600, TimeUnit.SECONDS);
    }

    void shutdown() {
        if (datasource != null) {
            datasource.close();
            datasource = null;
        }

        if (responseCleaner != null) {
            responseCleaner.cancel(false);
            responseCleaner = null;
        }

        if (issuerUpdater != null) {
            issuerUpdater.cancel(false);
            issuerUpdater = null;
        }

        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdown();
            while (!scheduledThreadPoolExecutor.isTerminated()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    LOG.error("interrupted: {}", ex.getMessage());
                }
            }
            scheduledThreadPoolExecutor = null;
        }
    }

    Integer getIssuerId(RequestIssuer reqIssuer) {
        IssuerEntry issuer = issuerStore.getIssuerForFp(reqIssuer);
        return (issuer == null) ? null : issuer.id();
    }

    synchronized Integer storeIssuer(X509Certificate issuerCert)
            throws CertificateException, InvalidConfException, DataAccessException {
        if (!master) {
            throw new IllegalStateException("storeIssuer is not permitted in slave mode");
        }

        for (Integer id : issuerStore.ids()) {
            if (issuerStore.getIssuerForId(id).cert().equals(issuerCert)) {
                return id;
            }
        }

        byte[] encodedCert = issuerCert.getEncoded();
        String sha1FpCert = HashAlgoType.SHA1.base64Hash(encodedCert);

        int maxId = (int) datasource.getMax(null, "ISSUER", "ID");
        int id = maxId + 1;
        try {
            final String sql = SQL_ADD_ISSUER;
            PreparedStatement ps = null;
            try {
                ps = prepareStatement(sql);
                int idx = 1;
                ps.setInt(idx++, id);
                ps.setString(idx++, sha1FpCert);
                ps.setString(idx++, Base64.encodeToString(encodedCert));

                ps.execute();

                IssuerEntry newInfo = new IssuerEntry(id, issuerCert);
                issuerStore.addIssuer(newInfo);
                return id;
            } catch (SQLException ex) {
                throw datasource.translate(sql, ex);
            } finally {
                datasource.releaseResources(ps, null);
            }
        } catch (DataAccessException ex) {
            if (ex instanceof DuplicateKeyException) {
                return id;
            }
            throw ex;
        }
    }

    OcspRespWithCacheInfo getOcspResponse(int issuerId, BigInteger serialNumber,
            AlgorithmCode sigAlg, AlgorithmCode certHashAlg)
            throws DataAccessException {
        final String sql = sqlSelectOcsp;
        byte[] identBytes = buildIdent(serialNumber, sigAlg, certHashAlg);
        long id = deriveId(issuerId, identBytes);
        PreparedStatement ps = prepareStatement(sql);
        ResultSet rs = null;

        try {
            ps.setLong(1, id);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }

            int dbIid = rs.getInt("IID");
            if (dbIid != issuerId) {
                return null;
            }

            String ident = Base64.encodeToString(identBytes);
            String dbIdent = rs.getString("IDENT");
            if (!ident.equals(dbIdent)) {
                return null;
            }

            long nextUpdate = rs.getLong("NEXT_UPDATE");
            if (nextUpdate != 0) {
                // nextUpdate must be at least in 600 seconds
                long minNextUpdate = System.currentTimeMillis() / 1000 + 600;

                if (nextUpdate < minNextUpdate) {
                    return null;
                }
            }

            long thisUpdate = rs.getLong("THIS_UPDATE");
            String b64Resp = rs.getString("RESP");
            byte[] encoded = Base64.decodeFast(b64Resp);
            ResponseCacheInfo cacheInfo = new ResponseCacheInfo(thisUpdate);
            if (nextUpdate != 0) {
                cacheInfo.setNextUpdate(nextUpdate);
            }
            return new OcspRespWithCacheInfo(encoded, cacheInfo);
        } catch (SQLException ex) {
            throw datasource.translate(sql, ex);
        } finally {
            datasource.releaseResources(ps, rs);
        }
    }

    void storeOcspResponse(int issuerId, BigInteger serialNumber, long thisUpdate,
            Long nextUpdate, AlgorithmCode sigAlgCode, AlgorithmCode certHashAlgCode,
            byte[] response) {
        byte[] identBytes = buildIdent(serialNumber, sigAlgCode, certHashAlgCode);
        String ident = Base64.encodeToString(identBytes);
        try {
            long id = deriveId(issuerId, identBytes);

            Connection conn = datasource.getConnection();
            try {
                String sql = SQL_ADD_RESP;
                PreparedStatement ps = datasource.prepareStatement(conn, sql);

                String b64Response = Base64.encodeToString(response);
                Boolean dataIntegrityViolationException = null;
                try {
                    int idx = 1;
                    ps.setLong(idx++, id);
                    ps.setInt(idx++, issuerId);
                    ps.setString(idx++, ident);
                    ps.setLong(idx++, thisUpdate);
                    if (nextUpdate != null && nextUpdate > 0) {
                        ps.setLong(idx++, nextUpdate);
                    } else {
                        ps.setNull(idx++, java.sql.Types.BIGINT);
                    }
                    ps.setString(idx++, b64Response);
                    ps.execute();
                } catch (SQLException ex) {
                    DataAccessException dex = datasource.translate(sql, ex);
                    if (dex instanceof DataIntegrityViolationException) {
                        dataIntegrityViolationException = Boolean.TRUE;
                    } else {
                        throw dex;
                    }
                } finally {
                    datasource.releaseResources(ps, null, false);
                }

                if (dataIntegrityViolationException == null) {
                    LOG.debug("added cached OCSP response iid={}, ident={}", issuerId, ident);
                    return;
                }

                sql = SQL_UPDATE_RESP;
                ps = datasource.prepareStatement(conn, sql);
                try {
                    int idx = 1;
                    ps.setLong(idx++, thisUpdate);
                    if (nextUpdate != null && nextUpdate > 0) {
                        ps.setLong(idx++, nextUpdate);
                    } else {
                        ps.setNull(idx++, java.sql.Types.BIGINT);
                    }
                    ps.setString(idx++, b64Response);
                    ps.setLong(idx++, id);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    throw datasource.translate(sql, ex);
                } finally {
                    datasource.releaseResources(ps, null, false);
                }
            } finally {
                datasource.returnConnection(conn);
            }
        } catch (DataAccessException ex) {
            LOG.info("could not cache OCSP response iid={}, ident={}", issuerId, ident);
            if (LOG.isDebugEnabled()) {
                LOG.debug("could not cache OCSP response iid=" + issuerId + ", ident=" + ident, ex);
            }
        }
    }

    private int removeExpiredResponses(long maxThisUpdate) throws DataAccessException {
        final String sql = SQL_DELETE_EXPIRED_RESP;
        PreparedStatement ps = null;
        try {
            ps = prepareStatement(sql);
            ps.setLong(1, maxThisUpdate);
            return ps.executeUpdate();
        } catch (SQLException ex) {
            throw datasource.translate(sql, ex);
        } finally {
            datasource.releaseResources(ps, null);
        }
    }

    private void updateCacheStore() {
        boolean stillOnService = updateCacheStore0();
        this.onService.set(stillOnService);
        if (!stillOnService) {
            LOG.error("OCSP response cacher is out of service");
        } else {
            LOG.info("OCSP response cacher is on service");
        }
    }

    /**
     * update the cache store.
     * @return whether the ResponseCacher is on service.
     */
    private boolean updateCacheStore0() {
        try {
            if (this.issuerStore == null) {
                return initIssuerStore();
            }

            // check for new issuers
            PreparedStatement ps = null;
            ResultSet rs = null;

            Set<Integer> ids = new HashSet<>();
            try {
                ps = prepareStatement(SQL_SELECT_ISSUER_ID);
                rs = ps.executeQuery();

                if (master) {
                    // If in master mode, the issuers are always up-to-date. Here just to check
                    // whether the database is accessible
                    return true;
                }

                while (rs.next()) {
                    ids.add(rs.getInt("ID"));
                }
            } catch (SQLException ex) {
                LogUtil.error(LOG, datasource.translate(SQL_SELECT_ISSUER_ID, ex),
                        "could not executing updateCacheStore()");
                return false;
            } catch (Exception ex) {
                LogUtil.error(LOG, ex, "could not executing updateCacheStore()");
                return false;
            } finally {
                datasource.releaseResources(ps, rs, false);
            }

            // add the new issuers
            ps = null;
            rs = null;

            Set<Integer> currentIds = issuerStore.ids();

            for (Integer id : ids) {
                if (currentIds.contains(id)) {
                    continue;
                }

                try {
                    if (ps == null) {
                        ps = prepareStatement(sqlSelectIssuerCert);
                    }

                    ps.setInt(1, id);
                    rs = ps.executeQuery();
                    rs.next();
                    String b64Cert = rs.getString("CERT");
                    X509Certificate cert = X509Util.parseBase64EncodedCert(b64Cert);
                    IssuerEntry caInfoEntry = new IssuerEntry(id, cert);
                    issuerStore.addIssuer(caInfoEntry);
                    LOG.info("added issuer {}", id);
                } catch (SQLException ex) {
                    LogUtil.error(LOG, datasource.translate(sqlSelectIssuerCert, ex),
                            "could not executing updateCacheStore()");
                    return false;
                } catch (Exception ex) {
                    LogUtil.error(LOG, ex, "could not executing updateCacheStore()");
                    return false;
                } finally {
                    datasource.releaseResources(null, rs, false);
                }
            }

            if (ps != null) {
                datasource.releaseResources(ps, null, false);
            }
        } catch (DataAccessException ex) {
            LogUtil.error(LOG, ex, "could not executing updateCacheStore()");
            return false;
        } catch (CertificateException ex) {
            // don't set the onService to false.
            LogUtil.error(LOG, ex, "could not executing updateCacheStore()");
        }

        return true;
    } // method updateCacheStore0

    private boolean initIssuerStore() throws DataAccessException, CertificateException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = prepareStatement(SQL_SELECT_ISSUER);
            rs = ps.executeQuery();
            List<IssuerEntry> caInfos = new LinkedList<>();

            PreparedStatement deleteIssuerStmt = null;

            while (rs.next()) {
                int id = rs.getInt("ID");
                String b64Cert = rs.getString("CERT");
                X509Certificate cert = X509Util.parseBase64EncodedCert(b64Cert);
                IssuerEntry caInfoEntry = new IssuerEntry(id, cert);
                RequestIssuer reqIssuer = new RequestIssuer(HashAlgoType.SHA1,
                        caInfoEntry.getEncodedHash(HashAlgoType.SHA1));

                boolean duplicated = false;
                for (IssuerEntry existingIssuer : caInfos) {
                    if (existingIssuer.matchHash(reqIssuer)) {
                        duplicated = true;
                        break;
                    }
                }

                String subject = cert.getSubjectX500Principal().getName();
                if (duplicated) {
                    if (deleteIssuerStmt == null) {
                        deleteIssuerStmt = prepareStatement(SQL_DELETE_ISSUER);
                    }

                    deleteIssuerStmt.setInt(1, id);
                    deleteIssuerStmt.executeUpdate();

                    LOG.warn("Delete duplicated issuer {}: {}", id, subject);
                } else {
                    LOG.info("added issuer {}: {}", id, subject);
                    caInfos.add(caInfoEntry);
                }
            } // end while (rs.next())

            this.issuerStore = new IssuerStore(caInfos);
            LOG.info("Updated issuers");
        } catch (SQLException ex) {
            throw datasource.translate(SQL_SELECT_ISSUER, ex);
        } finally {
            datasource.releaseResources(ps, rs, false);
        }

        return true;
    }

    private PreparedStatement prepareStatement(String sqlQuery) throws DataAccessException {
        Connection conn = datasource.getConnection();
        try {
            return datasource.prepareStatement(conn, sqlQuery);
        } catch (DataAccessException ex) {
            datasource.returnConnection(conn);
            throw ex;
        }
    }

    private static byte[] buildIdent(BigInteger serialNumber,
            AlgorithmCode sigAlg, AlgorithmCode certHashAlg) {
        byte[] snBytes = serialNumber.toByteArray();
        byte[] bytes = new byte[2 + snBytes.length];
        bytes[0] = sigAlg.code();
        bytes[1] = (certHashAlg == null) ? 0 : certHashAlg.code();
        System.arraycopy(snBytes, 0, bytes, 2, snBytes.length);
        return bytes;
    }

    private long deriveId(int issuerId, byte[] identBytes) {
        ConcurrentBagEntry<Digest> digest0 = null;
        try {
            digest0 = idDigesters.borrow(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            // do nothing
        }

        boolean newDigest = (digest0 == null);
        if (newDigest) {
            digest0 = new ConcurrentBagEntry<Digest>(HashAlgoType.SHA1.createDigest());
        }

        byte[] hash = new byte[20];
        try {
            Digest digest = digest0.value();
            digest.reset();
            digest.update(int2Bytes(issuerId), 0, 2);
            digest.update(identBytes, 0, identBytes.length);
            digest.doFinal(hash, 0);
        } finally {
            if (newDigest) {
                idDigesters.add(digest0);
            } else {
                idDigesters.requite(digest0);
            }
        }

        return  (0x7FL & hash[0]) << 56 | // ignore the first bit
                (0xFFL & hash[1]) << 48 |
                (0xFFL & hash[2]) << 40 |
                (0xFFL & hash[3]) << 32 |
                (0xFFL & hash[4]) << 24 |
                (0xFFL & hash[5]) << 16 |
                (0xFFL & hash[6]) << 8 |
                (0xFFL & hash[7]);
    }

    private static byte[] int2Bytes(int value) {
        if (value < 65535) {
            return new byte[]{(byte) (value >> 8), (byte) value};
        } else {
            throw new IllegalArgumentException("value is too large");
        }
    }

}
