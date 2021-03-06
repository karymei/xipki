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

package org.xipki.ca.dbtool.port.ca;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.CaHasProfiles;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.CaHasPublishers;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.CaHasRequestors;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Caaliases;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Cas;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Cmpcontrols;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Crlsigners;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Environments;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Profiles;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Publishers;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Requestors;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Responders;
import org.xipki.ca.dbtool.jaxb.ca.CAConfigurationType.Sceps;
import org.xipki.ca.dbtool.jaxb.ca.CaHasProfileType;
import org.xipki.ca.dbtool.jaxb.ca.CaHasPublisherType;
import org.xipki.ca.dbtool.jaxb.ca.CaHasRequestorType;
import org.xipki.ca.dbtool.jaxb.ca.CaType;
import org.xipki.ca.dbtool.jaxb.ca.CaaliasType;
import org.xipki.ca.dbtool.jaxb.ca.CmpcontrolType;
import org.xipki.ca.dbtool.jaxb.ca.CrlsignerType;
import org.xipki.ca.dbtool.jaxb.ca.EnvironmentType;
import org.xipki.ca.dbtool.jaxb.ca.ObjectFactory;
import org.xipki.ca.dbtool.jaxb.ca.ProfileType;
import org.xipki.ca.dbtool.jaxb.ca.PublisherType;
import org.xipki.ca.dbtool.jaxb.ca.RequestorType;
import org.xipki.ca.dbtool.jaxb.ca.ResponderType;
import org.xipki.ca.dbtool.jaxb.ca.ScepType;
import org.xipki.ca.dbtool.port.DbPorter;
import org.xipki.common.util.ParamUtil;
import org.xipki.common.util.XmlUtil;
import org.xipki.datasource.DataSourceWrapper;
import org.xipki.datasource.springframework.dao.DataAccessException;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

class CaConfigurationDbExporter extends DbPorter {

    private final Marshaller marshaller;

    CaConfigurationDbExporter(DataSourceWrapper datasource, Marshaller marshaller, String destDir,
            AtomicBoolean stopMe, boolean evaluateOnly) throws DataAccessException {
        super(datasource, destDir, stopMe, evaluateOnly);
        this.marshaller = ParamUtil.requireNonNull("marshaller", marshaller);
    }

    public void export() throws Exception {
        CAConfigurationType caconf = new CAConfigurationType();
        caconf.setVersion(VERSION);

        System.out.println("exporting CA configuration from database");

        exportCmpcontrol(caconf);
        exportResponder(caconf);
        exportEnvironment(caconf);
        exportCrlsigner(caconf);
        exportRequestor(caconf);
        exportPublisher(caconf);
        exportCa(caconf);
        exportProfile(caconf);
        exportCaalias(caconf);
        exportCaHasRequestor(caconf);
        exportCaHasPublisher(caconf);
        exportCaHasProfile(caconf);
        exportScep(caconf);

        JAXBElement<CAConfigurationType> root = new ObjectFactory().createCAConfiguration(caconf);
        try {
            marshaller.marshal(root, new File(baseDir, FILENAME_CA_CONFIGURATION));
        } catch (JAXBException ex) {
            throw XmlUtil.convert(ex);
        }

        System.out.println(" exported CA configuration from database");
    }

    private void exportCmpcontrol(CAConfigurationType caconf) throws DataAccessException {
        Cmpcontrols cmpcontrols = new Cmpcontrols();
        caconf.setCmpcontrols(cmpcontrols);
        System.out.println("exporting table CMPCONTROL");

        final String sql = "SELECT NAME,CONF FROM CMPCONTROL";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String name = rs.getString("NAME");
                String conf = rs.getString("CONF");

                CmpcontrolType cmpcontrol = new CmpcontrolType();
                cmpcontrols.getCmpcontrol().add(cmpcontrol);
                cmpcontrol.setName(name);
                cmpcontrol.setConf(conf);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        System.out.println(" exported table CMPCONTROL");
    } // method exportCmpcontrol

    private void exportEnvironment(CAConfigurationType caconf) throws DataAccessException {
        System.out.println("exporting table ENVIRONMENT");
        Environments environments = new Environments();
        final String sql = "SELECT NAME,VALUE2 FROM ENVIRONMENT";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String name = rs.getString("NAME");
                String value = rs.getString("VALUE2");

                EnvironmentType environment = new EnvironmentType();
                environment.setName(name);
                environment.setValue(value);
                environments.getEnvironment().add(environment);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setEnvironments(environments);
        System.out.println(" exported table ENVIRONMENT");
    } // method exportEnvironment

    private void exportCrlsigner(CAConfigurationType caconf)
            throws DataAccessException, IOException {
        System.out.println("exporting table CRLSIGNER");
        Crlsigners crlsigners = new Crlsigners();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT NAME,SIGNER_TYPE,SIGNER_CONF,SIGNER_CERT,CRL_CONTROL");
        sqlBuilder.append(" FROM CRLSIGNER");
        final String sql = sqlBuilder.toString();

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String name = rs.getString("NAME");
                String signerType = rs.getString("SIGNER_TYPE");
                String signerConf = rs.getString("SIGNER_CONF");
                String signerCert = rs.getString("SIGNER_CERT");
                String crlControl = rs.getString("CRL_CONTROL");

                CrlsignerType crlsigner = new CrlsignerType();
                crlsigner.setName(name);
                crlsigner.setSignerType(signerType);
                crlsigner.setSignerConf(
                        buildFileOrValue(signerConf, "ca-conf/signerconf-crlsigner-" + name));
                crlsigner.setSignerCert(
                        buildFileOrBase64Binary(signerCert,
                                "ca-conf/signercert-crlsigner-" + name + ".der"));
                crlsigner.setCrlControl(crlControl);

                crlsigners.getCrlsigner().add(crlsigner);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setCrlsigners(crlsigners);
        System.out.println(" exported table CRLSIGNER");
    } // method exportCrlsigner

    private void exportCaalias(CAConfigurationType caconf) throws DataAccessException {
        System.out.println("exporting table CAALIAS");
        Caaliases caaliases = new Caaliases();
        final String sql = "SELECT NAME,CA_ID FROM CAALIAS";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String name = rs.getString("NAME");
                int caId = rs.getInt("CA_ID");

                CaaliasType caalias = new CaaliasType();
                caalias.setName(name);
                caalias.setCaId(caId);

                caaliases.getCaalias().add(caalias);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setCaaliases(caaliases);
        System.out.println(" exported table CAALIAS");
    } // method exportCaalias

    private void exportRequestor(CAConfigurationType caconf)
            throws DataAccessException, IOException {
        System.out.println("exporting table REQUESTOR");
        Requestors requestors = new Requestors();
        final String sql = "SELECT ID,NAME,CERT FROM REQUESTOR";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("NAME");
                String cert = rs.getString("CERT");
                RequestorType requestor = new RequestorType();
                requestor.setId(id);
                requestor.setName(name);
                requestor.setCert(
                        buildFileOrBase64Binary(cert, "ca-conf/cert-requestor-" + name + ".der"));
                requestors.getRequestor().add(requestor);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setRequestors(requestors);
        System.out.println(" exported table REQUESTOR");
    } // method exportRequestor

    private void exportResponder(CAConfigurationType caconf)
            throws DataAccessException, IOException {
        System.out.println("exporting table RESPONDER");

        System.out.println("exporting table CRLSIGNER");
        Responders responders = new Responders();
        final String sql = "SELECT NAME,TYPE,CONF,CERT FROM RESPONDER";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String name = rs.getString("NAME");
                String type = rs.getString("TYPE");
                String conf = rs.getString("CONF");
                String cert = rs.getString("CERT");

                ResponderType responder = new ResponderType();
                responder.setName(name);
                responder.setType(type);
                responder.setConf(buildFileOrValue(conf, "ca-conf/conf-responder-" + name));
                responder.setCert(
                        buildFileOrBase64Binary(cert, "ca-conf/cert-responder-" + name + ".der"));
                responders.getResponder().add(responder);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setResponders(responders);
        System.out.println(" exported table RESPONDER");
    } // method exportResponder

    private void exportPublisher(CAConfigurationType caconf)
            throws DataAccessException, IOException {
        System.out.println("exporting table PUBLISHER");
        Publishers publishers = new Publishers();
        final String sql = "SELECT ID,NAME,TYPE,CONF FROM PUBLISHER";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("NAME");
                String type = rs.getString("TYPE");
                String conf = rs.getString("CONF");

                PublisherType publisher = new PublisherType();
                publisher.setId(id);
                publisher.setName(name);
                publisher.setType(type);
                publisher.setConf(buildFileOrValue(conf, "ca-conf/conf-publisher-" + name));

                publishers.getPublisher().add(publisher);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setPublishers(publishers);
        System.out.println(" exported table PUBLISHER");
    } // method exportPublisher

    private void exportProfile(CAConfigurationType caconf) throws DataAccessException, IOException {
        System.out.println("exporting table PROFILE");
        Profiles profiles = new Profiles();
        final String sql = "SELECT ID,NAME,ART,TYPE,CONF FROM PROFILE";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("NAME");
                int art = rs.getInt("ART");
                String type = rs.getString("TYPE");
                String conf = rs.getString("CONF");

                ProfileType profile = new ProfileType();
                profile.setId(id);
                profile.setName(name);
                profile.setArt(art);
                profile.setType(type);
                profile.setConf(buildFileOrValue(conf, "ca-conf/certprofile-" + name));

                profiles.getProfile().add(profile);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setProfiles(profiles);
        System.out.println(" exported table PROFILE");
    } // method exportProfile

    private void exportCa(CAConfigurationType caconf) throws DataAccessException, IOException {
        System.out.println("exporting table CA");
        Cas cas = new Cas();
        StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("SELECT ID,NAME,SN_SIZE,STATUS,CRL_URIS,OCSP_URIS,MAX_VALIDITY,CERT,");
        sqlBuilder.append("SIGNER_TYPE,SIGNER_CONF,CRLSIGNER_NAME,PERMISSION,NUM_CRLS,");
        sqlBuilder.append("EXPIRATION_PERIOD,KEEP_EXPIRED_CERT_DAYS,REV,RR,RT,RIT,");
        sqlBuilder.append("DUPLICATE_KEY,DUPLICATE_SUBJECT,SAVE_REQ,DELTACRL_URIS,");
        sqlBuilder.append("VALIDITY_MODE,CACERT_URIS,ART,NEXT_CRLNO,RESPONDER_NAME,");
        sqlBuilder.append("CMPCONTROL_NAME,EXTRA_CONTROL FROM CA");

        final String sql = sqlBuilder.toString();

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("NAME");
                int art = rs.getInt("ART");
                long nextCrlNo = rs.getLong("NEXT_CRLNO");
                String responderName = rs.getString("RESPONDER_NAME");
                String cmpcontrolName = rs.getString("CMPCONTROL_NAME");
                String caCertUris = rs.getString("CACERT_URIS");
                String extraControl = rs.getString("EXTRA_CONTROL");
                int serialSize = rs.getInt("SN_SIZE");
                String status = rs.getString("STATUS");
                String crlUris = rs.getString("CRL_URIS");
                String deltaCrlUris = rs.getString("DELTACRL_URIS");
                String ocspUris = rs.getString("OCSP_URIS");
                String maxValidity = rs.getString("MAX_VALIDITY");
                String cert = rs.getString("CERT");
                String signerType = rs.getString("SIGNER_TYPE");
                String signerConf = rs.getString("SIGNER_CONF");
                String crlsignerName = rs.getString("CRLSIGNER_NAME");
                int duplicateKey = rs.getInt("DUPLICATE_KEY");
                int duplicateSubject = rs.getInt("DUPLICATE_SUBJECT");
                int saveReq = rs.getInt("SAVE_REQ");
                int permission = rs.getInt("PERMISSION");
                int expirationPeriod = rs.getInt("EXPIRATION_PERIOD");
                int keepExpiredCertDays = rs.getInt("KEEP_EXPIRED_CERT_DAYS");
                String validityMode = rs.getString("VALIDITY_MODE");

                CaType ca = new CaType();
                ca.setId(id);
                ca.setName(name);
                ca.setArt(art);
                ca.setSnSize(serialSize);
                ca.setNextCrlNo(nextCrlNo);
                ca.setStatus(status);
                ca.setCrlUris(crlUris);
                ca.setDeltacrlUris(deltaCrlUris);
                ca.setOcspUris(ocspUris);
                ca.setCacertUris(caCertUris);
                ca.setMaxValidity(maxValidity);
                ca.setCert(buildFileOrBase64Binary(cert, "ca-conf/cert-ca-" + name + ".der"));
                ca.setSignerType(signerType);
                ca.setSignerConf(buildFileOrValue(signerConf, "ca-conf/signerconf-ca-" + name));
                ca.setCrlsignerName(crlsignerName);
                ca.setResponderName(responderName);
                ca.setCmpcontrolName(cmpcontrolName);
                ca.setDuplicateKey(duplicateKey);
                ca.setDuplicateSubject(duplicateSubject);
                ca.setSaveReq(saveReq);
                ca.setPermission(permission);
                ca.setExpirationPeriod(expirationPeriod);
                ca.setKeepExpiredCertDays(keepExpiredCertDays);
                ca.setValidityMode(validityMode);
                ca.setExtraControl(extraControl);

                int numCrls = rs.getInt("NUM_CRLS");
                ca.setNumCrls(numCrls);

                boolean revoked = rs.getBoolean("REV");
                ca.setRevoked(revoked);
                if (revoked) {
                    int reason = rs.getInt("RR");
                    long revTime = rs.getLong("RT");
                    long revInvalidityTime = rs.getLong("RIT");
                    ca.setRevReason(reason);
                    ca.setRevTime(revTime);
                    ca.setRevInvTime(revInvalidityTime);
                }

                cas.getCa().add(ca);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setCas(cas);
        System.out.println(" exported table CA");
    } // method exportCa

    private void exportCaHasRequestor(CAConfigurationType caconf) throws DataAccessException {
        System.out.println("exporting table CA_HAS_REQUESTOR");
        CaHasRequestors caHasRequestors = new CaHasRequestors();
        final String sql = "SELECT CA_ID,REQUESTOR_ID,RA,PERMISSION,PROFILES FROM CA_HAS_REQUESTOR";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int caId = rs.getInt("CA_ID");
                int requestorId = rs.getInt("REQUESTOR_ID");
                boolean ra = rs.getBoolean("RA");
                int permission = rs.getInt("PERMISSION");
                String profiles = rs.getString("PROFILES");

                CaHasRequestorType caHasRequestor = new CaHasRequestorType();
                caHasRequestor.setCaId(caId);
                caHasRequestor.setRequestorId(requestorId);
                caHasRequestor.setRa(ra);
                caHasRequestor.setPermission(permission);
                caHasRequestor.setProfiles(profiles);

                caHasRequestors.getCaHasRequestor().add(caHasRequestor);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setCaHasRequestors(caHasRequestors);
        System.out.println(" exported table CA_HAS_REQUESTOR");
    } // method exportCaHasRequestor

    private void exportCaHasPublisher(CAConfigurationType caconf) throws DataAccessException {
        System.out.println("exporting table CA_HAS_PUBLISHER");
        CaHasPublishers caHasPublishers = new CaHasPublishers();
        final String sql = "SELECT CA_ID,PUBLISHER_ID FROM CA_HAS_PUBLISHER";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int caId = rs.getInt("CA_ID");
                int publisherId = rs.getInt("PUBLISHER_ID");

                CaHasPublisherType caHasPublisher = new CaHasPublisherType();
                caHasPublisher.setCaId(caId);
                caHasPublisher.setPublisherId(publisherId);

                caHasPublishers.getCaHasPublisher().add(caHasPublisher);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setCaHasPublishers(caHasPublishers);
        System.out.println(" exported table CA_HAS_PUBLISHER");
    } // method exportCaHasPublisher

    private void exportScep(CAConfigurationType caconf) throws DataAccessException, IOException {
        System.out.println("exporting table SCEP");
        Sceps sceps = new Sceps();
        caconf.setSceps(sceps);

        final String sql = "SELECT NAME,CA_ID,ACTIVE,PROFILES,RESPONDER_TYPE,"
                + "RESPONDER_CONF,RESPONDER_CERT,CONTROL FROM SCEP";

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String name = rs.getString("NAME");
                int caId = rs.getInt("CA_ID");
                int active = rs.getInt("ACTIVE");
                String profiles = rs.getString("PROFILES");
                String respType = rs.getString("RESPONDER_TYPE");
                String respConf = rs.getString("RESPONDER_CONF");
                String respCert = rs.getString("RESPONDER_CERT");
                String control = rs.getString("CONTROL");

                ScepType scep = new ScepType();
                scep.setName(name);
                scep.setCaId(caId);
                scep.setActive(active);
                scep.setProfiles(profiles);
                scep.setResponderType(respType);
                scep.setResponderConf(
                        buildFileOrValue(respConf, "ca-conf/responderconf-scep-" + caId));
                scep.setResponderCert(
                        buildFileOrBase64Binary(respCert,
                                "ca-conf/respondercert-scep-" + caId + ".der"));
                scep.setControl(control);
                sceps.getScep().add(scep);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        System.out.println(" exported table SCEP");
    } // method exportScep

    private void exportCaHasProfile(CAConfigurationType caconf) throws DataAccessException {
        System.out.println("exporting table CA_HAS_PROFILE");
        CaHasProfiles caHasProfiles = new CaHasProfiles();
        StringBuilder sqlBuilder = new StringBuilder(100);
        sqlBuilder.append("SELECT CA_ID,PROFILE_ID FROM CA_HAS_PROFILE");
        final String sql = sqlBuilder.toString();

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int caId = rs.getInt("CA_ID");
                int profileId = rs.getInt("PROFILE_ID");

                CaHasProfileType caHasProfile = new CaHasProfileType();
                caHasProfile.setCaId(caId);
                caHasProfile.setProfileId(profileId);

                caHasProfiles.getCaHasProfile().add(caHasProfile);
            }
        } catch (SQLException ex) {
            throw translate(sql, ex);
        } finally {
            releaseResources(stmt, rs);
        }

        caconf.setCaHasProfiles(caHasProfiles);
        System.out.println(" exported table CA_HAS_PROFILE");
    } // method exportCaHasProfile

}
