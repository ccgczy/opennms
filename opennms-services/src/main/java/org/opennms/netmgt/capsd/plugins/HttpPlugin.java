//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2009 The OpenNMS Group, Inc. All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2009 May 07: Add response-text property, refactor string constants, indent. - jeffg@opennms.org
// 2004 Apr 30: Make this extend AbstractTcpPlugin and move code up.
// 2003 Jul 21: Explicitly close sockets.
// 2003 Jul 18: Fixed exception to enable retries.
// 2003 Jan 31: Cleaned up some unused imports.
// 2003 Jan 29: Added response time
// 2002 Nov 14: Used non-blocking I/O for speed improvements.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp. All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//       
// For more information contact:
//      OpenNMS Licensing <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
// Tab Size = 8
//

package org.opennms.netmgt.capsd.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.capsd.AbstractTcpPlugin;
import org.opennms.netmgt.capsd.ConnectionConfig;

/**
 * <P>
 * This class is designed to be used by the capabilities daemon to test for the
 * existance of an HTTP server on remote interfaces. The class implements the
 * Plugin interface that allows it to be used along with other plugins by the
 * daemon.
 * 
 * This plugin generates a HTTP GET request and checks the return code returned
 * by the remote host to determine if it supports the protocol.
 * 
 * The remote host's response will be deemed valid if the return code falls in
 * the 100 to 599 range (inclusive).
 * 
 * This is based on the following information from RFC 1945 (HTTP 1.0) HTTP 1.0
 * GET return codes: 1xx: Informational - Not used, future use 2xx: Success 3xx:
 * Redirection 4xx: Client error 5xx: Server error
 * </P>
 * 
 * This plugin generates a HTTP GET request and checks the return code returned
 * by the remote host to determine if it supports the protocol.
 * 
 * The remote host's response will be deemed valid if the return code falls in
 * the 100 to 599 range (inclusive).
 * 
 * This is based on the following information from RFC 1945 (HTTP 1.0) HTTP 1.0
 * GET return codes: 1xx: Informational - Not used, future use 2xx: Success 3xx:
 * Redirection 4xx: Client error 5xx: Server error
 * </P>
 * 
 * This plugin generates a HTTP GET request and checks the return code returned
 * by the remote host to determine if it supports the protocol.
 * 
 * The remote host's response will be deemed valid if the return code falls in
 * the 100 to 599 range (inclusive).
 * 
 * This is based on the following information from RFC 1945 (HTTP 1.0) HTTP 1.0
 * GET return codes: 1xx: Informational - Not used, future use 2xx: Success 3xx:
 * Redirection 4xx: Client error 5xx: Server error
 * </P>
 * 
 * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
 * @author <A HREF="mailto:sowmya@opennms.org">Sowmya </A>
 * @author <A HREF="mailto:weave@oculan.com">Weaver </A>
 * @author <A HREF="http://www.opennms.org">OpenNMS </A>
 * 
 * @version 1.1.1.1
 * 
 */
public class HttpPlugin extends AbstractTcpPlugin {

    // Names of properties configured for the protocol-plugin
    protected static final String PROPERTY_NAME_PORT = "port";
    protected static final String PROPERTY_NAME_MAX_RET_CODE = "max-ret-code";
    protected static final String PROPERTY_NAME_RETURN_CODE = "check-return-code";
    protected static final String PROPERTY_NAME_URL = "url";
    protected static final String PROPERTY_NAME_RESPONSE_TEXT = "response-text";

    /**
     * Boolean indicating whether to check for a return code
     */
    public static final boolean CHECK_RETURN_CODE = true;

    /**
     * <P>
     * The default ports on which the host is checked to see if it supports
     * HTTP.
     * </P>
     */
    private static final int[] DEFAULT_PORTS = { 80, 8080, 8888 };

    /**
     * Default number of retries for HTTP requests.
     */
    private final static int DEFAULT_RETRY = 0;

    /**
     * Default timeout (in milliseconds) for HTTP requests.
     */
    private final static int DEFAULT_TIMEOUT = 5000; // in milliseconds

    public static final String PROTOCOL_NAME = "HTTP";

    /**
     * The query to send to the HTTP server
     */
    public static final String QUERY_STRING = "GET / HTTP/1.0\r\n\r\n";

    /**
     * The query to send to the HTTP server
     */
    public static final String DEFAULT_URL = "/";

    /**
     * A string to look for in the response from the server
     */
    public static final String RESPONSE_STRING = "HTTP/";

    /**
     * Boolean indicating whether to check for a return code
     */
    private boolean m_checkReturnCode = true;

    /**
     * The default ports to check on a server
     */
    private int[] m_defaultPorts;

    /**
     * The query to send to the HTTP server
     */
    private String m_queryString = "GET / HTTP/1.0\r\n\r\n";

    /**
     * A string to look for in the response from the server
     */
    private String m_responseString = "HTTP/";

    /**
     * @param protocol
     * @param defaultPort
     * @param defaultTimeout
     * @param defaultRetries
     */
    public HttpPlugin() {
        this(PROTOCOL_NAME, CHECK_RETURN_CODE, QUERY_STRING, RESPONSE_STRING, DEFAULT_PORTS);
    }

    protected HttpPlugin(String protocolName, boolean checkReturnCode, String queryString, String responseString) {
        this(protocolName, checkReturnCode, queryString, responseString, DEFAULT_PORTS);
    }

    protected HttpPlugin(String protocolName, boolean checkReturnCode, String queryString, String responseString, int[] defaultPorts) {
        super(protocolName, DEFAULT_TIMEOUT, DEFAULT_RETRY);
        m_checkReturnCode = checkReturnCode;
        m_queryString = queryString;
        m_responseString = responseString;
        m_defaultPorts = defaultPorts;
    }

    /**
     * @param sChannel
     * @param isAServer
     * @return
     * @throws IOException
     */
    protected boolean checkProtocol(Socket socket, ConnectionConfig config) throws IOException {
        boolean isAServer = false;

        m_queryString = "GET " + config.getKeyedString(PROPERTY_NAME_URL, DEFAULT_URL) + " HTTP/1.0\r\n\r\n";

        ThreadCategory log = ThreadCategory.getInstance(getClass());
        if (log.isDebugEnabled()) {
            log.debug( "Query: " + m_queryString);
        }

        try {
            BufferedReader lineRdr = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            socket.getOutputStream().write(m_queryString.getBytes());
            char [] cbuf = new char[ 1024 ];
            int chars = 0;
            StringBuffer response = new StringBuffer();
            try {
                while ((chars = lineRdr.read( cbuf, 0, 1024)) != -1)
                {
                    String line = new String( cbuf, 0, chars );
                    if (log.isDebugEnabled()) {
                        log.debug( "Read: " + line.length() + " bytes: [" + line.toString() + "] from socket." );
                    }
                    response.append( line );
                }
            } catch ( java.net.SocketTimeoutException timeoutEx ) {
                if ( timeoutEx.bytesTransferred > 0 )
                {
                    String line = new String( cbuf, 0, timeoutEx.bytesTransferred );
                    if (log.isDebugEnabled()) {
                        log.debug( "Read: " + line.length() + " bytes: [" + line.toString() + "] from socket @ timeout!" );
                    }
                    response.append(line);
                }
            }
            if (response.toString() != null && response.toString().indexOf(m_responseString) > -1) {
                if (m_checkReturnCode) {
                    int maxRetCode = config.getKeyedInteger(PROPERTY_NAME_MAX_RET_CODE, 399);
                    if ( (DEFAULT_URL.equals(config.getKeyedString(PROPERTY_NAME_URL, DEFAULT_URL))) || (config.getKeyedBoolean(PROPERTY_NAME_RETURN_CODE, true) == false) )
                    {
                        maxRetCode = 600;
                    }	
                    StringTokenizer t = new StringTokenizer(response.toString());
                    t.nextToken();
                    int rVal = Integer.parseInt(t.nextToken());
                    if (log.isDebugEnabled()) {
                        log.debug(getPluginName() + ": Request returned code: " + rVal);
                    }
                    if (rVal >= 99 && rVal <= maxRetCode )
                        isAServer = true;
                } else {
                    isAServer = true;
                }
                if (isAServer) {
                    isAServer = checkResponseBody(config, response.toString());
                }
            }
        } catch (SocketException e) {
            log.debug(getPluginName() + ": a protocol error occurred talking to host " + config.getInetAddress().getHostAddress(), e);
            isAServer = false;
        } catch (NumberFormatException e) {
            log.debug(getPluginName() + ": failed to parse response code from host " + config.getInetAddress().getHostAddress(), e);
            isAServer = false;
        }
        return isAServer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opennms.netmgt.capsd.AbstractTcpPlugin#getConnectionConfigList(java.util.Map,
     *      java.net.InetAddress)
     */
    protected List<ConnectionConfig> getConnectionConfigList(Map<String, Object> qualifiers, InetAddress address) {
        int[] ports = getKeyedIntegerArray(qualifiers, PROPERTY_NAME_PORT, m_defaultPorts);

        List<ConnectionConfig> list = new LinkedList<ConnectionConfig>();
        for (int i = 0; i < ports.length; i++) {
            list.add(createConnectionConfig(address, ports[i]));
        }
        return list;
    }
    
    /**
     * Checks the response body as a substring or regular expression match
     * according to the leading-tilde convention
     * 
     * @param config ConnectionConfig object from which response-text property is extracted
     * @param response Body of HTTP response to check
     * @return Whether the response matches the response-text property
     */
    protected boolean checkResponseBody(ConnectionConfig config, String response) {
        String expectedResponse = config.getKeyedString(PROPERTY_NAME_RESPONSE_TEXT, null);
        if (expectedResponse == null) {
            return true;
        }
        if (expectedResponse.startsWith("~")) {
            Pattern bodyPat = Pattern.compile(expectedResponse.substring(1), Pattern.DOTALL);
            return bodyPat.matcher(response).matches();
        } else {
            return response.contains(expectedResponse);
        }
    }
}
