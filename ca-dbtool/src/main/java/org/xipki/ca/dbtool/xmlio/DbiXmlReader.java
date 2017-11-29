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

package org.xipki.ca.dbtool.xmlio;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public abstract class DbiXmlReader {

    protected final XMLStreamReader reader;

    private final XMLInputFactory factory = XMLInputFactory.newInstance();

    private final String rootElementName;

    private DbDataObject next;

    public DbiXmlReader(final String rootElementName, final InputStream xmlStream)
            throws XMLStreamException, InvalidDataObjectException {
        this.rootElementName = ParamUtil.requireNonBlank("rootElementName", rootElementName);
        ParamUtil.requireNonNull("xmlStream", xmlStream);

        synchronized (factory) {
            reader = factory.createXMLStreamReader(xmlStream);
        }

        String thisRootElement = null;
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                thisRootElement = reader.getLocalName();
                break;
            }
        }

        if (!this.rootElementName.equals(thisRootElement)) {
            throw new InvalidDataObjectException("the given XML stream does not have root element '"
                    + rootElementName + "', but '" + thisRootElement + "'");
        }

        this.next = retrieveNext();
    }

    public String rootElementName() {
        return rootElementName;
    }

    public boolean hasNext() {
        return next != null;
    }

    public DbDataObject next() throws InvalidDataObjectException, XMLStreamException {
        if (next == null) {
            throw new IllegalStateException("no more next element exists");
        }

        DbDataObject ret = next;
        next = retrieveNext();

        return ret;
    }

    protected abstract DbDataObject retrieveNext()
            throws InvalidDataObjectException, XMLStreamException;

}
