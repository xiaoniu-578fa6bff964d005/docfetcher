/*******************************************************************************
 * Copyright (c) 2010, 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher;

import java.io.Closeable;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;

import net.sourceforge.docfetcher.util.ConfLoader;
import net.sourceforge.docfetcher.util.LoopTimer;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.ConfLoader.Loadable;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

import de.schlichtherle.truezip.file.TFile;

/**
 * @author Tran Nam Quang
 */
privileged aspect CodeConventions {
	
	declare warning: call(* Throwable.printStackTrace*(..))
	&& !withincode(@SuppressAjWarnings * *(..)):
		"Use AppUtil.showStackTrace*(..) instead.";
	
	declare warning: call(* PrintStream.print*(..))
	&& !withincode(@SuppressAjWarnings * *(..)):
		"Don't forget to remove System.out.print*() calls after usage.";
	
	declare warning: call(LoopTimer+.new(..)):
		"Don't forget to remove the LoopTimer after usage.";
	
	declare warning: execution(!@SuppressAjWarnings boolean get*(..)):
		"Boolean getter methods should start with 'is'.";
	
	declare warning: (call(* File.list(..)) || call(* File.listFiles(..)))
	&& !withincode(@SuppressAjWarnings * *(..)):
		"Use the null-safe Util.listFiles(..) methods instead.";
	
	declare warning: (call(* File.getParent()) || call(* File.getParentFile()))
	&& !withincode(@SuppressAjWarnings * *(..)):
		"Use the null-safe Util.getParentFile(File) method instead.";
	
	declare warning: call(* Program.launch(..))
	&& !withincode(@SuppressAjWarnings * *(..)):
		"Use Util.launch(..) instead, it will also work on KDE.";
	
	declare warning: call(* File.createTempFile(..))
	&& !withincode(@SuppressAjWarnings * *(..)):
		"Use Util.createTempFile(..) instead.";
	
	declare warning: call(* File.getAbsolutePath())
	&& !withincode(@SuppressAjWarnings * *(..)):
		"Use Util.getAbsPath*(...) instead.";
	
	declare warning: call(* Closeable+.close(..))
	&& !withincode(* Closeable+.close(..)):
		"Resources should be closed with " +
		"com.google.common.io.Closeables.closeQuietly(Closeable) " +
		"inside a finally block.";
	
	declare warning: call(void Loadable.load(String))
	&& !within(ConfLoader):
		"Don't call Loadable.load(..) outside the ConfLoader class.";
	
	declare warning: (call(TFile+.new(java.io.File)) 
			|| call(TFile+.new(String))
			|| call(TFile+.new(URI))
			|| call(TFile+.new(java.io.File, String))
			|| call(TFile+.new(String, String)))
	&& !within(net.sourceforge.docfetcher..Test*)
	&& !within(net.sourceforge.docfetcher..*Test):
		"Don't construct a TrueZIP file without explicitly setting the archive detector.";
	
	declare warning: (call(* Display.syncExec(Runnable))
			|| call(* Display.asyncExec(Runnable)))
	&& !withincode(* Util.run*(..)):
		"Use the Util.run* methods instead.";
	
}
