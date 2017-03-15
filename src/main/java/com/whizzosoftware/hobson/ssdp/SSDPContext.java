/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.ssdp;

import com.whizzosoftware.hobson.api.disco.DeviceAdvertisement;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * An abstraction for actions taken by SSDPInboundHandler.
 *
 * @author Dan Noguerol
 */
public interface SSDPContext {
    void processDiscoveryRequest(InetSocketAddress address, SSDPPacket packet) throws IOException;
    void publishDeviceAdvertisement(DeviceAdvertisement advertisement, boolean internal);
    Future executeInEventLoop(Runnable runnable);
    void sendDiscoveryPacket();
}
