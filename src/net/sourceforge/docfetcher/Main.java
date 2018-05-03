/*******************************************************************************
 * Copyright (c) 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher;

import java.lang.reflect.Method;

import net.sourceforge.docfetcher.util.SwtJarLoader;

import py4j.GatewayServer;

/**
 * @author Tran Nam Quang
 */
public final class Main {

	private Main() {
	}

	private static GatewayServer server = new GatewayServer(new Main(), 28834);
	private static void open_gatewayserver(){
		Main.server.start();
	}
	private static void shutdown_gatewayserver(){
		Main.server.shutdown();
	}

	public static void main(String[] args) throws Exception {
		open_gatewayserver();

		SwtJarLoader.loadSwtJar();
		String appClassName = "net.sourceforge.docfetcher.gui.Application";
		Class<?> appClass = Class.forName(appClassName);
		Class<?>[] paramTypes = new Class<?>[] {String[].class};
		Method launchMethod = appClass.getMethod("main", paramTypes);
		launchMethod.invoke(null, new Object[] {args});

		shutdown_gatewayserver();
	}

}
