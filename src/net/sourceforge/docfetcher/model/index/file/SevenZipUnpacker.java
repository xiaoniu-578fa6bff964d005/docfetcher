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

package net.sourceforge.docfetcher.model.index.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import com.google.common.io.NullOutputStream;

import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import SevenZip.HRESULT;
import SevenZip.Archive.IArchiveExtractCallback;
import SevenZip.Archive.IInArchive;

/**
 * @author Tran Nam Quang
 */
abstract class SevenZipUnpacker<T> {

	private static final int mode = IInArchive.NExtract_NAskMode_kExtract;

	private final IInArchive archive;

	public SevenZipUnpacker(@NotNull IInArchive archive) {
		Util.checkNotNull(archive);
		this.archive = archive;
	}

	// the given indices array will be sorted
	public final T unpack(int... unpackIndices) throws IOException {
		// don't know if J7Zip expects the indices to be sorted
		Arrays.sort(unpackIndices);

		Callback callback = new Callback(unpackIndices);
		archive.Extract(unpackIndices, unpackIndices.length, mode, callback);
		return getUnpackResult();
	}

	public abstract File getOutputFile(int index) throws IOException;

	public abstract T getUnpackResult();

	private class Callback implements IArchiveExtractCallback {
		private final int[] indices;

		public Callback(int[] indices) {
			this.indices = Util.checkNotNull(indices);
		}

		public int GetStream(	int index,
								OutputStream[] outStream,
								int askExtractMode) throws IOException {
			/*
			 * Notes:
			 * 
			 * 1) If the given index is not in the unpack array (determined by
			 * binary search), then we're probably dealing with a solid archive,
			 * and J7Zip will request output streams for all archive entries,
			 * including those we don't need. For the latter, we can give J7Zip
			 * a NullOutputStream in order to avoid consuming disk space.
			 * 
			 * 1) Wrapping the FileOutputStream into a BufferedOutputStream does
			 * not seem to have a significant effect on performance.
			 * 
			 * 2) Not sure what to do if we fail to create the output stream.
			 * Here we're letting the IOException propagate outwards, but J7zip
			 * might expect HRESULT.E_FAIL or something else.
			 */
			if (Arrays.binarySearch(indices, index) < 0)
				outStream[0] = new NullOutputStream();
			else
				outStream[0] = new FileOutputStream(getOutputFile(index));
			return HRESULT.S_OK;
		}

		public final int PrepareOperation(int askExtractMode) {
			return HRESULT.S_OK;
		}

		public final int SetCompleted(long completeValue) {
			return HRESULT.S_OK;
		}

		public final int SetOperationResult(int resultEOperationResult)
				throws IOException {
			return HRESULT.S_OK;
		}

		public final int SetTotal(long total) {
			return HRESULT.S_OK;
		}
	}

}
