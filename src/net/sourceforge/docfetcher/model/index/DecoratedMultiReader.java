package net.sourceforge.docfetcher.model.index;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;

import java.io.IOException;

/**
 * Created by huzhengmian on 2018/5/7.
 */
public class DecoratedMultiReader extends MultiReader {
    public DecoratedMultiReader(IndexReader... subReaders) throws IOException {
        super(subReaders);
    }

    public DecoratedMultiReader(IndexReader[] subReaders, boolean closeSubReaders) throws IOException {
        super(subReaders,closeSubReaders);
    }

    public final int decoratedReaderIndex(int docID) {
        return super.readerIndex(docID);
    }
}
