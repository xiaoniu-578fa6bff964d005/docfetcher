/*******************************************************************************
 * Copyright (c) 2012 Paulos Siahu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Paulos Siahu - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.model.parse;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import net.sourceforge.docfetcher.enums.Msg;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

/**
 * @author Paulos Siahu
 */
final class ExifParser extends StreamParser {

	private static final Collection<String> extensions = Arrays.asList(
			"jpg", "jpeg");

	private static final Collection<String> types = Arrays.asList(
			"image/jpg");

	@Override
	protected ParseResult parse(InputStream in, ParseContext context)
			throws ParseException {
		StringBuilder sb = new StringBuilder();
		try {
			Metadata metadata = JpegMetadataReader.readMetadata(in);
			for (Directory dir : metadata.getDirectories()) {
				sb.append(dir.getName()).append("\n");
				for (Tag tag : dir.getTags()) {
					sb.append(tag.getDescription()).append("\n");
				}
			}
		} catch (Exception e) {
			throw new ParseException(e);
		}
		return new ParseResult(sb.toString());
	}
	
	@Override
	protected String renderText(InputStream in, String filename)
			throws ParseException {
		StringBuilder sb = new StringBuilder();
		try {
			Metadata metadata = JpegMetadataReader.readMetadata(in);
			for (Directory dir : metadata.getDirectories()) {
				sb.append("Directory = " + dir.getName() + "\n");
				for (Tag tag : dir.getTags()) {
					sb.append("  Tag " + tag.getTagName() + " = " + tag.getDescription() + "\n");
				}
			}
		} catch (Exception e) {
			throw new ParseException(e);
		}
		return sb.toString();
	}

	@Override
	protected Collection<String> getExtensions() {
		return extensions;
	}

	@Override
	protected Collection<String> getTypes() {
		return types;
	}

	@Override
	public String getTypeLabel() {
		return Msg.filetype_jpg.get();
	}

}
