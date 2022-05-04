package com.apixio.model.indexing;

public enum IndexableType implements IndexableNGram {

    UUID(12, 20),
    PID(12, 20),
    FN(2, 20),
    LN(2, 20),
    BD(10, 20),
    AID(6, 20),
    NT(null, null); //"no type" does not have ngram lengths

    private final Integer minNgramLength;
    private final Integer maxNgramLength;

    IndexableType(Integer minNgramLength, Integer maxNgramLength) {
        this.minNgramLength = minNgramLength;
        this.maxNgramLength = maxNgramLength;
    }

    @Override
    public Integer getMinNGramLength() {
        return minNgramLength;
    }

    @Override
    public Integer getMaxNGramLength() {
        return maxNgramLength;
    }
}
