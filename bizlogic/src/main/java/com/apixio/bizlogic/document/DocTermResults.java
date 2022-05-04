package com.apixio.bizlogic.document;

import java.util.List;

public class DocTermResults {
    public List<PageTextWithCoords> highlights;
    public List<String> snippets;

    public DocTermResults(List<PageTextWithCoords> highlights,
                          List<String> snippets) {
        this.highlights = highlights;
        this.snippets = snippets;
    }
}
