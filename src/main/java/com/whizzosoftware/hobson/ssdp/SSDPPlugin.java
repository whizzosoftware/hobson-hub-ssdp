/*
 *******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************
*/
package com.whizzosoftware.hobson.ssdp;

import com.whizzosoftware.hobson.api.disco.DeviceAdvertisement;
import com.whizzosoftware.hobson.api.hub.NetworkInfo;
import com.whizzosoftware.hobson.api.plugin.AbstractHobsonPlugin;
import com.whizzosoftware.hobson.api.plugin.PluginStatus;
import com.whizzosoftware.hobson.api.plugin.PluginType;
import com.whizzosoftware.hobson.api.property.PropertyContainer;
import com.whizzosoftware.hobson.api.property.TypedProperty;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Collection;

/**
 * Class that looks for devices advertising via SSDP and publishes them to a DiscoManager.
 *
 * @author Dan Noguerol
 */
public class SSDPPlugin extends AbstractHobsonPlugin implements SSDPContext {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String PROTOCOL = "ssdp";
    private static final int PORT = 1900;

    private NioEventLoopGroup eventLoopGroup;
    private NetworkInterface nic;
    private InetSocketAddress localAddress;
    private InetSocketAddress groupAddress;
    private NioDatagramChannel multicastChannel;
    private NioDatagramChannel localChannel;

    public SSDPPlugin(String pluginId, String version, String description) {
        super(pluginId, version, description);
    }

    @Override
    protected TypedProperty[] getConfigurationPropertyTypes() {
        return null;
    }

    @Override
    public String getName() {
        return "SSDP Plugin";
    }

    @Override
    public PluginType getType() {
        return PluginType.CORE;
    }

    @Override
    public void onStartup(PropertyContainer config) {
        logger.debug("SSDP scanner starting");
        eventLoopGroup = new NioEventLoopGroup(1);
        try {
            NetworkInfo ni = getHubManager().getLocalManager().getNetworkInfo();
            nic = ni.getNetworkInterface();
            localAddress = new InetSocketAddress(ni.getInetAddress(), 52378);
            groupAddress = new InetSocketAddress("239.255.255.250", PORT);
            createSockets();
            setStatus(PluginStatus.running());
        } catch (IOException e) {
            setStatus(PluginStatus.failed("A startup error occurred. See log for details."));
        }
    }

    @Override
    public void onShutdown() {
        logger.info("SSDP scanner stopping");
        try {
            multicastChannel.leaveGroup(groupAddress.getAddress());
            multicastChannel.close().syncUninterruptibly();
            localChannel.close().syncUninterruptibly();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onPluginConfigurationUpdate(PropertyContainer config) {
    }

    public void createSockets() {
        try {
            logger.debug("Using network interface: {}; local address: {}", nic, localAddress);

            if (nic == null) {
                logger.error("Unable to determine local NIC; discovery may not work properly");
            }

            if (nic != null) {
                Bootstrap clientBootstrap = new Bootstrap()
                    .group(eventLoopGroup)
                    .channelFactory(new ChannelFactory<Channel>() {
                        @Override
                        public Channel newChannel() {
                        return new NioDatagramChannel(InternetProtocolFamily.IPv4);
                        }
                    })
                    .localAddress(groupAddress)
                    .option(ChannelOption.IP_MULTICAST_IF, nic)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new SSDPInboundHandler(this));

                clientBootstrap.bind().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        multicastChannel = (NioDatagramChannel) channelFuture.channel();
                        multicastChannel.joinGroup(groupAddress, nic);
                    }
                });
            }

            Bootstrap serverBootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channelFactory(new ChannelFactory<Channel>() {
                    @Override
                    public Channel newChannel() {
                        return new NioDatagramChannel(InternetProtocolFamily.IPv4);
                    }
                })
                .localAddress(localAddress)
                .option(ChannelOption.IP_MULTICAST_IF, nic)
                .option(ChannelOption.SO_REUSEADDR, true)
                .handler(new SSDPInboundHandler(this));

            serverBootstrap.bind().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    localChannel = (NioDatagramChannel) channelFuture.channel();
                    sendDiscoveryPacket();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendDiscoveryPacket() {
        try {
            if (localChannel != null) {
                logger.debug("Sending SSDP discovery packet");
                byte[] disco = SSDPPacket.createSearchRequest().toString().getBytes();
                ByteBuf buf = Unpooled.copiedBuffer(disco);
                localChannel.writeAndFlush(new DatagramPacket(buf, groupAddress, localAddress)).sync();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void sendDiscoveryResponse(InetSocketAddress address, DeviceAdvertisement da) throws IOException {
        try {
            if (localChannel != null) {
                String data = SSDPPacket.createSearchResponse(da.getUri(), da.getId(), "urn").toString();
                logger.trace("Sending SSDP search response to {}: {}", address, data);
                ByteBuf buf = Unpooled.copiedBuffer(data.getBytes());
                localChannel.writeAndFlush(new DatagramPacket(buf, address, localAddress)).sync();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes a search request.
     *
     * @param address the address from which the request originated
     * @param packet the SSDPPacket that was sent
     *
     * @throws IOException on failure
     */
    public void processDiscoveryRequest(InetSocketAddress address, SSDPPacket packet) throws IOException {
        if (packet.getST() != null) {
            if ("ssdp:all".equals(packet.getST())) {
                Collection<DeviceAdvertisement> das = getDiscoManager().getInternalDeviceAdvertisements(getContext().getHubContext(), PROTOCOL);
                if (das != null) {
                    for (DeviceAdvertisement da : das) {
                        sendDiscoveryResponse(address, da);
                    }
                }
            } else {
                DeviceAdvertisement da = getDiscoManager().getInternalDeviceAdvertisement(getContext().getHubContext(), PROTOCOL, packet.getST());
                if (da != null) {
                    sendDiscoveryResponse(address, da);
                } else {
                    logger.trace("No device advertisement has been published to respond to: {}", packet.getST());
                }
            }
        }
    }
}
