package com.apixio.model.converter.vocabulary;

/**
 * Created by jctoledo on 12/15/16.
 */
public class CodingSystemVocabulary {
    public static enum CodingSystemOID {
        PROV_TYPE_RAPS("2.25.427343779826048612975555299345414078866");

        private final String oid;

        CodingSystemOID(String anOID) {
            this.oid = anOID;
        }
        public String toString(){
            return this.oid;
        }
    }

}
