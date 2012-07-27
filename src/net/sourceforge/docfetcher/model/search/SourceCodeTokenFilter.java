package net.sourceforge.docfetcher.model.search;

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

/**
 *
 */
public final class SourceCodeTokenFilter extends TokenFilter {
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	public SourceCodeTokenFilter(Version matchVersion, TokenStream in) {
		super(in);
	}

	@Override
	public final boolean incrementToken() throws IOException {
		if (input.incrementToken()) {
			final char[] buffer = termAtt.buffer();
			final int length = termAtt.length();
			for (int i = 0; i < length;) {
				if ((buffer[i] == '.') || (buffer[i] == '='))
					i += Character.toChars(' ', buffer, i);
				else
					i += Character.toChars(buffer[i], buffer, i);
			}
			return true;
		} else
			return false;
	}
}
