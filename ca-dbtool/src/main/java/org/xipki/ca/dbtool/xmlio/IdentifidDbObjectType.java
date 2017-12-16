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

package org.xipki.ca.dbtool.xmlio;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public abstract class IdentifidDbObjectType extends DbDataObject {

    public static final String TAG_ID = "id";

    public static final String TAG_FILE = "file";

    private Long id;

    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public void validate() throws InvalidDataObjectException {
        assertNotNull(TAG_ID, id);
    }

}
