package com.apixio.cerveau.model.util;

import com.apixio.XUUID;

import java.util.List;

public class DocumentSignalDateCoverageOutput {

    private List<DocumentDateInformation> documentDateList;

    public DocumentSignalDateCoverageOutput(List<DocumentDateInformation> documentDateList) {
        this.documentDateList = documentDateList;
    }

    public List<DocumentDateInformation> getDocumentDateList() {
        return documentDateList;
    }

    static public class DocumentDateInformation {
        List<String> documentDates;
        private XUUID documentId;
        private boolean isDateAssumed;

        public DocumentDateInformation(XUUID documentId, boolean isDateAssumed, List<String> documentDates) {
            this.documentId = documentId;
            this.isDateAssumed = isDateAssumed;
            this.documentDates = documentDates;
        }

        public XUUID getDocumentId() {
            return documentId;
        }

        public boolean isDateAssumed() {
            return isDateAssumed;
        }

        public List<String> getDocumentDates() {
            return documentDates;
        }
    }
}
