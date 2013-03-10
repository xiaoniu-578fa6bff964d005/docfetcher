package net.sourceforge.docfetcher.model.parse;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.annotations.NotNull;

/**
 * <p>Based on FLAC specifications in http://flac.sourceforge.net/format.html#metadata_block_vorbis_comment
 * 
 * 
 * @author Paulos Siahu
 */
final class FLACParser extends StreamParser {

	private static final Collection<String> extensions = Arrays.asList(
			"flac");

	private static final Collection<String> types = Arrays.asList(
			"audio/flac");

	
	private static long[] readMetadataBlock(byte[] data) {
		long last = data[0] & 0x80;
		long type = data[0] & 0x7F;
		long size = 0;
		for (int i=1; i<4; i++) {
			size = (size << 8) + data[i];
		}
		return new long[] {last, type, size};
	}

	
	@NotNull
	private static String extract(@NotNull InputStream in, boolean forViewing)
			throws IOException, ParseException {
		StringBuffer sb = new StringBuffer();
		DataInputStream raf = new DataInputStream(in);
		
		/*
		 * Check if the file starts with the FLAC identifier.
		 */
		byte[] data = new byte[4];
		raf.read(data);
		if (Arrays.equals(Arrays.copyOfRange(data, 0, 4), new byte[] {0x66, 0x4C, 0x61, 0x43}) == false) { // "fLaC"
			return sb.toString();
		}
		
		/*
		 * Loop through each metadata block until METADATA BLOCK VORBIS COMMENT IS FOUND
		 */
		long[] typesize = null;
		for (int i = 0; i < 100; i++) { // 100 is a safe-guard number to prevent an infinite loop due to corrupted data stream
			data = new byte[4];
			raf.read(data);
			typesize = readMetadataBlock(data);
			long last = typesize[0];
			long type = typesize[1];
			long size = typesize[2];
			
			if (type != 0x04) {
				raf.skip(size);
			} else {
				break;
			}
			if (last != 0) {
				break;
			}
		}

		// vendor_length
		data = new byte[4];
		raf.read(data);
		long size = 0;
		for(int i=0; i<4; i++) {
			size += (data[i] << (i*8));
		}
		
		// vendor string
		raf.skip(size);
		
		// user_comment_list_length
		data = new byte[4];
		raf.read(data);
		long commentCount = 0;
		for(int i=0; i<4; i++) {
			commentCount += (data[i] << (i*8));
		}
		
		// 100 is a safe-guard number to prevent an infinite loop due to corrupted data stream
		commentCount = Math.min(commentCount, 100);
		
		for (int i = 0; i < commentCount; i++) {
			
			// length
			data = new byte[4];
			raf.read(data);
			size = 0;
			for(int j=0; j<4; j++) {
				size += (data[j] << (j*8));
			}
			
			data = new byte[(int)size];
			raf.read(data);
			String entry = new String(data);
			if (forViewing == false) {
				String[] cells = entry.split("=");
				if (cells != null && cells.length >= 2) {
					entry = cells[1];
				}
			}
			sb.append(entry + "\n");
		}

		return sb.toString();
	}
	
	@Override
	protected ParseResult parse(InputStream in, ParseContext context)
			throws ParseException {
		String text = "";
		try {
			text = extract(in, false);
		} catch (Exception e) {
			throw new ParseException(e);
		}
		return new ParseResult(text);
	}
	
	@Override
	protected String renderText(InputStream in, String filename)
			throws ParseException {
		String text = "";
		try {
			text = extract(in, true);
		} catch (Exception e) {
			throw new ParseException(e);
		}
		return text;
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
		return Msg.filetype_flac.get();
	}

}
