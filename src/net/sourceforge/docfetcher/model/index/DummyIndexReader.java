package net.sourceforge.docfetcher.model.index;

import org.apache.lucene.index.*;
import org.apache.lucene.util.Bits;

import java.io.IOException;

/**
 * Created by huzhengmian on 2018/5/6.
 */
public class DummyIndexReader extends AtomicReader {
    @Override
    public Fields fields() throws IOException {
        return null;
    }

    @Override
    public DocValues docValues(String s) throws IOException {
        return null;
    }

    @Override
    public DocValues normValues(String s) throws IOException {
        return null;
    }

    @Override
    public FieldInfos getFieldInfos() {
        return null;
    }

    @Override
    public Bits getLiveDocs() {
        return null;
    }

    @Override
    public Fields getTermVectors(int i) throws IOException {
        return null;
    }

    @Override
    public int numDocs() {
        return 0;
    }

    @Override
    public int maxDoc() {
        return 0;
    }

    @Override
    public void document(int i, StoredFieldVisitor storedFieldVisitor) throws IOException {

    }

    @Override
    public boolean hasDeletions() {
        return false;
    }

    @Override
    protected void doClose() throws IOException {

    }
}
