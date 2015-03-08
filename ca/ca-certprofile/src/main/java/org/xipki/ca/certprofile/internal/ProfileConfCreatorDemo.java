/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2014 - 2015 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

package org.xipki.ca.certprofile.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.w3c.dom.Element;
import org.xipki.ca.api.profile.x509.SpecialX509CertprofileBehavior;
import org.xipki.ca.certprofile.XmlX509Certprofile;
import org.xipki.ca.certprofile.internal.x509.jaxb.Admission;
import org.xipki.ca.certprofile.internal.x509.jaxb.AlgorithmType;
import org.xipki.ca.certprofile.internal.x509.jaxb.AnyType;
import org.xipki.ca.certprofile.internal.x509.jaxb.AuthorityKeyIdentifier;
import org.xipki.ca.certprofile.internal.x509.jaxb.BasicConstraints;
import org.xipki.ca.certprofile.internal.x509.jaxb.CertificatePolicies;
import org.xipki.ca.certprofile.internal.x509.jaxb.CertificatePolicyInformationType;
import org.xipki.ca.certprofile.internal.x509.jaxb.ConstantExtValue;
import org.xipki.ca.certprofile.internal.x509.jaxb.DSAParameters;
import org.xipki.ca.certprofile.internal.x509.jaxb.ECParameters;
import org.xipki.ca.certprofile.internal.x509.jaxb.ECParameters.Curves;
import org.xipki.ca.certprofile.internal.x509.jaxb.ECParameters.PointEncodings;
import org.xipki.ca.certprofile.internal.x509.jaxb.ExtendedKeyUsage;
import org.xipki.ca.certprofile.internal.x509.jaxb.ExtendedKeyUsage.Usage;
import org.xipki.ca.certprofile.internal.x509.jaxb.ExtensionType;
import org.xipki.ca.certprofile.internal.x509.jaxb.ExtensionValueType;
import org.xipki.ca.certprofile.internal.x509.jaxb.ExtensionsType;
import org.xipki.ca.certprofile.internal.x509.jaxb.GeneralNameType;
import org.xipki.ca.certprofile.internal.x509.jaxb.GeneralNameType.OtherName;
import org.xipki.ca.certprofile.internal.x509.jaxb.GeneralSubtreeBaseType;
import org.xipki.ca.certprofile.internal.x509.jaxb.GeneralSubtreesType;
import org.xipki.ca.certprofile.internal.x509.jaxb.InhibitAnyPolicy;
import org.xipki.ca.certprofile.internal.x509.jaxb.KeyParametersType;
import org.xipki.ca.certprofile.internal.x509.jaxb.KeyUsage;
import org.xipki.ca.certprofile.internal.x509.jaxb.KeyUsageEnum;
import org.xipki.ca.certprofile.internal.x509.jaxb.NameConstraints;
import org.xipki.ca.certprofile.internal.x509.jaxb.NameValueType;
import org.xipki.ca.certprofile.internal.x509.jaxb.ObjectFactory;
import org.xipki.ca.certprofile.internal.x509.jaxb.OidWithDescType;
import org.xipki.ca.certprofile.internal.x509.jaxb.PolicyConstraints;
import org.xipki.ca.certprofile.internal.x509.jaxb.PolicyIdMappingType;
import org.xipki.ca.certprofile.internal.x509.jaxb.PolicyMappings;
import org.xipki.ca.certprofile.internal.x509.jaxb.RSAParameters;
import org.xipki.ca.certprofile.internal.x509.jaxb.RangeType;
import org.xipki.ca.certprofile.internal.x509.jaxb.RangesType;
import org.xipki.ca.certprofile.internal.x509.jaxb.RdnType;
import org.xipki.ca.certprofile.internal.x509.jaxb.SubjectAltName;
import org.xipki.ca.certprofile.internal.x509.jaxb.SubjectInfoAccess;
import org.xipki.ca.certprofile.internal.x509.jaxb.UsageType;
import org.xipki.ca.certprofile.internal.x509.jaxb.X509ProfileType;
import org.xipki.ca.certprofile.internal.x509.jaxb.X509ProfileType.KeyAlgorithms;
import org.xipki.ca.certprofile.internal.x509.jaxb.X509ProfileType.Parameters;
import org.xipki.ca.certprofile.internal.x509.jaxb.X509ProfileType.Subject;
import org.xipki.common.ObjectIdentifiers;
import org.xipki.common.SecurityUtil;
import org.xipki.common.StringUtil;
import org.xipki.common.XMLUtil;
import org.xml.sax.SAXException;

/**
 * @author Lijun Liao
 */

public class ProfileConfCreatorDemo
{
    private static final ASN1ObjectIdentifier id_gematik = new ASN1ObjectIdentifier("1.2.276.0.76.4");

    private static final String REGEX_FQDN =
            "(?=^.{1,254}$)(^(?:(?!\\d+\\.|-)[a-zA-Z0-9_\\-]{1,63}(?<!-)\\.?)+(?:[a-zA-Z]{2,})$)";
    private static final String REGEX_SN = "[\\d]{1,}";

    private static final Set<ASN1ObjectIdentifier> requestExtensions;

    static
    {
        requestExtensions = new HashSet<>();
        requestExtensions.add(Extension.keyUsage);
        requestExtensions.add(Extension.extendedKeyUsage);
        requestExtensions.add(Extension.subjectAlternativeName);
        requestExtensions.add(Extension.subjectInfoAccess);
    }

    private static class ExampleDescription extends AnyType
    {
        public ExampleDescription(Element appInfo)
        {
            setAny(appInfo);
        }
    }

    public static void main(String[] args)
    {
        try
        {
            Marshaller m = JAXBContext.newInstance(ObjectFactory.class).createMarshaller();
            final SchemaFactory schemaFact = SchemaFactory.newInstance(
                    javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL url = XmlX509Certprofile.class.getResource("/xsd/certprofile.xsd");
            m.setSchema(schemaFact.newSchema(url));
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.setProperty("com.sun.xml.internal.bind.indentString", "  ");

            // RootCA
            X509ProfileType profile = Certprofile_RootCA(false);
            marshall(m, profile, "Certprofile_RootCA.xml");

            // RootCA-Cross
            profile = Certprofile_RootCA(true);
            marshall(m, profile, "Certprofile_RootCA_Cross.xml");

            // SubCA
            profile = Certprofile_SubCA();
            marshall(m, profile, "Certprofile_SubCA.xml");

            profile = Certprofile_SubCA_Complex();
            marshall(m, profile, "Certprofile_SubCA_Complex.xml");

            // OCSP
            profile = Certprofile_OCSP();
            marshall(m, profile, "Certprofile_OCSP.xml");

            // TLS
            profile = Certprofile_TLS();
            marshall(m, profile, "Certprofile_TLS.xml");

            // TLS_C
            profile = Certprofile_TLS_C();
            marshall(m, profile, "Certprofile_TLS_C.xml");

            // TLSwithIncSN
            profile = Certprofile_TLSwithIncSN();
            marshall(m, profile, "Certprofile_TLSwithIncSN.xml");

            //gSMC-K
            profile = Certprofile_gSMC_K();
            marshall(m, profile, "Certprofile_gSMC_K.xml");

            //multiple-OUs
            profile = Certprofile_MultipleOUs();
            marshall(m, profile, "Certprofile_multipleOUs.xml");

        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void marshall(Marshaller m, X509ProfileType profile, String filename)
    throws Exception
    {
        File file = new File("tmp", filename);
        file.getParentFile().mkdirs();
        JAXBElement<X509ProfileType> root = new ObjectFactory().createX509Profile(profile);
        FileOutputStream out = new FileOutputStream(file);
        try
        {
            m.marshal(root, out);
        } catch(JAXBException e)
        {
            throw XMLUtil.convert(e);
        } finally
        {
            out.close();
        }

    }

    private static X509ProfileType Certprofile_RootCA(boolean cross)
    throws Exception
    {
        X509ProfileType profile = getBaseProfile("Certprofile RootCA" + (cross ? " Cross" : ""),
                true, "10y", false);

        // Subject
        Subject subject = profile.getSubject();
        subject.setIncSerialNumber(false);

        List<RdnType> rdnControls = subject.getRdn();
        rdnControls.add(createRDN(ObjectIdentifiers.DN_C, 1, 1, new String[]{"DE|FR"}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_O, 1, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_OU, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_SN, 0, 1, new String[]{REGEX_SN}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_CN, 1, 1));

        // Extensions
        ExtensionsType extensions = profile.getExtensions();

        List<ExtensionType> list = extensions.getExtension();
        list.add(createExtension(Extension.subjectKeyIdentifier, true, false, null));
        list.add(createExtension(Extension.authorityInfoAccess, true, false, null));
        list.add(createExtension(Extension.cRLDistributionPoints, false, false, null));
        list.add(createExtension(Extension.freshestCRL, false, false, null));

        // Extensions - basicConstraints
        ExtensionValueType extensionValue = null;
        list.add(createExtension(Extension.basicConstraints, true, true, extensionValue));

        // Extensions - AuthorityKeyIdentifier
        if(cross)
        {
            extensionValue = createAuthorityKeyIdentifier(false);
            list.add(createExtension(Extension.authorityKeyIdentifier, true, false, extensionValue));
        }

        // Extensions - keyUsage
        extensionValue = createKeyUsages(
                new KeyUsageEnum[]{KeyUsageEnum.KEY_CERT_SIGN},
                new KeyUsageEnum[]{KeyUsageEnum.C_RL_SIGN});
        list.add(createExtension(Extension.keyUsage, true, true, extensionValue));

        return profile;
    }

    private static X509ProfileType Certprofile_SubCA()
    throws Exception
    {
        X509ProfileType profile = getBaseProfile("Certprofile SubCA", true, "8y", false);

        // Subject
        Subject subject = profile.getSubject();
        subject.setIncSerialNumber(false);

        List<RdnType> rdnControls = subject.getRdn();
        rdnControls.add(createRDN(ObjectIdentifiers.DN_C, 1, 1, new String[]{"DE|FR"}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_O, 1, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_OU, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_SN, 0, 1, new String[]{REGEX_SN}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_CN, 1, 1));

        // Extensions
        ExtensionsType extensions = profile.getExtensions();

        // Extensions - controls
        List<ExtensionType> list = extensions.getExtension();
        list.add(createExtension(Extension.subjectKeyIdentifier, true, false, null));
        list.add(createExtension(Extension.authorityInfoAccess, true, false, null));
        list.add(createExtension(Extension.cRLDistributionPoints, false, false, null));
        list.add(createExtension(Extension.freshestCRL, false, false, null));

        // Extensions - basicConstraints
        ExtensionValueType extensionValue = createBasicConstraints(1);
        list.add(createExtension(Extension.basicConstraints, true, true, extensionValue));

        // Extensions - AuthorityKeyIdentifier
        extensionValue = createAuthorityKeyIdentifier(false);
        list.add(createExtension(Extension.authorityKeyIdentifier, true, false, extensionValue));

        // Extensions - keyUsage
        extensionValue = createKeyUsages(
                new KeyUsageEnum[]{KeyUsageEnum.KEY_CERT_SIGN},
                new KeyUsageEnum[]{KeyUsageEnum.C_RL_SIGN});
        list.add(createExtension(Extension.keyUsage, true, true, extensionValue));

        return profile;
    }

    private static X509ProfileType Certprofile_SubCA_Complex()
    throws Exception
    {
        X509ProfileType profile = getBaseProfile("Certprofile SubCA with most extensions", true, "8y", false);

        // Subject
        Subject subject = profile.getSubject();
        subject.setIncSerialNumber(false);

        List<RdnType> rdnControls = subject.getRdn();
        rdnControls.add(createRDN(ObjectIdentifiers.DN_C, 1, 1, new String[]{"DE|FR"}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_O, 1, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_OU, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_SN, 0, 1, new String[]{REGEX_SN}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_CN, 1, 1, null, "PREFIX ", " SUFFIX"));

        // Extensions
        ExtensionsType extensions = profile.getExtensions();

        List<ExtensionType> list = extensions.getExtension();
        list.add(createExtension(Extension.subjectKeyIdentifier, true, false, null));
        list.add(createExtension(Extension.authorityInfoAccess, true, false, null));
        list.add(createExtension(Extension.cRLDistributionPoints, false, false, null));
        list.add(createExtension(Extension.freshestCRL, false, false, null));

        // Extensions - basicConstraints
        ExtensionValueType extensionValue = createBasicConstraints(1);
        list.add(createExtension(Extension.basicConstraints, true, true, extensionValue));

        // Extensions - AuthorityKeyIdentifier
        extensionValue = createAuthorityKeyIdentifier(false);
        list.add(createExtension(Extension.authorityKeyIdentifier, true, false, extensionValue));

        // Extensions - keyUsage
        extensionValue = createKeyUsages(
                new KeyUsageEnum[]{KeyUsageEnum.KEY_CERT_SIGN},
                new KeyUsageEnum[]{KeyUsageEnum.C_RL_SIGN});
        list.add(createExtension(Extension.keyUsage, true, true, extensionValue));

        // Certificate Policies
        extensionValue = createCertificatePolicies(
                new ASN1ObjectIdentifier("1.2.3.4.5"), new ASN1ObjectIdentifier("2.4.3.2.1"));
        list.add(createExtension(Extension.certificatePolicies, true, false, extensionValue));

        // Policy Mappings
        PolicyMappings policyMappings = new PolicyMappings();
        policyMappings.getMapping().add(createPolicyIdMapping(
                new ASN1ObjectIdentifier("1.1.1.1.1"),
                new ASN1ObjectIdentifier("2.1.1.1.1")));
        policyMappings.getMapping().add(createPolicyIdMapping(
                new ASN1ObjectIdentifier("1.1.1.1.2"),
                new ASN1ObjectIdentifier("2.1.1.1.2")));
        extensionValue = createExtensionValueType(policyMappings);
        list.add(createExtension(Extension.policyMappings, true, true, extensionValue));

        // Policy Constraints
        PolicyConstraints policyConstraints = createPolicyConstraints(2, 2);
        extensionValue = createExtensionValueType(policyConstraints);
        list.add(createExtension(Extension.policyConstraints, true, true, extensionValue));

        // Name Constrains
        NameConstraints nameConstraints = createNameConstraints();
        extensionValue = createExtensionValueType(nameConstraints);
        list.add(createExtension(Extension.nameConstraints, true, true, extensionValue));

        // Inhibit anyPolicy
        InhibitAnyPolicy inhibitAnyPolicy = createInhibitAnyPolicy(1);
        extensionValue = createExtensionValueType(inhibitAnyPolicy);
        list.add(createExtension(Extension.inhibitAnyPolicy, true, true, extensionValue));

        // SubjectAltName
        SubjectAltName subjectAltNameMode = new SubjectAltName();

        OtherName otherName = new OtherName();
        otherName.getType().add(createOidType(ObjectIdentifiers.DN_O));
        subjectAltNameMode.setOtherName(otherName);
        subjectAltNameMode.setRfc822Name("");
        subjectAltNameMode.setDNSName("");
        subjectAltNameMode.setDirectoryName("");
        subjectAltNameMode.setEdiPartyName("");
        subjectAltNameMode.setUniformResourceIdentifier("");
        subjectAltNameMode.setIPAddress("");
        subjectAltNameMode.setRegisteredID("");

        extensionValue = createExtensionValueType(subjectAltNameMode);
        list.add(createExtension(Extension.subjectAlternativeName, true, false, extensionValue));

        // SubjectInfoAccess
        SubjectInfoAccess subjectInfoAccessMode = new SubjectInfoAccess();

        SubjectInfoAccess.Access access = new SubjectInfoAccess.Access();
        access.setAccessMethod(createOidType(ObjectIdentifiers.id_ad_caRepository));

        GeneralNameType accessLocation = new GeneralNameType();
        access.setAccessLocation(accessLocation);
        accessLocation.setDirectoryName("");
        accessLocation.setUniformResourceIdentifier("");

        subjectInfoAccessMode.getAccess().add(access);

        extensionValue = createExtensionValueType(subjectInfoAccessMode);
        list.add(createExtension(Extension.subjectInfoAccess, true, false, extensionValue));

        // Custom Extension
        ASN1ObjectIdentifier customExtensionOid = new ASN1ObjectIdentifier("1.2.3.4");
        extensionValue = createConstantExtValue(DERNull.INSTANCE.getEncoded());
        list.add(createExtension(customExtensionOid, true, false, extensionValue, "custom extension 1"));

        return profile;
    }

    private static X509ProfileType Certprofile_OCSP()
    throws Exception
    {
        X509ProfileType profile = getBaseProfile("Certprofile OCSP", false, "5y", false);

        // Subject
        Subject subject = profile.getSubject();
        subject.setIncSerialNumber(false);

        List<RdnType> rdnControls = subject.getRdn();
        rdnControls.add(createRDN(ObjectIdentifiers.DN_C, 1, 1, new String[]{"DE|FR"}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_O, 1, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_OU, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_SN, 0, 1, new String[]{REGEX_SN}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_CN, 1, 1));

        // Extensions
        ExtensionsType extensions = profile.getExtensions();
        List<ExtensionType> list = extensions.getExtension();

        list.add(createExtension(Extension.subjectKeyIdentifier, true, false, null));
        list.add(createExtension(Extension.authorityInfoAccess, false, false, null));
        list.add(createExtension(Extension.cRLDistributionPoints, false, false, null));
        list.add(createExtension(Extension.freshestCRL, false, false, null));
        list.add(createExtension(ObjectIdentifiers.id_extension_pkix_ocsp_nocheck, false, false, null));

        // Extensions - basicConstraints
        ExtensionValueType extensionValue = null;
        list.add(createExtension(Extension.basicConstraints, true, true, extensionValue));

        // Extensions - AuthorityKeyIdentifier
        extensionValue = createAuthorityKeyIdentifier(true);
        list.add(createExtension(Extension.authorityKeyIdentifier, true, false, extensionValue));

        // Extensions - keyUsage
        extensionValue = createKeyUsages(
                new KeyUsageEnum[]{KeyUsageEnum.CONTENT_COMMITMENT},
                null);
        list.add(createExtension(Extension.keyUsage, true, true, extensionValue));

        // Extensions - extenedKeyUsage
        extensionValue = createExtendedKeyUsage(new ASN1ObjectIdentifier[]{ObjectIdentifiers.id_kp_OCSPSigning}, null);
        list.add(createExtension(Extension.extendedKeyUsage, true, false, extensionValue));

        return profile;
    }

    private static X509ProfileType Certprofile_TLS()
    throws Exception
    {
        X509ProfileType profile = getBaseProfile("Certprofile TLS", false, "5y", true);

        // Subject
        Subject subject = profile.getSubject();
        subject.setIncSerialNumber(false);

        List<RdnType> rdnControls = subject.getRdn();
        rdnControls.add(createRDN(ObjectIdentifiers.DN_C, 1, 1, new String[]{"DE|FR"}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_O, 1, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_OU, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_SN, 0, 1, new String[]{REGEX_SN}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_CN, 1, 1, new String[]{REGEX_FQDN}, null, null));

        // Extensions
        // Extensions - general
        ExtensionsType extensions = profile.getExtensions();

        // Extensions - controls
        List<ExtensionType> list = extensions.getExtension();
        list.add(createExtension(Extension.subjectKeyIdentifier, true, false, null));
        list.add(createExtension(Extension.authorityInfoAccess, true, false, null));
        list.add(createExtension(Extension.cRLDistributionPoints, false, false, null));
        list.add(createExtension(Extension.freshestCRL, false, false, null));

        // Extensions - basicConstraints
        ExtensionValueType extensionValue = null;
        list.add(createExtension(Extension.basicConstraints, true, true, extensionValue));

        // Extensions - AuthorityKeyIdentifier
        extensionValue = createAuthorityKeyIdentifier(true);
        list.add(createExtension(Extension.authorityKeyIdentifier, true, false, extensionValue));

        // Extensions - keyUsage
        extensionValue = createKeyUsages(
                new KeyUsageEnum[]{KeyUsageEnum.DIGITAL_SIGNATURE, KeyUsageEnum.DATA_ENCIPHERMENT,
                        KeyUsageEnum.KEY_ENCIPHERMENT},
                null);
        list.add(createExtension(Extension.keyUsage, true, true, extensionValue));

        // Extensions - extenedKeyUsage
        extensionValue = createExtendedKeyUsage(new ASN1ObjectIdentifier[]{ObjectIdentifiers.id_kp_serverAuth},
                new ASN1ObjectIdentifier[]{ObjectIdentifiers.id_kp_clientAuth});
        list.add(createExtension(Extension.extendedKeyUsage, true, false, extensionValue));

        // Admission - just DEMO, does not belong to TLS certificate
        extensionValue = createAdmission(new ASN1ObjectIdentifier("1.1.1.2"), "demo item");
        list.add(createExtension(ObjectIdentifiers.id_extension_admission, true, false, extensionValue));

        return profile;
    }

    private static X509ProfileType Certprofile_TLS_C()
    throws Exception
    {
        X509ProfileType profile = getBaseProfile("Certprofile TLS_C", false, "5y", false);

        // Subject
        Subject subject = profile.getSubject();
        subject.setIncSerialNumber(false);

        List<RdnType> rdnControls = subject.getRdn();
        rdnControls.add(createRDN(ObjectIdentifiers.DN_C, 1, 1, new String[]{"DE|FR"}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_O, 1, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_OU, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_SN, 0, 1, new String[]{REGEX_SN}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_CN, 1, 1));

        // Extensions
        ExtensionsType extensions = profile.getExtensions();
        List<ExtensionType> list = extensions.getExtension();

        list.add(createExtension(Extension.subjectKeyIdentifier, true, false, null));
        list.add(createExtension(Extension.authorityInfoAccess, true, false, null));
        list.add(createExtension(Extension.cRLDistributionPoints, false, false, null));
        list.add(createExtension(Extension.freshestCRL, false, false, null));

        // Extensions - basicConstraints
        ExtensionValueType extensionValue = null;
        list.add(createExtension(Extension.basicConstraints, true, true, extensionValue));

        // Extensions - AuthorityKeyIdentifier
        extensionValue = createAuthorityKeyIdentifier(true);
        list.add(createExtension(Extension.authorityKeyIdentifier, true, false, extensionValue));

        // Extensions - keyUsage
        extensionValue = createKeyUsages(
                new KeyUsageEnum[]{KeyUsageEnum.DIGITAL_SIGNATURE, KeyUsageEnum.DATA_ENCIPHERMENT,
                        KeyUsageEnum.KEY_ENCIPHERMENT},
                null);
        list.add(createExtension(Extension.keyUsage, true, true, extensionValue));

        // Extensions - extenedKeyUsage
        extensionValue = createExtendedKeyUsage(new ASN1ObjectIdentifier[]{ObjectIdentifiers.id_kp_clientAuth}, null);
        list.add(createExtension(Extension.extendedKeyUsage, true, false, extensionValue));

        return profile;
    }

    private static X509ProfileType Certprofile_TLSwithIncSN()
    throws Exception
    {
        X509ProfileType profile = getBaseProfile("Certprofile TLSwithIncSN", false, "5y", false);

        // Subject
        Subject subject = profile.getSubject();
        subject.setIncSerialNumber(true);

        List<RdnType> rdnControls = subject.getRdn();
        rdnControls.add(createRDN(ObjectIdentifiers.DN_C, 1, 1, new String[]{"DE|FR"}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_O, 1, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_OU, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_SN, 0, 1, new String[]{REGEX_SN}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_CN, 1, 1, new String[]{REGEX_FQDN}, null, null));

        // Extensions
        // Extensions - general
        ExtensionsType extensions = profile.getExtensions();

        // Extensions - controls
        List<ExtensionType> list = extensions.getExtension();
        list.add(createExtension(Extension.subjectKeyIdentifier, true, false, null));
        list.add(createExtension(Extension.authorityInfoAccess, true, false, null));
        list.add(createExtension(Extension.cRLDistributionPoints, false, false, null));
        list.add(createExtension(Extension.freshestCRL, false, false, null));

        // Extensions - basicConstraints
        ExtensionValueType extensionValue = null;
        list.add(createExtension(Extension.basicConstraints, true, true, extensionValue));

        // Extensions - AuthorityKeyIdentifier
        extensionValue = createAuthorityKeyIdentifier(true);
        list.add(createExtension(Extension.authorityKeyIdentifier, true, false, extensionValue));

        // Extensions - keyUsage
        extensionValue = createKeyUsages(
                new KeyUsageEnum[]{KeyUsageEnum.DIGITAL_SIGNATURE, KeyUsageEnum.DATA_ENCIPHERMENT,
                        KeyUsageEnum.KEY_ENCIPHERMENT},
                null);
        list.add(createExtension(Extension.keyUsage, true, true, extensionValue));

        // Extensions - extenedKeyUsage
        extensionValue = createExtendedKeyUsage(new ASN1ObjectIdentifier[]{ObjectIdentifiers.id_kp_serverAuth},
                new ASN1ObjectIdentifier[]{ObjectIdentifiers.id_kp_clientAuth});
        list.add(createExtension(Extension.extendedKeyUsage, true, false, extensionValue));

        return profile;
    }

    private static X509ProfileType Certprofile_gSMC_K()
    throws Exception
    {
        X509ProfileType profile = getBaseProfile("Certprofile gSMC_K", false, "5y", false);
        profile.setDuplicateSubject(true);

        // SpecialBehavior
        profile.setSpecialBehavior(SpecialX509CertprofileBehavior.gematik_gSMC_K.name());

        // Maximal liftime
        Parameters profileParams = new Parameters();
        profile.setParameters(profileParams);
        NameValueType nv = new NameValueType();
        nv.setName(SpecialX509CertprofileBehavior.PARAMETER_MAXLIFTIME);
        nv.setValue(Integer.toString(20 * 365));
        profileParams.getParameter().add(nv);

        // Subject
        Subject subject = profile.getSubject();
        subject.setIncSerialNumber(false);

        List<RdnType> rdnControls = subject.getRdn();
        rdnControls.add(createRDN(ObjectIdentifiers.DN_C, 1, 1, new String[]{"DE"}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_O, 1, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_OU, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_ST, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_L, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_POSTAL_CODE, 0, 1));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_STREET, 0, 1));
        // regex: ICCSN-yyyyMMdd
        String regex = "80276[\\d]{15,15}-20\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])";
        rdnControls.add(createRDN(ObjectIdentifiers.DN_CN, 1, 1, new String[]{regex}, null, null));

        // Extensions
        ExtensionsType extensions = profile.getExtensions();
        List<ExtensionType> list = extensions.getExtension();

        list.add(createExtension(Extension.subjectKeyIdentifier, true, false, null));
        list.add(createExtension(Extension.authorityInfoAccess, true, false, null));
        list.add(createExtension(Extension.cRLDistributionPoints, false, false, null));

        // Extensions - basicConstraints
        ExtensionValueType extensionValue = null;
        list.add(createExtension(Extension.basicConstraints, true, true, extensionValue));

        // Extensions - AuthorityKeyIdentifier
        extensionValue = createAuthorityKeyIdentifier(true);
        list.add(createExtension(Extension.authorityKeyIdentifier, true, false, extensionValue));

        // Extensions - keyUsage
        extensionValue = createKeyUsages(
                new KeyUsageEnum[]{KeyUsageEnum.DIGITAL_SIGNATURE, KeyUsageEnum.KEY_ENCIPHERMENT},
                null);
        list.add(createExtension(Extension.keyUsage, true, true, extensionValue));

        // Extensions - extenedKeyUsage
        extensionValue = createExtendedKeyUsage(new ASN1ObjectIdentifier[]{ObjectIdentifiers.id_kp_serverAuth},
                new ASN1ObjectIdentifier[]{ObjectIdentifiers.id_kp_clientAuth});
        list.add(createExtension(Extension.extendedKeyUsage, true, false, extensionValue));

        // Extensions - Policy
        CertificatePolicies policies = new CertificatePolicies();
        ASN1ObjectIdentifier[] policyIds = new ASN1ObjectIdentifier[]
        {
                id_gematik.branch("79"), id_gematik.branch("163")
        };
        for(ASN1ObjectIdentifier id : policyIds)
        {
            CertificatePolicyInformationType policyInfo = new CertificatePolicyInformationType();
            policies.getCertificatePolicyInformation().add(policyInfo);
            policyInfo.setPolicyIdentifier(createOidType(id));
        }
        extensionValue = createExtensionValueType(policies);
        list.add(createExtension(Extension.certificatePolicies, true, false, extensionValue));

        // Extension - Adminssion
        Admission admission = new Admission();
        admission.getProfessionOid().add(createOidType(id_gematik.branch("103")));
        admission.getProfessionItem().add("Anwendungskonnektor");
        extensionValue = createExtensionValueType(admission);
        list.add(createExtension(ObjectIdentifiers.id_extension_admission, true, false, extensionValue));

        // SubjectAltNames
        extensionValue = null;
        list.add(createExtension(Extension.subjectAlternativeName, false, false, extensionValue));

        return profile;
    }

    private static X509ProfileType Certprofile_MultipleOUs()
    throws Exception
    {
        X509ProfileType profile = getBaseProfile("Certprofile Multiple OUs DEMO", false, "5y", false);

        // Subject
        Subject subject = profile.getSubject();
        subject.setIncSerialNumber(false);

        List<RdnType> rdnControls = subject.getRdn();
        rdnControls.add(createRDN(ObjectIdentifiers.DN_C, 1, 1, new String[]{"DE|FR"}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_O, 1, 1));

        final String regex_ou1 = "[A-Z]{1,1}[\\d]{5,5}";
        final String regex_ou2 = "[\\d]{5,5}";
        rdnControls.add(createRDN(ObjectIdentifiers.DN_OU, 2, 2, new String[]{regex_ou1,regex_ou2}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_SN, 0, 1, new String[]{REGEX_SN}, null, null));
        rdnControls.add(createRDN(ObjectIdentifiers.DN_CN, 1, 1));

        // Extensions
        // Extensions - general
        ExtensionsType extensions = profile.getExtensions();
        List<ExtensionType> list = extensions.getExtension();

        list.add(createExtension(Extension.subjectKeyIdentifier, true, false, null));
        list.add(createExtension(Extension.authorityInfoAccess, false, false, null));
        list.add(createExtension(Extension.cRLDistributionPoints, false, false, null));
        list.add(createExtension(Extension.freshestCRL, false, false, null));

        // Extensions - basicConstraints
        ExtensionValueType extensionValue = null;
        list.add(createExtension(Extension.basicConstraints, true, true, extensionValue));

        // Extensions - AuthorityKeyIdentifier
        extensionValue = createAuthorityKeyIdentifier(true);
        list.add(createExtension(Extension.authorityKeyIdentifier, true, false, extensionValue));

        // Extensions - keyUsage
        extensionValue = createKeyUsages(
                new KeyUsageEnum[]{KeyUsageEnum.CONTENT_COMMITMENT},
                null);
        list.add(createExtension(Extension.keyUsage, true, true, extensionValue));

        return profile;
    }

    private static RdnType createRDN(ASN1ObjectIdentifier type, int min, int max)
    {
        return createRDN(type, min, max, null, null, null);
    }

    private static RdnType createRDN(ASN1ObjectIdentifier type, int min, int max, String[] regexArrays,
            String prefix, String suffix)
    {
        RdnType ret = new RdnType();
        ret.setType(createOidType(type));
        ret.setMinOccurs(min);
        ret.setMaxOccurs(max);

        if(regexArrays != null)
        {
            if(regexArrays.length != max)
            {
                throw new IllegalArgumentException("regexArrays.length " + regexArrays.length + " != max " + max);
            }
            for(String regex : regexArrays)
            {
                ret.getRegex().add(regex);
            }
        }

        if(StringUtil.isNotBlank(prefix))
        {
            ret.setPrefix(prefix);
        }

        if(StringUtil.isNotBlank(suffix))
        {
            ret.setSuffix(suffix);
        }

        return ret;
    }

    private static ExtensionType createExtension(ASN1ObjectIdentifier type,
            boolean required, boolean critical,
            ExtensionValueType extValue)
    {
        return createExtension(type, required, critical, extValue, null);
    }

    private static ExtensionType createExtension(ASN1ObjectIdentifier type, boolean required, boolean critical,
            ExtensionValueType extValue, String description)
    {
        ExtensionType ret = new ExtensionType();
        // abbributes
        ret.setRequired(required);
        ret.setPermittedInRequest(requestExtensions.contains(type));
        // children
        ret.setType(createOidType(type, description));
        ret.setCritical(critical);
        ret.setValue(extValue);
        return ret;
    }

    private static ExtensionValueType createAuthorityKeyIdentifier(boolean includeSerialAndSerial)
    {

        AuthorityKeyIdentifier akiType = new AuthorityKeyIdentifier();
        akiType.setIncludeIssuerAndSerial(includeSerialAndSerial);
        return createExtensionValueType(akiType);

    }
    private static ExtensionValueType createBasicConstraints(int pathLen)
    {
        BasicConstraints extValue = new BasicConstraints();
        extValue.setPathLen(pathLen);
        return createExtensionValueType(extValue);
    }

    private static ExtensionValueType createKeyUsages(
            KeyUsageEnum[] requiredUsages, KeyUsageEnum[] optionalUsages)
    {
        KeyUsage extValue = new KeyUsage();
        if(requiredUsages != null)
        {
            for(KeyUsageEnum m : requiredUsages)
            {
                UsageType usage = new UsageType();
                usage.setValue(m);
                usage.setRequired(true);
                extValue.getUsage().add(usage);
            }
        }
        if(optionalUsages != null)
        {
            for(KeyUsageEnum m : optionalUsages)
            {
                UsageType usage = new UsageType();
                usage.setValue(m);
                usage.setRequired(false);
                extValue.getUsage().add(usage);
            }
        }

        return createExtensionValueType(extValue);
    }

    private static ExtensionValueType createExtendedKeyUsage(
            ASN1ObjectIdentifier[] requiredUsages, ASN1ObjectIdentifier[] optionalUsages)
    {
        ExtendedKeyUsage extValue = new ExtendedKeyUsage();
        if(requiredUsages != null)
        {
            for(ASN1ObjectIdentifier usage : requiredUsages)
            {
                extValue.getUsage().add(createSingleExtKeyUsage(usage, true));
            }
        }

        if(optionalUsages != null)
        {
            for(ASN1ObjectIdentifier usage : optionalUsages)
            {
                extValue.getUsage().add(createSingleExtKeyUsage(usage, false));
            }
        }

        return createExtensionValueType(extValue);
    }

    private static Usage createSingleExtKeyUsage(ASN1ObjectIdentifier usage, boolean required)
    {
        Usage type = new Usage();
        type.setValue(usage.getId());
        type.setRequired(required);
        String desc = getDescription(usage);
        if(desc != null)
        {
            type.setDescription(desc);
        }
        return type;
    }

    private static ExtensionValueType createAdmission(ASN1ObjectIdentifier oid, String item)
    {
        Admission extValue = new Admission();
        extValue.getProfessionItem().add(item);
        extValue.getProfessionOid().add(createOidType(oid));
        return createExtensionValueType(extValue);
    }

    private static ExtensionValueType createCertificatePolicies(
            ASN1ObjectIdentifier... policyOids)
    {
        if(policyOids == null || policyOids.length == 0)
        {
            return null;
        }

        CertificatePolicies extValue = new CertificatePolicies();
        List<CertificatePolicyInformationType> l = extValue.getCertificatePolicyInformation();
        for(ASN1ObjectIdentifier oid : policyOids)
        {
            CertificatePolicyInformationType single = new CertificatePolicyInformationType();
            l.add(single);
            single.setPolicyIdentifier(createOidType(oid));
        }

        return createExtensionValueType(extValue);
    }

    private static String getDescription(ASN1ObjectIdentifier oid)
    {
        return ObjectIdentifiers.getName(oid);
    }

    private static PolicyIdMappingType createPolicyIdMapping(
        ASN1ObjectIdentifier issuerPolicyId,
        ASN1ObjectIdentifier subjectPolicyId)
    {
        PolicyIdMappingType ret = new PolicyIdMappingType();
        ret.setIssuerDomainPolicy(createOidType(issuerPolicyId));
        ret.setSubjectDomainPolicy(createOidType(subjectPolicyId));

        return ret;
    }

    private static PolicyConstraints createPolicyConstraints(Integer inhibitPolicyMapping,
            Integer requireExplicitPolicy)
    {
        PolicyConstraints ret = new PolicyConstraints();
        if(inhibitPolicyMapping != null)
        {
            ret.setInhibitPolicyMapping(inhibitPolicyMapping);
        }

        if(requireExplicitPolicy != null)
        {
            ret.setRequireExplicitPolicy(requireExplicitPolicy);
        }
        return ret;
    }

    private static NameConstraints createNameConstraints()
    {
        NameConstraints ret = new NameConstraints();
        GeneralSubtreesType permitted = new GeneralSubtreesType();
        GeneralSubtreeBaseType single = new GeneralSubtreeBaseType();
        single.setDirectoryName("O=example organization, C=DE");
        permitted.getBase().add(single);
        ret.setPermittedSubtrees(permitted);

        GeneralSubtreesType excluded = new GeneralSubtreesType();
        single = new GeneralSubtreeBaseType();
        single.setDirectoryName("OU=bad OU, O=example organization, C=DE");
        excluded.getBase().add(single);
        ret.setExcludedSubtrees(excluded);

        return ret;
    }

    private static InhibitAnyPolicy createInhibitAnyPolicy(int skipCerts)
    {
        InhibitAnyPolicy ret = new InhibitAnyPolicy();
        ret.setSkipCerts(skipCerts);
        return ret;
    }

    private static OidWithDescType createOidType(ASN1ObjectIdentifier oid)
    {
        return createOidType(oid, null);
    }

    private static OidWithDescType createOidType(ASN1ObjectIdentifier oid, String description)
    {
        OidWithDescType ret = new OidWithDescType();
        ret.setValue(oid.getId());
        if(description == null)
        {
            description = getDescription(oid);
        }
        if(description != null)
        {
            ret.setDescription(description);
        }
        return ret;
    }

    private static ExtensionValueType createConstantExtValue(byte[] bytes)
    {
        ConstantExtValue extValue = new ConstantExtValue();
        extValue.setValue(bytes);
        return createExtensionValueType(extValue);
    }

    private static X509ProfileType getBaseProfile(String description, boolean ca,
            String validity, boolean useMidnightNotBefore)
    {
        final boolean qa = false;
        return getBaseProfile(description, ca, qa, validity, useMidnightNotBefore);
    }

    private static X509ProfileType getBaseProfile(String description, boolean ca, boolean qa,
            String validity, boolean useMidnightNotBefore)
    {
        X509ProfileType profile = new X509ProfileType();

        profile.setAppInfo(createDescription(description));
        if(qa)
        {
            profile.setQaOnly(true);
        }
        profile.setCa(ca);
        profile.setVersion(3);
        profile.setValidity(validity);
        profile.setNotBeforeTime(useMidnightNotBefore ? "midnight" : "current");

        profile.setDuplicateKey(false);
        profile.setDuplicateSubject(false);
        profile.setSerialNumberInReq(false);

        // Subject
        Subject subject = new Subject();
        profile.setSubject(subject);

        subject.setDnBackwards(false);

        // Key
        profile.setKeyAlgorithms(createKeyAlgorithms());

        // Extensions
        ExtensionsType extensions = new ExtensionsType();
        profile.setExtensions(extensions);

        return profile;
    }

    private static KeyAlgorithms createKeyAlgorithms()
    {
        KeyAlgorithms ret = new KeyAlgorithms();
        List<AlgorithmType> list = ret.getAlgorithm();

        // RSA
        {
            AlgorithmType algorithm = new AlgorithmType();
            list.add(algorithm);

            algorithm.getAlgorithm().add(createOidType(PKCSObjectIdentifiers.rsaEncryption, "RSA"));

            RSAParameters params = new RSAParameters();
            algorithm.setParameters(createKeyParametersType(params));

            RangesType ranges = new RangesType();
            params.setModulusLength(ranges);
            List<RangeType> modulusLengths = ranges.getRange();
            modulusLengths.add(createRange(2048));
            modulusLengths.add(createRange(3072));
        }

        // DSA
        {
            AlgorithmType algorithm = new AlgorithmType();
            list.add(algorithm);

            algorithm.getAlgorithm().add(createOidType(X9ObjectIdentifiers.id_dsa, "DSA"));
            DSAParameters params = new DSAParameters();
            algorithm.setParameters(createKeyParametersType(params));

            RangesType ranges = new RangesType();
            params.setPLength(ranges);

            List<RangeType> pLengths = ranges.getRange();
            pLengths.add(createRange(1024));
            pLengths.add(createRange(2048));

            ranges = new RangesType();
            params.setQLength(ranges);
            List<RangeType> qLengths = ranges.getRange();
            qLengths.add(createRange(160));
            qLengths.add(createRange(224));
            qLengths.add(createRange(256));
        }

        // EC
        {
            AlgorithmType algorithm = new AlgorithmType();
            list.add(algorithm);

            algorithm.getAlgorithm().add(createOidType(X9ObjectIdentifiers.id_ecPublicKey, "EC"));
            ECParameters params = new ECParameters();
            algorithm.setParameters(createKeyParametersType(params));

            Curves curves = new Curves();
            params.setCurves(curves);

            ASN1ObjectIdentifier[] curveIds = new ASN1ObjectIdentifier[]
            {
                SECObjectIdentifiers.secp256r1, TeleTrusTObjectIdentifiers.brainpoolP256r1
            };

            for(ASN1ObjectIdentifier curveId : curveIds)
            {
                String name = SecurityUtil.getCurveName(curveId);
                curves.getCurve().add(createOidType(curveId, name));
            }

            params.setPointEncodings(new PointEncodings());
            final Byte unpressed = 4;
            params.getPointEncodings().getPointEncoding().add(unpressed);
        }

        return ret;
    }

    private static RangeType createRange(Integer size)
    {
        return createRange(size, size);
    }

    private static RangeType createRange(Integer min, Integer max)
    {
        RangeType range = new RangeType();
        if(min != null)
        {
            range.setMin(min);
        }
        if(max != null)
        {
            range.setMax(max);
        }
        return range;
    }

    private static AnyType createDescription(String details)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<my:myDescription xmlns:my=\"http://example.org\">\n");
        sb.append("      <my:category>cat A</my:category>\n");
        sb.append("      <my:details>").append(details).append("</my:details>\n");
        sb.append("    </my:myDescription>\n");
        Element element;
        try
        {
            element = XMLUtil.getDocumentElment(sb.toString().getBytes());
        } catch (IOException | SAXException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        return new ExampleDescription(element);
    }

    private static ExtensionValueType createExtensionValueType(Object object)
    {
        ExtensionValueType ret = new ExtensionValueType();
        ret.setAny(object);
        return ret;
    }

    private static KeyParametersType createKeyParametersType(Object object)
    {
        KeyParametersType ret = new KeyParametersType();
        ret.setAny(object);
        return ret;
    }

}