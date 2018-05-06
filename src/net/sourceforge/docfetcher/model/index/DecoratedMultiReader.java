package net.sourceforge.docfetcher.model.index;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;

/**
 * Created by huzhengmian on 2018/5/7.
 */
public class DecoratedMultiReader extends MultiReader {
    public DecoratedMultiReader(IndexReader... subReaders) {
        super(subReaders);
    }

    public DecoratedMultiReader(IndexReader[] subReaders, boolean closeSubReaders) {
        super(subReaders,closeSubReaders);
    }

    public final int decoratedReaderIndex(int docID) {
        return super.readerIndex(docID);
    }
}
