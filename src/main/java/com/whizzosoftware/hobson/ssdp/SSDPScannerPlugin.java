/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.ssdp;

import com.whizzosoftware.hobson.api.disco.DeviceAdvertisement;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Dictionary;

/**
 * Class that looks for devices advertising via SSDP and publishes them to a DiscoManager.
 *
 * @author Dan Noguerol
 */
public class SSDPScannerPlugin extends AbstractHobsonPlugin implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int PORT = 1900;

    private Thread discoThread;
    private InetAddress address;
    private MulticastSocket multicastSocket;

    public SSDPScannerPlugin(String pluginId) {
        super(pluginId);
    }

    @Override
    public String getName() {
        return "SSDP Scanner Plugin";
    }

    @Override
    public void onStartup(Dictionary config) {
        try {
            logger.info("SSDP scanner starting");
            address = InetAddress.getByName("239.255.255.250");
            if (discoThread == null) {
                discoThread = new Thread(this, "SSDP Scanner");
                discoThread.start();
            }
        } catch (UnknownHostException e) {
            logger.error("Unable to lookup UDP address", e);
        }
    }

    @Override
    public void onShutdown() {
        logger.info("SSDP scanner stopping");
        if (discoThread != null) {
            discoThread.interrupt();
        }
    }

    @Override
    public void onPluginConfigurationUpdate(Dictionary dictionary) {
    }

    public void refresh() {
        try {
            sendDiscoveryPacket();
        } catch (IOException e) {
            logger.error("Error sending SSDP discovery packet", e);
        }
    }

    @Override
    public void run() {
        logger.debug("SSDP discovery thread is starting");

        int retryCount = 0;

        try {
            // create the initial socket
            createSocket(address);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    logger.trace("Waiting for next response");

                    // wait for a new packet to come in from the socket
                    byte[] buf = new byte[8192];
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(p);

                    // reset the retry count since we successfully received some data
                    retryCount = 0;

                    // create a String from the packet data
                    final String data = new String(p.getData(), 0, p.getLength(), "UTF8");
                    logger.trace("Received data: {}", data);

                    // publish the advertisement
                    try {
                        final SSDPPacket packet = new SSDPPacket(data);
                        if (packet.getUSN() != null && packet.getLocation() != null) {
                            // execute this in the event loop so we can get on with processing UDP packets as
                            // quickly as possible
                            executeInEventLoop(new Runnable() {
                                @Override
                                public void run() {
                                    fireDeviceAdvertisement(new DeviceAdvertisement(packet.getUSN(), SSDPPacket.PROTOCOL_ID, data, packet));
                                }
                            });
                        } else {
                            logger.trace("Ignoring SSDP packet with USN {} and location: {}", packet.getUSN(), packet.getLocation());
                        }
                    } catch (Exception e) {
                        logger.error("Error creating SSDP packet", e);
                    }
                } catch (SocketTimeoutException ste) {
                    logger.trace("Socket timed out; re-listening");
                } catch (IOException ioe) {
                    retryCount++;
                    if (retryCount < 5) {
                        logger.error("An exception occurred; re-creating socket", ioe);
                        createSocket(address);
                    } else {
                        // this will avoid spinning in a tight re-connect loop
                        // TODO: perhaps exponential decay retries?
                        throw new IOException("An excessive number of retries was detected");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("An unrecoverable exception occurred", e);
        }

        logger.debug("SSDP discovery thread is exiting");
    }

    synchronized private void createSocket(InetAddress address) throws IOException {
        if (multicastSocket != null) {
            multicastSocket.close();
        }

        // create socket
        multicastSocket = new MulticastSocket(PORT);
        multicastSocket.setReuseAddress(true);
        multicastSocket.setSoTimeout(130000);
        multicastSocket.joinGroup(address);

        sendDiscoveryPacket();
        multicastSocket.setSoTimeout(2000);
    }

    private void sendDiscoveryPacket() throws IOException {
        logger.debug("Sending SSDP discovery packet");
        byte[] disco = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 5\r\nST: ssdp:all\r\n\r\n".getBytes();
        DatagramPacket packet = new DatagramPacket(disco, disco.length, address, PORT);
        multicastSocket.send(packet);
    }
}
