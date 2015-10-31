package org.apache.tika.parser.microsoft;

import java.util.HashSet;
import java.util.Set;

import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.tika.mime.MediaType;

public class OfficeParser {
    public enum POIFSDocumentType {
        WORKBOOK("xls", MediaType.application("vnd.ms-excel")),
        OLE10_NATIVE("ole", POIFSContainerDetector.OLE10_NATIVE),
        COMP_OBJ("ole", POIFSContainerDetector.COMP_OBJ),
        WORDDOCUMENT("doc", MediaType.application("msword")),
        UNKNOWN("unknown", MediaType.application("x-tika-msoffice")),
        ENCRYPTED("ole", MediaType.application("x-tika-ooxml-protected")),
        POWERPOINT("ppt", MediaType.application("vnd.ms-powerpoint")),
        PUBLISHER("pub", MediaType.application("x-mspublisher")),
        PROJECT("mpp", MediaType.application("vnd.ms-project")),
        VISIO("vsd", MediaType.application("vnd.visio")),
        WORKS("wps", MediaType.application("vnd.ms-works")),
        XLR("xlr", MediaType.application("x-tika-msworks-spreadsheet")),
        OUTLOOK("msg", MediaType.application("vnd.ms-outlook")),
        SOLIDWORKS_PART("sldprt", MediaType.application("sldworks")),
        SOLIDWORKS_ASSEMBLY("sldasm", MediaType.application("sldworks")),
        SOLIDWORKS_DRAWING("slddrw", MediaType.application("sldworks"));

        private final String extension;
        private final MediaType type;

        POIFSDocumentType(String extension, MediaType type) {
            this.extension = extension;
            this.type = type;
        }

        public static POIFSDocumentType detectType(POIFSFileSystem fs) {
            return detectType(fs.getRoot());
        }

        public static POIFSDocumentType detectType(NPOIFSFileSystem fs) {
            return detectType(fs.getRoot());
        }

        public static POIFSDocumentType detectType(DirectoryEntry node) {
            Set<String> names = new HashSet<String>();
            for (Entry entry : node) {
                names.add(entry.getName());
            }
            MediaType type = POIFSContainerDetector.detect(names, node);
            for (POIFSDocumentType poifsType : values()) {
                if (type.equals(poifsType.type)) {
                    return poifsType;
                }
            }
            return UNKNOWN;
        }

        public String getExtension() {
            return extension;
        }

        public MediaType getType() {
            return type;
        }
    }

}