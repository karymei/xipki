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

package org.xipki.ca.server.impl.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.xipki.ca.api.profile.x509.SubjectDnSpec;
import org.xipki.ca.api.profile.x509.X509CertLevel;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class CaUtil {

    private CaUtil() {
    }

    public static Extensions getExtensions(final CertificationRequestInfo csr) {
        ParamUtil.requireNonNull("csr", csr);
        ASN1Set attrs = csr.getAttributes();
        for (int i = 0; i < attrs.size(); i++) {
            Attribute attr = Attribute.getInstance(attrs.getObjectAt(i));
            if (PKCSObjectIdentifiers.pkcs_9_at_extensionRequest.equals(attr.getAttrType())) {
                return Extensions.getInstance(attr.getAttributeValues()[0]);
            }
        }
        return null;
    }

    public static String getChallengePassword(final CertificationRequestInfo csr) {
        ParamUtil.requireNonNull("csr", csr);
        ASN1Set attrs = csr.getAttributes();
        for (int i = 0; i < attrs.size(); i++) {
            Attribute attr = Attribute.getInstance(attrs.getObjectAt(i));
            if (PKCSObjectIdentifiers.pkcs_9_at_challengePassword.equals(attr.getAttrType())) {
                ASN1String str = (ASN1String) attr.getAttributeValues()[0];
                return str.getString();
            }
        }
        return null;
    }

    public static BasicConstraints createBasicConstraints(final X509CertLevel level,
            final Integer pathLen) {
        BasicConstraints basicConstraints;
        if (level == X509CertLevel.RootCA || level == X509CertLevel.SubCA) {
            basicConstraints = (pathLen != null)  ? new BasicConstraints(pathLen)
                    : new BasicConstraints(true);
        } else if (level == X509CertLevel.EndEntity) {
            basicConstraints = new BasicConstraints(false);
        } else {
            throw new RuntimeException("unknown X509CertLevel " + level);
        }
        return basicConstraints;
    }

    public static AuthorityInformationAccess createAuthorityInformationAccess(
            final List<String> caIssuerUris, final List<String> ocspUris) {
        if (CollectionUtil.isEmpty(caIssuerUris) && CollectionUtil.isEmpty(ocspUris)) {
            throw new IllegalArgumentException("caIssuerUris and ospUris must not be both empty");
        }

        List<AccessDescription> accessDescriptions = new ArrayList<>(ocspUris.size());

        if (CollectionUtil.isNonEmpty(caIssuerUris)) {
            for (String uri : caIssuerUris) {
                GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, uri);
                accessDescriptions.add(
                        new AccessDescription(X509ObjectIdentifiers.id_ad_caIssuers, gn));
            }
        }

        if (CollectionUtil.isNonEmpty(ocspUris)) {
            for (String uri : ocspUris) {
                GeneralName gn = new GeneralName(GeneralName.uniformResourceIdentifier, uri);
                accessDescriptions.add(new AccessDescription(X509ObjectIdentifiers.id_ad_ocsp, gn));
            }
        }

        DERSequence seq = new DERSequence(accessDescriptions.toArray(new AccessDescription[0]));
        return AuthorityInformationAccess.getInstance(seq);
    }

    public static CRLDistPoint createCrlDistributionPoints(final List<String> crlUris,
            final X500Name caSubject, final X500Name crlSignerSubject) {
        ParamUtil.requireNonEmpty("crlUris", crlUris);
        int size = crlUris.size();
        DistributionPoint[] points = new DistributionPoint[1];

        GeneralName[] names = new GeneralName[size];
        for (int i = 0; i < size; i++) {
            names[i] = new GeneralName(GeneralName.uniformResourceIdentifier, crlUris.get(i));
        }
        // Distribution Point
        GeneralNames gns = new GeneralNames(names);
        DistributionPointName pointName = new DistributionPointName(gns);

        GeneralNames crlIssuer = null;
        if (crlSignerSubject != null && !crlSignerSubject.equals(caSubject)) {
            GeneralName crlIssuerName = new GeneralName(crlSignerSubject);
            crlIssuer = new GeneralNames(crlIssuerName);
        }

        points[0] = new DistributionPoint(pointName, null, crlIssuer);

        return new CRLDistPoint(points);
    }

    public static X500Name sortX509Name(final X500Name name) {
        ParamUtil.requireNonNull("name", name);
        RDN[] requstedRdns = name.getRDNs();

        List<RDN> rdns = new LinkedList<>();

        List<ASN1ObjectIdentifier> sortedDNs = SubjectDnSpec.getForwardDNs();
        int size = sortedDNs.size();
        for (int i = 0; i < size; i++) {
            ASN1ObjectIdentifier type = sortedDNs.get(i);
            RDN[] thisRdns = getRdns(requstedRdns, type);
            if (thisRdns == null) {
                continue;
            }
            if (thisRdns.length == 0) {
                continue;
            }

            for (RDN m : thisRdns) {
                rdns.add(m);
            }
        }

        return new X500Name(rdns.toArray(new RDN[0]));
    }

    private static RDN[] getRdns(final RDN[] rdns, final ASN1ObjectIdentifier type) {
        ParamUtil.requireNonNull("rdns", rdns);
        ParamUtil.requireNonNull("type", type);
        List<RDN> ret = new ArrayList<>(1);
        for (int i = 0; i < rdns.length; i++) {
            RDN rdn = rdns[i];
            if (rdn.getFirst().getType().equals(type)) {
                ret.add(rdn);
            }
        }

        return CollectionUtil.isEmpty(ret) ? null : ret.toArray(new RDN[0]);
    }

}
