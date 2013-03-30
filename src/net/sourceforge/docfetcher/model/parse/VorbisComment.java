package net.sourceforge.docfetcher.model.parse;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * This class is created because several media formats
 * use the Vorbis Comment specification for metadata.
 * 
 * @author psiahu
 */
public class VorbisComment {

	long bigToLittleEndianInteger(long bigEndian) {
		long byte0 = (bigEndian >> 24) & 0xFF;
		long byte1 = (bigEndian >> 16) & 0xFF;
		long byte2 = (bigEndian >> 8) & 0xFF;
		long byte3 = bigEndian & 0xFF;
		long littleEndian = byte0
				| (byte1 << 8)
				| (byte2 << 16)
				| (byte3 << 24);
		return littleEndian;
	}
	
	
	void parse(DataInputStream dis, StringBuffer sb, boolean forViewing) throws IOException {
		// vendor_length
		long size = bigToLittleEndianInteger(dis.readInt());
		
		// vendor string
		dis.skipBytes((int)size);
		
		// user_comment_list_length
		long commentCount = bigToLittleEndianInteger(dis.readInt());
		
		// 100 is a safe-guard number to prevent an infinite loop due to corrupted data stream
		commentCount = Math.min(commentCount, 100);
		
		for (int i = 0; i < commentCount; i++) {
			
			// length
			long commentSize = bigToLittleEndianInteger(dis.readInt());
			
			// This is another safe-guard against data corruption
			// If commentSize is too big we can run out of memory when reading comment to String
			if (commentSize > 100000) {
				dis.skipBytes((int)commentSize);
				continue;
			}
			
			byte[] comment = new byte[(int)commentSize];
			dis.readFully(comment);
			String entry = new String(comment);
			if (forViewing == false) {
				String[] cells = entry.split("=");
				if (cells != null && cells.length >= 2) {
					entry = cells[1];
				}
			}
			sb.append(entry + "\n");
		}
	}
}
