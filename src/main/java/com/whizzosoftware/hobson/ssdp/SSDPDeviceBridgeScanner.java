/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.ssdp;

import com.whizzosoftware.hobson.api.disco.DeviceBridgeMetaData;
import com.whizzosoftware.hobson.api.disco.DeviceBridgeScanner;
import com.whizzosoftware.hobson.api.disco.DiscoManager;
import com.whizzosoftware.hobson.api.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;

/**
 * Device bridge scanner that looks for devices advertising via SSDP.
 *
 * @author Dan Noguerol
 */
public class SSDPDeviceBridgeScanner implements DeviceBridgeScanner, Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String ID = "ssdp";
    private static final int PORT = 1900;

    private volatile DiscoManager discoManager;

    private Thread discoThread;
    private InetAddress address;
    private MulticastSocket multicastSocket;

    @Override
    public String getPluginId() {
        return null;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    synchronized public void start() {
        try {
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
    synchronized public void stop() {
        if (discoThread != null) {
            discoThread.interrupt();
        }
    }

    @Override
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
                    processData(UserUtil.DEFAULT_USER, UserUtil.DEFAULT_HUB, new String(p.getData(), 0, p.getLength(), "UTF8"));
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
        byte[] disco = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 120\r\nST: ssdp:all\r\n".getBytes();
        DatagramPacket packet = new DatagramPacket(disco, disco.length, address, PORT);
        multicastSocket.send(packet);
    }

    private void processData(String userId, String hubId, String data) {
        logger.trace("Received data: {}", data);

        // send to DiscoManager for analysis
        DeviceBridgeMetaData entity = new DeviceBridgeMetaData(getId(), data);
        discoManager.processDeviceBridgeMetaData(userId, hubId, entity);
    }
}
