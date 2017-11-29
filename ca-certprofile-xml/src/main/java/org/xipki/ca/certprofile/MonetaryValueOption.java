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

package org.xipki.ca.certprofile;

import org.bouncycastle.asn1.x509.qualified.Iso4217CurrencyCode;
import org.xipki.ca.certprofile.x509.jaxb.Range2Type;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

class MonetaryValueOption {

    private final Iso4217CurrencyCode currency;

    private final String currencyString;

    private final Range2Type amountRange;

    private final Range2Type exponentRange;

    public MonetaryValueOption(final Iso4217CurrencyCode currency, final Range2Type amountRange,
            final Range2Type exponentRange) {
        this.currency = ParamUtil.requireNonNull("currency", currency);
        this.amountRange = ParamUtil.requireNonNull("amountRange", amountRange);
        this.exponentRange = ParamUtil.requireNonNull("exponentRange", exponentRange);

        this.currencyString = currency.isAlphabetic() ? currency.getAlphabetic().toUpperCase()
                : Integer.toString(currency.getNumeric());
    }

    public Iso4217CurrencyCode currency() {
        return currency;
    }

    public Range2Type amountRange() {
        return amountRange;
    }

    public Range2Type exponentRange() {
        return exponentRange;
    }

    public String currencyString() {
        return currencyString;
    }

}
