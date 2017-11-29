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

package org.xipki.ca.server.mgmt.api;

import org.xipki.ca.api.NameId;
import org.xipki.ca.api.profile.CertValidity;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class ChangeCaEntry {

    private final NameId ident;

    private CaStatus status;

    private CertValidity maxValidity;

    private String signerType;

    private String signerConf;

    private String cmpControlName;

    private String responderName;

    private Boolean duplicateKeyPermitted;

    private Boolean duplicateSubjectPermitted;

    private Boolean saveRequest;

    private ValidityMode validityMode;

    private Integer permission;

    private Integer keepExpiredCertInDays;

    private Integer expirationPeriod;

    private String extraControl;

    public ChangeCaEntry(final NameId ident) throws CaMgmtException {
        this.ident = ParamUtil.requireNonNull("ident", ident);
    }

    public NameId ident() {
        return ident;
    }

    public CaStatus status() {
        return status;
    }

    public void setStatus(final CaStatus status) {
        this.status = status;
    }

    public CertValidity maxValidity() {
        return maxValidity;
    }

    public void setMaxValidity(final CertValidity maxValidity) {
        this.maxValidity = maxValidity;
    }

    public String signerType() {
        return signerType;
    }

    public void setSignerType(final String signerType) {
        this.signerType = signerType;
    }

    public String signerConf() {
        return signerConf;
    }

    public void setSignerConf(final String signerConf) {
        this.signerConf = signerConf;
    }

    public String cmpControlName() {
        return cmpControlName;
    }

    public void setCmpControlName(final String cmpControlName) {
        this.cmpControlName = (cmpControlName == null) ? null : cmpControlName.toUpperCase();
    }

    public String responderName() {
        return responderName;
    }

    public void setResponderName(final String responderName) {
        this.responderName = (responderName == null) ? null : responderName.toUpperCase();
    }

    public Boolean duplicateKeyPermitted() {
        return duplicateKeyPermitted;
    }

    public void setDuplicateKeyPermitted(final Boolean duplicateKeyPermitted) {
        this.duplicateKeyPermitted = duplicateKeyPermitted;
    }

    public Boolean duplicateSubjectPermitted() {
        return duplicateSubjectPermitted;
    }

    public void setDuplicateSubjectPermitted(final Boolean duplicateSubjectPermitted) {
        this.duplicateSubjectPermitted = duplicateSubjectPermitted;
    }

    public ValidityMode validityMode() {
        return validityMode;
    }

    public void setValidityMode(final ValidityMode validityMode) {
        this.validityMode = validityMode;
    }

    public Boolean saveRequest() {
        return saveRequest;
    }

    public void setSaveRequest(Boolean saveRequest) {
        this.saveRequest = saveRequest;
    }

    public Integer permission() {
        return permission;
    }

    public void setPermission(final Integer permission) {
        this.permission = permission;
    }

    public Integer expirationPeriod() {
        return expirationPeriod;
    }

    public void setExpirationPeriod(final Integer expirationPeriod) {
        this.expirationPeriod = expirationPeriod;
    }

    public Integer keepExpiredCertInDays() {
        return keepExpiredCertInDays;
    }

    public void setKeepExpiredCertInDays(final Integer days) {
        this.keepExpiredCertInDays = days;
    }

    public String extraControl() {
        return extraControl;
    }

    public void setExtraControl(final String extraControl) {
        this.extraControl = extraControl;
    }

}
