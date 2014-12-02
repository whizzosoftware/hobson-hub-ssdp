/*******************************************************************************
 * Copyright (c) 2014 Whizzo Software, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.whizzosoftware.hobson.ssdp;

import com.whizzosoftware.hobson.api.disco.DeviceBridgeScanner;
import com.whizzosoftware.hobson.api.disco.DiscoManager;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;

/**
 * Activator class for the bundle.
 *
 * @author Dan Noguerol
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        Hashtable props = new Hashtable();
        props.put("pluginId", context.getBundle().getSymbolicName());
        props.put("id", SSDPDeviceBridgeScanner.ID);
        manager.add(
            createComponent().
                setInterface(DeviceBridgeScanner.class.getName(), props).
                setImplementation(SSDPDeviceBridgeScanner.class).
                add(createServiceDependency().setService(DiscoManager.class).setRequired(true))
        );
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }
}
