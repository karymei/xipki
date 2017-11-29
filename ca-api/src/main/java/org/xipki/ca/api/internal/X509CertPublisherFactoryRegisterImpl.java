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

package org.xipki.ca.api.internal;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.api.publisher.x509.X509CertPublisher;
import org.xipki.ca.api.publisher.x509.X509CertPublisherFactory;
import org.xipki.ca.api.publisher.x509.X509CertPublisherFactoryRegister;
import org.xipki.common.ObjectCreationException;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class X509CertPublisherFactoryRegisterImpl implements X509CertPublisherFactoryRegister {

    private static final Logger LOG = LoggerFactory.getLogger(
            X509CertPublisherFactoryRegisterImpl.class);

    private ConcurrentLinkedDeque<X509CertPublisherFactory> services =
            new ConcurrentLinkedDeque<X509CertPublisherFactory>();

    @Override
    public X509CertPublisher newPublisher(final String type) throws ObjectCreationException {
        ParamUtil.requireNonBlank("type", type);

        for (X509CertPublisherFactory service : services) {
            if (service.canCreatePublisher(type)) {
                return service.newPublisher(type);
            }
        }

        throw new ObjectCreationException(
                "could not find factory to create Publisher of type " + type);
    }

    public void bindService(final X509CertPublisherFactory service) {
        //might be null if dependency is optional
        if (service == null) {
            LOG.info("bindService invoked with null.");
            return;
        }

        boolean replaced = services.remove(service);
        services.add(service);

        String action = replaced ? "replaced" : "added";
        LOG.info("{} X509CertPublisherFactory binding for {}", action, service);
    }

    public void unbindService(final X509CertPublisherFactory service) {
        //might be null if dependency is optional
        if (service == null) {
            LOG.info("unbindService invoked with null.");
            return;
        }

        if (services.remove(service)) {
            LOG.info("removed X509CertPublisherFactory binding for {}", service);
        } else {
            LOG.info("no X509CertPublisherFactory binding found to remove for {}", service);
        }
    }

}
