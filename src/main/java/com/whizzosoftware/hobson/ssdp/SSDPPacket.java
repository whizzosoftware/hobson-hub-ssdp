/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.ssdp;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Encapsulates the details of an SSDP packet. This is used in a DeviceAdvertisement to allow listeners to get SSDP
 * information without having to parse the raw packet themselves.
 *
 * @author Dan Noguerol
 */
public class SSDPPacket {
    public static final String PROTOCOL_ID = "ssdp";

    private static final String DEFAULT_CACHE_CONTROL = "180";
    private static final String DEFAULT_SERVER = "Hobson/1.0";

    private static final String CACHE_CONTROL = "CACHE-CONTROL";
    private static final String EXT = "EXT";
    private static final String HOST = "HOST";
    private static final String LOCATION = "LOCATION";
    private static final String MAN = "MAN";
    private static final String MX = "MX";
    private static final String NT = "NT";
    private static final String NTS = "NTS";
    private static final String SERVER = "SERVER";
    private static final String ST = "ST";
    private static final String USN = "USN";

    private String startLine;
    private Map<String,String> headerMap = new HashMap<>();

    static public SSDPPacket createWithData(String data) {
        return new SSDPPacket(data);
    }

    static public SSDPPacket createSearchRequest() {
        SSDPPacket p = new SSDPPacket();
        p.setStartLine("M-SEARCH * HTTP/1.1");
        p.setHost("239.255.255.250:1900");
        p.setMAN("\"ssdp:discover\"");
        p.setMX("5");
        p.setST("ssdp:all");
        return p;
    }

    static public SSDPPacket createSearchResponse(String location, String searchTarget, String uniqueServiceName) {
        SSDPPacket p = new SSDPPacket();
        p.setStartLine("HTTP/1.1 200 OK");
        p.setCacheControl(DEFAULT_CACHE_CONTROL);
        p.setExt("");
        p.setLocation(location);
        p.setServer(DEFAULT_SERVER);
        p.setST(searchTarget);
        p.setUSN(uniqueServiceName);
        return p;
    }

    private SSDPPacket() {}

    private SSDPPacket(String data) {
        Scanner s = new Scanner(data);

        // store the HTTP method
        this.startLine = s.nextLine();

        // store the remaining headers
        while (s.hasNextLine()) {
            String line = s.nextLine();
            int ix = line.indexOf(':');
            if (ix > -1) {
                String header = line.substring(0, ix).trim();
                String value = line.substring(ix + 1).trim();
                headerMap.put(header.toUpperCase(), value);
            }
        }
    }

    public void setStartLine(String startLine) {
        this.startLine = startLine;
    }

    public String getMethod() {
        return startLine.substring(0, startLine.indexOf(' '));
    }

    public String getCacheControl() {
        return headerMap.get(CACHE_CONTROL);
    }

    public void setCacheControl(String cacheControl) {
        headerMap.put(CACHE_CONTROL, cacheControl);
    }

    public String getExt() {
        return headerMap.get(EXT);
    }

    public void setExt(String ext) {
        headerMap.put(EXT, ext);
    }

    public String getHost() {
        return headerMap.get(HOST);
    }

    public void setHost(String host) {
        headerMap.put(HOST, host);
    }

    public String getLocation() {
        return headerMap.get(LOCATION);
    }

    public void setLocation(String location) {
        headerMap.put(LOCATION, location);
    }

    public String getMAN() {
        return headerMap.get(MAN);
    }

    public void setMAN(String man) {
        headerMap.put(MAN, man);
    }

    public void setMX(String mx) {
        headerMap.put(MX, mx);
    }

    public String getMX() {
        return headerMap.get(MX);
    }

    public String getNT() {
        return headerMap.get(NT);
    }

    public String getNTS() {
        return headerMap.get(NTS);
    }

    public String getServer() {
        return headerMap.get(SERVER);
    }

    public void setServer(String server) {
        headerMap.put(SERVER, server);
    }

    public String getST() {
        return headerMap.get(ST);
    }

    public void setST(String st) {
        headerMap.put(ST, st);
    }

    public String getUSN() {
        return headerMap.get(USN);
    }

    public void setUSN(String usn) {
        headerMap.put(USN, usn);
    }

    public String getHeader(String name) {
        return headerMap.get(name);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(this.startLine).append("\r\n");
        if (getCacheControl() != null) {
            sb.append("CACHE-CONTROL: ").append(getCacheControl()).append("\r\n");
        }
        if (getExt() != null) {
            sb.append("EXT: ").append(getExt()).append("\r\n");
        }
        if (getHost() != null) {
            sb.append("HOST: ").append(getHost()).append("\r\n");
        }
        if (getMAN() != null) {
            sb.append("MAN: ").append(getMAN()).append("\r\n");
        }
        if (getMX() != null) {
            sb.append("MX: ").append(getMX()).append("\r\n");
        }
        if (getLocation() != null) {
            sb.append("LOCATION: ").append(getLocation()).append("\r\n");
        }
        if (getNTS() != null) {
            sb.append("NTS: ").append(getNTS()).append("\r\n");
        }
        if (getServer() != null) {
            sb.append("SERVER: ").append(getServer()).append("\r\n");
        }
        if (getST() != null) {
            sb.append("ST: ").append(getST()).append("\r\n");
        }
        if (getUSN() != null) {
            sb.append("USN: ").append(getUSN()).append("\r\n");
        }
        return sb.toString();
    }
}
