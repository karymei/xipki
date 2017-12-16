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

package org.xipki.ca.api.profile;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class CertValidity implements Comparable<CertValidity> {

    public enum Unit {

        YEAR("y"),
        DAY("d"),
        HOUR("h");

        private String suffix;

        Unit(final String suffix) {
            this.suffix = suffix;
        }

        public String suffix() {
            return suffix;
        }

    } // enum Unit

    private static final long SECOND = 1000L;

    private static final long HOUR = 60L * 60 * SECOND;

    private static final long DAY = 24L * HOUR;

    private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");

    private final int validity;
    private final Unit unit;

    public CertValidity(final int validity, final Unit unit) {
        this.validity = ParamUtil.requireMin("validity", validity, 1);
        this.unit = ParamUtil.requireNonNull("unit", unit);
    }

    public static CertValidity getInstance(final String validityS) {
        ParamUtil.requireNonBlank("validityS", validityS);

        final int len = validityS.length();
        final char suffix = validityS.charAt(len - 1);
        Unit unit;
        String numValdityS;
        if (suffix == 'y' || suffix == 'Y') {
            unit = Unit.YEAR;
            numValdityS = validityS.substring(0, len - 1);
        } else if (suffix == 'd' || suffix == 'D') {
            unit = Unit.DAY;
            numValdityS = validityS.substring(0, len - 1);
        } else if (suffix == 'h' || suffix == 'H') {
            unit = Unit.HOUR;
            numValdityS = validityS.substring(0, len - 1);
        } else if (suffix >= '0' && suffix <= '9') {
            unit = Unit.DAY;
            numValdityS = validityS;
        } else {
            throw new IllegalArgumentException(String.format("invalid validityS: %s", validityS));
        }

        int validity;
        try {
            validity = Integer.parseInt(numValdityS);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(String.format("invalid validityS: %s", validityS));
        }
        return new CertValidity(validity, unit);
    } // method getInstance

    public int validity() {
        return validity;
    }

    public Unit unit() {
        return unit;
    }

    public Date add(final Date referenceDate) {
        switch (unit) {
        case HOUR:
            return new Date(referenceDate.getTime() + HOUR - SECOND);
        case DAY:
            return new Date(referenceDate.getTime() + DAY - SECOND);
        case YEAR:
            Calendar cal = Calendar.getInstance(TIMEZONE_UTC);
            cal.setTime(referenceDate);
            cal.add(Calendar.YEAR, validity);
            cal.add(Calendar.SECOND, -1);

            int month = cal.get(Calendar.MONTH);
            // February
            if (month == 1) {
                int day = cal.get(Calendar.DAY_OF_MONTH);
                if (day > 28) {
                    int year = cal.get(Calendar.YEAR);
                    day = isLeapYear(year) ? 29 : 28;
                }
            }

            return cal.getTime();
        default:
            throw new RuntimeException(String.format(
                    "should not reach here, unknown CertValidity.Unit %s", unit));
        }
    } // method add

    private int approxHours() {
        switch (unit) {
        case HOUR:
            return validity;
        case DAY:
            return 24 * validity;
        case YEAR:
            return (365 * validity + validity / 4) * 24;
        default:
            throw new RuntimeException(String.format(
                    "should not reach here, unknown CertValidity.Unit %s", unit));
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public int compareTo(final CertValidity obj) {
        ParamUtil.requireNonNull("obj", obj);
        if (unit == obj.unit) {
            if (validity == obj.validity) {
                return 0;
            }

            return (validity < obj.validity) ? -1 : 1;
        } else {
            int thisHours = approxHours();
            int thatHours = obj.approxHours();
            if (thisHours == thatHours) {
                return 0;
            } else {
                return (thisHours < thatHours) ? -1 : 1;
            }
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof CertValidity)) {
            return false;
        }

        CertValidity other = (CertValidity) obj;
        return unit == other.unit && validity == other.validity;
    }

    @Override
    public String toString() {
        switch (unit) {
        case HOUR:
            return validity + "h";
        case DAY:
            return validity + "d";
        case YEAR:
            return validity + "y";
        default:
            throw new RuntimeException(String.format(
                    "should not reach here, unknown CertValidity.Unit %s", unit));
        }
    }

    private static boolean isLeapYear(final int year) {
        if (year % 4 != 0) {
            return false;
        } else if (year % 100 != 0) {
            return true;
        } else {
            return year % 400 == 0;
        }
    }

}
