package net.sourceforge.docfetcher.model.parse;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import net.sourceforge.docfetcher.enums.Msg;

/**
 * <p>Based on ID3 specifications in http://id3.org
 * 
 * <p>Currently only ID3v2.4.0 is parsed here.
 * 
 * @author Paulos Siahu
 */
public class MP3Parser extends StreamParser {

	private static final Collection<String> extensions = Arrays.asList(
			"mp3");

	private static final Collection<String> types = Arrays.asList(
			"audio/mpeg");

	private static HashSet<String> tagIDs;

	
	public MP3Parser() {
		tagIDs = new HashSet<String>();
		tagIDs.add("TALB");
		tagIDs.add("TIT2");
		tagIDs.add("TIT3s");
	}

	protected String extract(InputStream in, boolean forViewing) throws IOException, ParseException {
		StringBuffer sb = new StringBuffer();
		DataInputStream raf = new DataInputStream(in);
		
		/*
		 * Check if the file starts with the ID3 identifier.
		 */
		byte[] data = new byte[10];
		long pos = raf.read(data);
		if (Arrays.equals(Arrays.copyOfRange(data, 0, 3), new byte[] {0x49, 0x44, 0x33}) == false) { // "ID3"
			return sb.toString();
		}
		long size = 0;
		for (int i=6; i<10; i++) {
			size = (size << 7) + data[i];  // synchsafe integer only uses 7 bits of each byte.
		}
		
		while (pos < size) {
			pos += raf.read(data);
			byte[] id = Arrays.copyOfRange(data, 0, 4);
			if (Arrays.equals(id, new byte[] {0x0, 0x0, 0x0, 0x0})) { // End
				break;
			}
			int textlength = (data[4] << 24) | (data[5] << 16) | (data[6] << 8) | (data[7]);
			String tagID = new String(id);
			if (tagIDs.contains(tagID)) {
				byte[] text = new byte[textlength-1];
				raf.readByte();
				pos++;
				pos += raf.read(text);
				sb.append((forViewing ? tagID+"=" : "") + new String(text) + "\n");
			} else {
				raf.skipBytes(textlength);
			}
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
		return Msg.filetype_mp3.get();
	}

}
