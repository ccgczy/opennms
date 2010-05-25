//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2005 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
// OpenNMS Licensing       <license@opennms.org>
//     http://www.opennms.org/
//     http://www.opennms.com/
//
package org.opennms.netmgt.config;

import java.util.List;

import org.opennms.netmgt.xml.event.Event;

public interface EventTranslatorConfig {
	
	static final String TRANSLATOR_NAME = "OpenNMS.EventTranslator";

    /**
     * Get the list of UEIs that are registered in the passive status configuration.
     * @return list of UEIs
     */
    List<String> getUEIList();
    
    /**
     * Determine if the @param e is a translation event
     * @param e Event
     * @return true if e is a translation event
     */
    boolean isTranslationEvent(Event e);

    /**
     * Translate the @param e to a new event
     * @param e Event
     * @return a translated event
     */
	List<Event> translateEvent(Event e);
	
	void update() throws Exception;

}