/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.ssdp;

import com.whizzosoftware.hobson.api.disco.DeviceAdvertisement;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * A handler for inbound SSDP packets. It delegates the appropriate action to the SSDPContext object that it
 * is provided.
 *
 * @author Dan Noguerol
 */
public class SSDPInboundHandler implements ChannelInboundHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SSDPContext context;

    public SSDPInboundHandler(SSDPContext context) {
        this.context = context;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        logger.trace("channelRegistered");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        logger.trace("channelUnregistered");
    }

    @Override
    public void channelActive(ChannelHandlerContext channelHandlerContext) throws Exception {
        logger.trace("channelActive");
    }

    @Override
    public void channelInactive(ChannelHandlerContext channelHandlerContext) throws Exception {
        logger.trace("channelInactive");
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        final DatagramPacket p = (io.netty.channel.socket.DatagramPacket)o;
        ByteBuf buf = p.content();
        byte[] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        final String data = new String(b, 0, b.length, "UTF8");
        buf.release();
        logger.trace("Received data from {}: {}", p.sender().getHostString(), data);

        // publish the advertisement
        try {
            final SSDPPacket packet = SSDPPacket.createWithData(data);
            // ignore packets that originated from Hobson
            if (!p.sender().getAddress().equals(InetAddress.getLocalHost())) {
                if ("M-SEARCH".equals(packet.getMethod())) {
                    context.executeInEventLoop(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                context.processDiscoveryRequest(p.sender(), packet);
                            } catch (Throwable e) {
                                logger.error("Error processing discovery packet", e);
                            }
                        }
                    });
                } else if (packet.getUSN() != null && packet.getLocation() != null) {
                    // execute this in the event loop so we can get on with processing UDP packets as
                    // quickly as possible
                    context.executeInEventLoop(new Runnable() {
                        @Override
                        public void run() {
                            context.publishDeviceAdvertisement(new DeviceAdvertisement.Builder(packet.getUSN(), SSDPPacket.PROTOCOL_ID).rawData(data).object(packet).build(), false);
                        }
                    });
                } else {
                    logger.trace("Ignoring SSDP packet with USN {} and location: {}", packet.getUSN(), packet.getLocation());
                }
            }
        } catch (Throwable e) {
            logger.error("Error creating SSDP packet", e);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext channelHandlerContext) throws Exception {
        logger.trace("channelReadComplete");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        logger.trace("userEventTriggered");
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext channelHandlerContext) throws Exception {
        logger.trace("channelWritabilityChanged");
    }

    @Override
    public void handlerAdded(ChannelHandlerContext channelHandlerContext) throws Exception {
        logger.trace("handlerAdded");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext channelHandlerContext) throws Exception {
        logger.trace("handlerRemoved");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
        logger.error("Exception in SSDP handler", throwable);
    }
}
