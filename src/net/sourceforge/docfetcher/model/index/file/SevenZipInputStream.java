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

package net.sourceforge.docfetcher.model.index.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Tran Nam Quang
 */
final class SevenZipInputStream extends SevenZip.IInStream {
	
	private final RandomAccessFile file;
	
	public SevenZipInputStream(File file) throws IOException {
		this.file = new RandomAccessFile(file.getPath(), "r");
	}
	
	public long Seek(long offset, int seekOrigin) throws IOException {
		if (seekOrigin == STREAM_SEEK_SET) {
			file.seek(offset);
		} else if (seekOrigin == STREAM_SEEK_CUR) {
			file.seek(offset + file.getFilePointer());
		}
		return file.getFilePointer();
	}
	
	public int read() throws IOException {
		return file.read();
	}
	
	public int read(byte[] data, int off, int size) throws IOException {
		return file.read(data, off, size);
	}
	
	public void close() throws IOException {
		file.close();
	}
	
}