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

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public enum GeneralNameTag {

    otherName(0),
    rfc822Name(1),
    dNSName(2),
    x400Adress(3),
    directoryName(4),
    ediPartyName(5),
    uniformResourceIdentifier(6),
    iPAddress(7),
    registeredID(8);

    private final int tag;

    private GeneralNameTag(final int tag) {
        this.tag = tag;
    }

    public int tag() {
        return tag;
    }

}
