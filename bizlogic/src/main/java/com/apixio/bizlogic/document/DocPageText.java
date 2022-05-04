package com.apixio.bizlogic.document;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DocPageText<T extends PageText> {
    T[] text;

    public DocPageText(T[] text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocPageText<?> that = (DocPageText<?>) o;
        if (text.length != that.text.length)
            return false;
        for (int i = 0; i < text.length; i++) {
            if (!text[i].equals(that.text[i]))
                return false;
        }
        return true;
    }

    public List<PageTextWithCoords> searchHighlights(String term) {
        return text[0].searchHighlights(term, text);
    }

    public List<PageTextWithCoords> searchHighlights(Collection<String> terms) {
        return text[0].searchHighlights(terms, text);
    }

    public <T> Map<T, List<PageTextWithCoords>> searchHighlights(Map<T, Collection<String>> termsMap) {
        return text[0].searchHighlights(termsMap, text);
    }

    public String searchSnippets(String term) {
        return text[0].searchSnippets(term, text);
    }

    public List<String> searchSnippets(Collection<String> terms) {
        return text[0].searchSnippets(terms, text);
    }

    public <T> Map<T, List<String>> searchSnippets(Map<T, Collection<String>> termsMap) {
        return text[0].searchSnippets(termsMap, text);
    }
}
