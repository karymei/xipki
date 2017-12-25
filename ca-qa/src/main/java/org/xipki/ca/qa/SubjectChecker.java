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

package org.xipki.ca.qa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERT61String;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.xipki.ca.api.BadCertTemplateException;
import org.xipki.ca.api.profile.CertprofileException;
import org.xipki.ca.api.profile.RdnControl;
import org.xipki.ca.api.profile.StringType;
import org.xipki.ca.api.profile.x509.SpecialX509CertprofileBehavior;
import org.xipki.ca.api.profile.x509.SubjectControl;
import org.xipki.ca.api.profile.x509.SubjectDnSpec;
import org.xipki.common.qa.ValidationIssue;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.ObjectIdentifiers;
import org.xipki.security.util.X509Util;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class SubjectChecker {

    private final SpecialX509CertprofileBehavior specialBehavior;

    private final SubjectControl subjectControl;

    public SubjectChecker(final SpecialX509CertprofileBehavior specialBehavior,
            final SubjectControl subjectControl) throws CertprofileException {
        this.specialBehavior = specialBehavior;
        this.subjectControl = ParamUtil.requireNonNull("subjectControl", subjectControl);
    }

    public List<ValidationIssue> checkSubject(final X500Name subject,
            final X500Name requestedSubject) {
        ParamUtil.requireNonNull("subject", subject);
        ParamUtil.requireNonNull("requestedSubject", requestedSubject);

        // collect subject attribute types to check
        Set<ASN1ObjectIdentifier> oids = new HashSet<>();

        for (ASN1ObjectIdentifier oid : subjectControl.types()) {
            oids.add(oid);
        }

        for (ASN1ObjectIdentifier oid : subject.getAttributeTypes()) {
            oids.add(oid);
        }

        List<ValidationIssue> result = new LinkedList<>();

        ValidationIssue issue = new ValidationIssue("X509.SUBJECT.group", "X509 subject RDN group");
        result.add(issue);
        if (CollectionUtil.isNonEmpty(subjectControl.groups())) {
            Set<String> groups = new HashSet<>(subjectControl.groups());
            for (String g : groups) {
                boolean toBreak = false;
                RDN rdn = null;
                for (ASN1ObjectIdentifier type : subjectControl.getTypesForGroup(g)) {
                    RDN[] rdns = subject.getRDNs(type);
                    if (rdns == null || rdns.length == 0) {
                        continue;
                    }

                    if (rdns.length > 1) {
                        issue.setFailureMessage("AttributeTypeAndValues of group " + g
                                + " is not in one RDN");
                        toBreak = true;
                        break;
                    }

                    if (rdn == null) {
                        rdn = rdns[0];
                    } else if (rdn != rdns[0]) {
                        issue.setFailureMessage("AttributeTypeAndValues of group " + g
                                + " is not in one RDN");
                        toBreak = true;
                        break;
                    }
                }

                if (toBreak) {
                    break;
                }
            }
        }

        for (ASN1ObjectIdentifier type : oids) {
            ValidationIssue valIssue;
            try {
                valIssue = checkSubjectAttribute(type, subject, requestedSubject);
            } catch (BadCertTemplateException ex) {
                valIssue = new ValidationIssue("X509.SUBJECT.REQUEST", "Subject in request");
                valIssue.setFailureMessage(ex.getMessage());
            }
            result.add(valIssue);
        }

        return result;
    } // method checkSubject

    private ValidationIssue checkSubjectAttribute(final ASN1ObjectIdentifier type,
            final X500Name subject, final X500Name requestedSubject)
            throws BadCertTemplateException {
        boolean multiValuedRdn = subjectControl.getGroup(type) != null;
        if (multiValuedRdn) {
            return checkSubjectAttributeMultiValued(type, subject, requestedSubject);
        } else {
            return checkSubjectAttributeNotMultiValued(type, subject, requestedSubject);
        }
    }

    private ValidationIssue checkSubjectAttributeNotMultiValued(final ASN1ObjectIdentifier type,
            final X500Name subject, final X500Name requestedSubject)
            throws BadCertTemplateException {
        ValidationIssue issue = createSubjectIssue(type);

        // control
        RdnControl rdnControl = subjectControl.getControl(type);
        int minOccurs = (rdnControl == null) ? 0 : rdnControl.minOccurs();
        int maxOccurs = (rdnControl == null) ? 0 : rdnControl.maxOccurs();

        RDN[] rdns = subject.getRDNs(type);
        int rdnsSize = (rdns == null) ? 0 : rdns.length;

        if (rdnsSize < minOccurs || rdnsSize > maxOccurs) {
            issue.setFailureMessage("number of RDNs '" + rdnsSize
                    + "' is not within [" + minOccurs + ", " + maxOccurs + "]");
            return issue;
        }

        RDN[] requestedRdns = requestedSubject.getRDNs(type);

        if (rdnsSize == 0) {
            // check optional attribute but is present in requestedSubject
            if (maxOccurs > 0 && requestedRdns != null && requestedRdns.length > 0) {
                issue.setFailureMessage("is absent but expected present");
            }
            return issue;
        }

        StringBuilder failureMsg = new StringBuilder();

        // check the encoding
        StringType stringType = null;
        if (rdnControl != null) {
            stringType = rdnControl.stringType();
        }

        List<String> requestedCoreAtvTextValues = new LinkedList<>();
        if (requestedRdns != null) {
            for (RDN requestedRdn : requestedRdns) {
                String textValue = getRdnTextValueOfRequest(requestedRdn);
                requestedCoreAtvTextValues.add(textValue);
            }

            if (rdnControl != null && rdnControl.patterns() != null) {
                // sort the requestedRDNs
                requestedCoreAtvTextValues = sort(requestedCoreAtvTextValues,
                        rdnControl.patterns());
            }
        }

        if (rdns == null) { // return always false, only to make the null checker happy
            return issue;
        }

        for (int i = 0; i < rdns.length; i++) {
            RDN rdn = rdns[i];
            AttributeTypeAndValue[] atvs = rdn.getTypesAndValues();
            if (atvs.length > 1) {
                failureMsg.append("size of RDN[" + i + "] is '" + atvs.length
                        + "' but expected '1'");
                failureMsg.append("; ");
                continue;
            }

            String atvTextValue = getAtvValueString("RDN[" + i + "]", atvs[0], stringType,
                    failureMsg);
            if (atvTextValue == null) {
                continue;
            }

            checkAttributeTypeAndValue("RDN[" + i + "]", type,
                    atvTextValue, rdnControl, requestedCoreAtvTextValues, i, failureMsg);
        }

        int len = failureMsg.length();
        if (len > 2) {
            failureMsg.delete(len - 2, len);
            issue.setFailureMessage(failureMsg.toString());
        }

        return issue;
    } // method checkSubjectAttributeNotMultiValued

    private ValidationIssue checkSubjectAttributeMultiValued(final ASN1ObjectIdentifier type,
            final X500Name subject, final X500Name requestedSubject)
            throws BadCertTemplateException {
        ValidationIssue issue = createSubjectIssue(type);

        RDN[] rdns = subject.getRDNs(type);
        int rdnsSize = (rdns == null) ? 0 : rdns.length;

        RDN[] requestedRdns = requestedSubject.getRDNs(type);

        if (rdnsSize != 1) {
            if (rdnsSize == 0) {
                // check optional attribute but is present in requestedSubject
                if (requestedRdns != null && requestedRdns.length > 0) {
                    issue.setFailureMessage("is absent but expected present");
                }
            } else {
                issue.setFailureMessage("number of RDNs '" + rdnsSize + "' is not 1");
            }
            return issue;
        }

        // control
        final RdnControl rdnControl = subjectControl.getControl(type);

        // check the encoding
        StringType stringType = null;
        if (rdnControl != null) {
            stringType = rdnControl.stringType();
        }
        List<String> requestedCoreAtvTextValues = new LinkedList<>();
        if (requestedRdns != null) {
            for (RDN requestedRdn : requestedRdns) {
                String textValue = getRdnTextValueOfRequest(requestedRdn);
                requestedCoreAtvTextValues.add(textValue);
            }

            if (rdnControl != null && rdnControl.patterns() != null) {
                // sort the requestedRDNs
                requestedCoreAtvTextValues = sort(requestedCoreAtvTextValues,
                        rdnControl.patterns());
            }
        }

        if (rdns == null) { // return always false, only to make the null checker happy
            return issue;
        }

        StringBuilder failureMsg = new StringBuilder();

        AttributeTypeAndValue[] li = rdns[0].getTypesAndValues();
        List<AttributeTypeAndValue> atvs = new LinkedList<>();
        for (AttributeTypeAndValue m : li) {
            if (type.equals(m.getType())) {
                atvs.add(m);
            }
        }

        final int atvsSize = atvs.size();

        int minOccurs = (rdnControl == null) ? 0 : rdnControl.minOccurs();
        int maxOccurs = (rdnControl == null) ? 0 : rdnControl.maxOccurs();

        if (atvsSize < minOccurs || atvsSize > maxOccurs) {
            issue.setFailureMessage("number of AttributeTypeAndValuess '" + atvsSize
                    + "' is not within [" + minOccurs + ", " + maxOccurs + "]");
            return issue;
        }

        for (int i = 0; i < atvsSize; i++) {
            AttributeTypeAndValue atv = atvs.get(i);
            String atvTextValue = getAtvValueString("AttributeTypeAndValue[" + i + "]", atv,
                    stringType, failureMsg);
            if (atvTextValue == null) {
                continue;
            }

            checkAttributeTypeAndValue("AttributeTypeAndValue[" + i + "]", type, atvTextValue,
                    rdnControl, requestedCoreAtvTextValues, i, failureMsg);
        }

        int len = failureMsg.length();
        if (len > 2) {
            failureMsg.delete(len - 2, len);
            issue.setFailureMessage(failureMsg.toString());
        }

        return issue;
    } // method checkSubjectAttributeMultiValued

    private void checkAttributeTypeAndValue(final String name, final ASN1ObjectIdentifier type,
            final String atvTextValue, final RdnControl rdnControl,
            final List<String> requestedCoreAtvTextValues, final int index,
            final StringBuilder failureMsg) throws BadCertTemplateException {
        String tmpAtvTextValue = atvTextValue;
        if (ObjectIdentifiers.DN_DATE_OF_BIRTH.equals(type)) {
            if (!SubjectDnSpec.PATTERN_DATE_OF_BIRTH.matcher(tmpAtvTextValue).matches()) {
                throw new BadCertTemplateException(
                        "Value of RDN dateOfBirth does not have format YYYMMDD000000Z");
            }
        } else if (rdnControl != null) {
            String prefix = rdnControl.prefix();
            if (prefix != null) {
                if (!tmpAtvTextValue.startsWith(prefix)) {
                    failureMsg.append(name).append(" '").append(tmpAtvTextValue)
                        .append("' does not start with prefix '").append(prefix).append("'; ");
                    return;
                } else {
                    tmpAtvTextValue = tmpAtvTextValue.substring(prefix.length());
                }
            }

            String suffix = rdnControl.suffix();
            if (suffix != null) {
                if (!tmpAtvTextValue.endsWith(suffix)) {
                    failureMsg.append(name).append(" '").append(tmpAtvTextValue)
                        .append("' does not end with suffix '").append(suffix).append("'; ");
                    return;
                } else {
                    tmpAtvTextValue = tmpAtvTextValue.substring(0,
                            tmpAtvTextValue.length() - suffix.length());
                }
            }

            List<Pattern> patterns = rdnControl.patterns();
            if (patterns != null) {
                Pattern pattern = patterns.get(index);
                boolean matches = pattern.matcher(tmpAtvTextValue).matches();
                if (!matches) {
                    failureMsg.append(name).append(" '").append(tmpAtvTextValue)
                        .append("' is not valid against regex '")
                        .append(pattern.pattern()).append("'; ");
                    return;
                }
            }
        }

        if (CollectionUtil.isEmpty(requestedCoreAtvTextValues)) {
            if (!type.equals(ObjectIdentifiers.DN_SERIALNUMBER)) {
                failureMsg.append("is present but not contained in the request");
                failureMsg.append("; ");
            }
        } else {
            String requestedCoreAtvTextValue = requestedCoreAtvTextValues.get(index);
            if (ObjectIdentifiers.DN_CN.equals(type) && specialBehavior != null
                    && SpecialX509CertprofileBehavior.gematik_gSMC_K.equals(specialBehavior)) {
                if (!tmpAtvTextValue.startsWith(requestedCoreAtvTextValue + "-")) {
                    failureMsg.append("content '").append(tmpAtvTextValue)
                        .append("' does not start with '")
                        .append(requestedCoreAtvTextValue).append("-'; ");
                }
            } else if (!type.equals(ObjectIdentifiers.DN_SERIALNUMBER)) {
                if (!tmpAtvTextValue.equals(requestedCoreAtvTextValue)) {
                    failureMsg.append("content '").append(tmpAtvTextValue)
                        .append("' but expected '").append(requestedCoreAtvTextValue).append("'; ");
                }
            }
        }
    } // mehtod checkAttributeTypeAndValue

    private static List<String> sort(final List<String> contentList,
            final List<Pattern> patternList) {
        List<String> sorted = new ArrayList<>(contentList.size());
        for (Pattern p : patternList) {
            for (String value : contentList) {
                if (!sorted.contains(value) && p.matcher(value).matches()) {
                    sorted.add(value);
                }
            }
        }
        for (String value : contentList) {
            if (!sorted.contains(value)) {
                sorted.add(value);
            }
        }
        return sorted;
    }

    private static boolean matchStringType(final ASN1Encodable atvValue,
            final StringType stringType) {
        boolean correctStringType = true;
        switch (stringType) {
        case bmpString:
            correctStringType = (atvValue instanceof DERBMPString);
            break;
        case printableString:
            correctStringType = (atvValue instanceof DERPrintableString);
            break;
        case teletexString:
            correctStringType = (atvValue instanceof DERT61String);
            break;
        case utf8String:
            correctStringType = (atvValue instanceof DERUTF8String);
            break;
        case ia5String:
            correctStringType = (atvValue instanceof DERIA5String);
            break;
        default:
            throw new RuntimeException("should not reach here, unknown StringType " + stringType);
        } // end switch
        return correctStringType;
    }

    private static String getRdnTextValueOfRequest(final RDN requestedRdn)
            throws BadCertTemplateException {
        ASN1ObjectIdentifier type = requestedRdn.getFirst().getType();
        ASN1Encodable vec = requestedRdn.getFirst().getValue();
        if (ObjectIdentifiers.DN_DATE_OF_BIRTH.equals(type)) {
            if (!(vec instanceof ASN1GeneralizedTime)) {
                throw new BadCertTemplateException("requested RDN is not of GeneralizedTime");
            }
            return ((ASN1GeneralizedTime) vec).getTimeString();
        } else if (ObjectIdentifiers.DN_POSTAL_ADDRESS.equals(type)) {
            if (!(vec instanceof ASN1Sequence)) {
                throw new BadCertTemplateException("requested RDN is not of Sequence");
            }

            ASN1Sequence seq = (ASN1Sequence) vec;
            final int n = seq.size();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                ASN1Encodable obj = seq.getObjectAt(i);
                String textValue = X509Util.rdnValueToString(obj);
                sb.append("[").append(i).append("]=").append(textValue).append(",");
            }

            return sb.toString();
        } else {
            return X509Util.rdnValueToString(vec);
        }
    }

    private static ValidationIssue createSubjectIssue(final ASN1ObjectIdentifier subjectAttrType) {
        ValidationIssue issue;
        String attrName = ObjectIdentifiers.getName(subjectAttrType);
        if (attrName == null) {
            attrName = subjectAttrType.getId().replace('.', '_');
            issue = new ValidationIssue("X509.SUBJECT." + attrName, "attribute "
                    + subjectAttrType.getId());
        } else {
            issue = new ValidationIssue("X509.SUBJECT." + attrName, "attribute " + attrName
                    + " (" + subjectAttrType.getId() + ")");
        }
        return issue;
    }

    private static String getAtvValueString(final String name, final AttributeTypeAndValue atv,
            final StringType stringType, final StringBuilder failureMsg) {
        ASN1ObjectIdentifier type = atv.getType();
        ASN1Encodable atvValue = atv.getValue();

        if (ObjectIdentifiers.DN_DATE_OF_BIRTH.equals(type)) {
            if (!(atvValue instanceof ASN1GeneralizedTime)) {
                failureMsg.append(name).append(" is not of type GeneralizedTime; ");
                return null;
            }
            return ((ASN1GeneralizedTime) atvValue).getTimeString();
        } else if (ObjectIdentifiers.DN_POSTAL_ADDRESS.equals(type)) {
            if (!(atvValue instanceof ASN1Sequence)) {
                failureMsg.append(name).append(" is not of type Sequence; ");
                return null;
            }

            ASN1Sequence seq = (ASN1Sequence) atvValue;
            final int n = seq.size();

            StringBuilder sb = new StringBuilder();
            boolean validEncoding = true;
            for (int i = 0; i < n; i++) {
                ASN1Encodable obj = seq.getObjectAt(i);
                if (!matchStringType(obj, stringType)) {
                    failureMsg.append(name).append(".[").append(i).append("] is not of type ")
                        .append(stringType.name()).append("; ");
                    validEncoding = false;
                    break;
                }

                String textValue = X509Util.rdnValueToString(obj);
                sb.append("[").append(i).append("]=").append(textValue).append(",");
            }

            if (!validEncoding) {
                return null;
            }

            return sb.toString();
        } else {
            if (!matchStringType(atvValue, stringType)) {
                failureMsg.append(name).append(" is not of type " + stringType.name()).append("; ");
                return null;
            }

            return X509Util.rdnValueToString(atvValue);
        }
    } // method getAtvValueString

}
