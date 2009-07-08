/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Copyright (C) 2006-2008 The OpenNMS Group, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc.:
 *
 *      51 Franklin Street
 *      5th Floor
 *      Boston, MA 02110-1301
 *      USA
 *
 * For more information contact:
 *
 *      OpenNMS Licensing <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 *
 *******************************************************************************/

package org.opennms.netmgt.dao;

import java.util.Collection;
import java.util.Collections;

import org.opennms.netmgt.config.CollectdPackage;
import org.opennms.netmgt.config.collectd.Collector;

public class CollectorConfigDaoStub implements CollectorConfigDao {

    /*
    public Set getCollectorNames() {
        // TODO Auto-generated method stub
        return null;
    }
    */

    public int getSchedulerThreads() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
    public Collection getSpecificationsForInterface(OnmsIpInterface iface, String svcName) {
        // TODO Auto-generated method stub
        return null;
    }
    */

    public Collection<Collector> getCollectors() {
        // TODO Auto-generated method stub
        return null;
    }

    public void rebuildPackageIpListMap() {
        // do nothing
    }

    public CollectdPackage getPackage(String name) {
        return null;
    }

    public Collection<CollectdPackage> getPackages() {
        return Collections.emptySet();
    }

}
