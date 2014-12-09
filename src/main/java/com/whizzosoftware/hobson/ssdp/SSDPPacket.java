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

    private static final String HOST = "HOST";
    private static final String LOCATION = "LOCATION";
    private static final String NT = "NT";
    private static final String NTS = "NTS";
    private static final String SERVER = "SERVER";
    private static final String ST = "ST";
    private static final String USN = "USN";

    private Map<String,String> headerMap = new HashMap<>();

    public SSDPPacket(String data) {
        Scanner s = new Scanner(data);

        // skip the HTTP response header
        s.nextLine();

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

    public String getHost() {
        return headerMap.get(HOST);
    }

    public String getLocation() {
        return headerMap.get(LOCATION);
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

    public String getST() {
        return headerMap.get(ST);
    }

    public String getUSN() {
        return headerMap.get(USN);
    }

    public String getHeader(String name) {
        return headerMap.get(name);
    }
}
