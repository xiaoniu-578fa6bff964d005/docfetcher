package net.sourceforge.docfetcher.model.parse;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;

/**
 * @author Nam-Quang Tran
 */
final class MP3Parser extends StreamParser {

	private static final Collection<String> extensions = Arrays.asList(
			"mp3");

	private static final Collection<String> types = Arrays.asList(
			"audio/mpeg");

	@Override
	protected ParseResult parse(InputStream in, ParseContext context)
			throws ParseException {
		try {
			return new ParseResult(extract(in, false));
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
	
	@Override
	protected String renderText(InputStream in, String filename)
			throws ParseException {
		try {
			return extract(in, true);
		} catch (Exception e) {
			throw new ParseException(e);
		}
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
	
	@NotNull
	private static String extract(@NotNull InputStream in, boolean forViewing)
			throws Exception {
		BodyContentHandler bodyHandler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		new Mp3Parser().parse(in, bodyHandler, metadata, ParseService.tikaContext());
		return bodyHandler.toString();
	}
	
}
