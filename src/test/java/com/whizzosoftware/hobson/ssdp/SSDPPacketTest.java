/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.ssdp;

import org.junit.Test;
import static org.junit.Assert.*;

public class SSDPPacketTest {
    @Test
    public void testConstructor() {
        SSDPPacket packet = new SSDPPacket("NOTIFY * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "CACHE-CONTROL: max-age=90\r\n" +
                "LOCATION: http://192.168.0.13:49153/nmsDescription.xml\r\n" +
                "NT: upnp:rootdevice\r\n" +
                "NTS: ssdp:alive\r\n" +
                "SERVER: Windows2000/0.0 UPnP/1.0 PhilipsIntelSDK/1.4 DLNADOC/1.50\r\n" +
                "X-User-Agent: redsonic\r\n" +
                "USN: uuid:5AFEF00D-BABE-DADA-FA5A-00113215F871::upnp:rootdevice\r\n" +
                "CONTENT-LENGTH: 0\r\n\r\n");
        assertEquals("239.255.255.250:1900", packet.getHost());
        assertEquals("http://192.168.0.13:49153/nmsDescription.xml", packet.getLocation());
        assertEquals("upnp:rootdevice", packet.getNT());
        assertEquals("ssdp:alive", packet.getNTS());
        assertEquals("Windows2000/0.0 UPnP/1.0 PhilipsIntelSDK/1.4 DLNADOC/1.50", packet.getServer());
        assertEquals("uuid:5AFEF00D-BABE-DADA-FA5A-00113215F871::upnp:rootdevice", packet.getUSN());
    }

    @Test
    public void testConstructor2() {
        SSDPPacket packet = new SSDPPacket("HTTP/1.1 200 OK\r\n" +
                "CACHE-CONTROL: max-age=86400\r\n" +
                "DATE: Mon, 08 Dec 2014 13:16:05 GMT\r\n" +
                "EXT:\r\n" +
                "LOCATION: http://192.168.0.179:49153/setup.xml\r\n" +
                "OPT: \"http://schemas.upnp.org/upnp/1/0/\"; ns=01\r\n" +
                "01-NLS: 80587e26-1dd2-11b2-83d0-be74c3b5e86b\r\n" +
                "SERVER: Unspecified, UPnP/1.0, Unspecified\r\n" +
                "X-User-Agent: redsonic\r\n" +
                "ST: urn:Belkin:service:metainfo:1\r\n" +
                "USN: uuid:Insight-1_0-221437K1200D6D::urn:Belkin:service:metainfo:1\r\n\r\n");
        assertEquals("http://192.168.0.179:49153/setup.xml", packet.getLocation());
        assertEquals("urn:Belkin:service:metainfo:1", packet.getST());
    }

    @Test
    public void testConstructorWithLowercaseHeader() {
        SSDPPacket packet = new SSDPPacket("HTTP/1.1 200 OK\r\n" +
                "CACHE-CONTROL: max-age=86400\r\n" +
                "DATE: Mon, 08 Dec 2014 13:16:05 GMT\r\n" +
                "EXT:\r\n" +
                "Location: http://192.168.0.179:49153/setup.xml\r\n" +
                "OPT: \"http://schemas.upnp.org/upnp/1/0/\"; ns=01\r\n" +
                "01-NLS: 80587e26-1dd2-11b2-83d0-be74c3b5e86b\r\n" +
                "SERVER: Unspecified, UPnP/1.0, Unspecified\r\n" +
                "X-User-Agent: redsonic\r\n" +
                "ST: urn:Belkin:service:metainfo:1\r\n" +
                "USN: uuid:Insight-1_0-221437K1200D6D::urn:Belkin:service:metainfo:1\r\n\r\n");
        assertEquals("http://192.168.0.179:49153/setup.xml", packet.getLocation());
        assertEquals("urn:Belkin:service:metainfo:1", packet.getST());
    }
}
