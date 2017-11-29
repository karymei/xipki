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

package org.xipki.ocsp.api.internal;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.common.ObjectCreationException;
import org.xipki.common.util.ParamUtil;
import org.xipki.ocsp.api.OcspStore;
import org.xipki.ocsp.api.OcspStoreFactory;
import org.xipki.ocsp.api.OcspStoreFactoryRegister;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class OcspStoreFactoryRegisterImpl implements OcspStoreFactoryRegister {

    private static final Logger LOG = LoggerFactory.getLogger(
            OcspStoreFactoryRegisterImpl.class);

    private ConcurrentLinkedDeque<OcspStoreFactory> services =
            new ConcurrentLinkedDeque<OcspStoreFactory>();

    @Override
    public OcspStore newOcspStore(final String type) throws ObjectCreationException {
        ParamUtil.requireNonBlank("type", type);

        for (OcspStoreFactory service : services) {
            if (service.canCreateOcspStore(type)) {
                LOG.info("found factory to create OcspStore of type '" + type + "'");
                return service.newOcspStore(type);
            }
        }

        throw new ObjectCreationException(
                "could not find factory to create OcspStore of type '" + type + "'");
    }

    public void bindService(final OcspStoreFactory service) {
        //might be null if dependency is optional
        if (service == null) {
            LOG.info("bindService invoked with null.");
            return;
        }

        boolean replaced = services.remove(service);
        services.add(service);

        String action = replaced ? "replaced" : "added";
        LOG.info("{} CertStatusStoreFactory binding for {}", action, service);
    }

    public void unbindService(final OcspStoreFactory service) {
        //might be null if dependency is optional
        if (service == null) {
            LOG.info("unbindService invoked with null.");
            return;
        }

        if (services.remove(service)) {
            LOG.info("removed CertStatusStoreFactory binding for {}", service);
        } else {
            LOG.info("no CertStatusStoreFactory binding found to remove for '{}'", service);
        }
    }

}
