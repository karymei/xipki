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

package org.xipki.ca.api.profile;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class Range {

    private Integer min;

    private Integer max;

    public Range(final Integer min, final Integer max) {
        setRange(min, max);
    }

    public Integer min() {
        return min;
    }

    public Integer max() {
        return max;
    }

    public void setRange(final Integer min, final Integer max) {
        if (min == null && max == null) {
            throw new IllegalArgumentException("min and max must not be both null");
        }
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(String.format(
                    "min must not be greater than max: %d > %d", min, max));
        }
        this.min = min;
        this.max = max;
    }

    public boolean match(final int val) {
        if (min != null && val < min) {
            return false;
        }
        if (max != null && val > max) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append("[");
        if (min != null) {
            sb.append(min);
        }
        sb.append(", ");
        if (max != null) {
            sb.append(max);
        }
        sb.append("]");
        return sb.toString();
    }

}
