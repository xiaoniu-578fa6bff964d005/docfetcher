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

package net.sourceforge.docfetcher.model.index.outlook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingException;
import net.sourceforge.docfetcher.model.parse.ParseException;
import net.sourceforge.docfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import com.google.common.io.Closeables;
import com.pff.PSTAttachment;
import com.pff.PSTMessage;

/**
 * @author Tran Nam Quang
 */
abstract class AttachmentVisitor {
	
	private final IndexingConfig config;
	private final PSTMessage email;
	private final boolean deleteTempFiles;

	public AttachmentVisitor(	@NotNull IndexingConfig config,
								@NotNull PSTMessage email,
								boolean deleteTempFiles) {
		Util.checkNotNull(config, email);
		this.config = config;
		this.email = email;
		this.deleteTempFiles = deleteTempFiles;
	}
	
	public final void run() {
		int numberOfAttachments = email.getNumberOfAttachments();
		for (int i = 0; i < numberOfAttachments; i++) {
			String filename = null;
			File tempFile = null;
			try {
				PSTAttachment attach = email.getAttachment(i);

				// Get the filename; both long and short filenames can be used for attachments
				filename = attach.getLongFilename();
				if (filename.isEmpty())
					filename = attach.getFilename();

				// Set up input and output stream
				tempFile = config.createDerivedTempFile(filename);
				InputStream in = attach.getFileInputStream();
				FileOutputStream out = new FileOutputStream(tempFile);

				/*
				 * TODO post-release-1.1: Instead of writing to a temporary
				 * file, we could directly give the input stream to the parse
				 * service. (But temporary files would still be necessary for
				 * archives.)
				 */

				/*
				 * Copy bytes from input stream to output stream
				 * 
				 * 8176 is the block size used internally and should give the
				 * best performance.
				 */
				int bufferSize = 8176;
				byte[] buffer = new byte[bufferSize];
				int count = in.read(buffer);
				while (count > 0) {
					out.write(buffer, 0, count);
					count = in.read(buffer);
				}
				Closeables.closeQuietly(out);
				Closeables.closeQuietly(in);

				handleAttachment(filename, tempFile);
			}
			catch (CheckedOutOfMemoryError e) {
				if (filename == null)
					filename = "???";
				handleException(filename, e.getCause());
			}
			catch (Exception e) {
				if (filename == null)
					filename = "???";
				if (e instanceof IndexingException)
					e = ((IndexingException) e).getIOException();
				handleException(filename, e);
			}
			finally {
				if (deleteTempFiles && tempFile != null)
					tempFile.delete();
			}
		}
		runFinally();
	}
	
	protected abstract void handleAttachment(	@NotNull String filename,
												@NotNull File tempFile)
			throws ParseException, CheckedOutOfMemoryError;
	
	protected abstract void handleException(@NotNull String filename,
											@NotNull Throwable e);
	
	protected void runFinally() {}

}
