/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Copyright (C) 2005-2009 The OpenNMS Group, Inc.  All rights reserved.
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


package org.opennms.netmgt.snmp;

import java.net.InetAddress;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.core.xml.bind.InetAddressXmlAdapter;

/**
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 */
@XmlRootElement(name="snmpAgentConfig")
public class SnmpAgentConfig extends SnmpConfiguration {
    
    private InetAddress m_address;
    private InetAddress m_proxyFor;
    
    public SnmpAgentConfig() {
        this(null);
    }
    
    public SnmpAgentConfig(InetAddress agentAddress) {
        this(agentAddress, SnmpConfiguration.DEFAULTS);
    }
    
    public SnmpAgentConfig(InetAddress agentAddress, SnmpConfiguration defaults) {
        super(defaults);
        m_address = agentAddress;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer("AgentConfig[");
        buff.append("Address: "+getAddress());
        buff.append(", ProxyForAddress: "+getProxyFor());
        buff.append(", Port: "+getPort());
        buff.append(", Community: "+getReadCommunity());
        buff.append(", Timeout: "+getTimeout());
        buff.append(", Retries: "+getRetries());
        buff.append(", MaxVarsPerPdu: "+getMaxVarsPerPdu());
        buff.append(", MaxRepititions: "+getMaxRepetitions());
        buff.append(", Max request size: "+getMaxRequestSize());
        buff.append(", Version: "+versionToString(getVersion()));
        if (getVersion() == VERSION3) {
            buff.append(", Security level: "+getSecurityLevel());
            buff.append(", Security name: "+getSecurityName());
            buff.append(", auth-passphrase: "+getAuthPassPhrase());
            buff.append(", auth-protocol: "+getAuthProtocol());
            buff.append(", priv-passprhase: "+getPrivPassPhrase());
            buff.append(", priv-protocol: "+getPrivProtocol());
        }
        buff.append("]");
        return buff.toString();
    }


    @XmlJavaTypeAdapter(InetAddressXmlAdapter.class)
    public InetAddress getAddress() {
        return m_address;
    }

    public void setAddress(InetAddress address) {
        m_address = address;
    }

    public InetAddress getProxyFor() {
        return m_proxyFor;
    }
    
    public void setProxyFor(InetAddress address) {
        m_proxyFor = address;
    }
    
}
