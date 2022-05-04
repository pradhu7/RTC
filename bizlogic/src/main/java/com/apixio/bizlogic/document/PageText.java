package com.apixio.bizlogic.document;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface PageText {
    List<PageTextWithCoords> searchHighlights(String term, PageText[] words);

    default List<PageTextWithCoords> searchHighlights(Collection<String> terms, PageText[] words) {
        return terms.stream().flatMap(term -> searchHighlights(term, words).stream()).collect(Collectors.toList());
    }

    // Okay, this naive implementation is a quaint cop-out, which
    // certainly makes it a low-hanging fruit for optimization in
    // a future where the repeated calls to the single-term search
    // becomes the bottleneck. Thankfully, we stand on the
    // shoulders of giants! A brief procrastination shows that
    // GNU's `grep` uses a variant of the "Commentz-Walter
    // algorithm"[1][2], and many other approaches for searching
    // for many patterns within a single corpus exist[3].
    //
    // [1]: https://en.wikipedia.org/wiki/Commentz-Walter_algorithm
    // [2]: https://web.archive.org/web/20171010223532/http://www.hs-albsig.de/studium/wirtschaftsinformatik/Documents/commentzwalterextab.pdf
    // [3]: https://en.wikipedia.org/wiki/String-searching_algorithm#Algorithms_using_a_finite_set_of_patterns
    default <T> Map<T, List<PageTextWithCoords>> searchHighlights(Map<T, Collection<String>> termsMap, PageText[] words) {
        return termsMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        es -> searchHighlights(es.getValue(), words)
                ));
    }

    String searchSnippets(String term, PageText[] words);

    default List<String> searchSnippets(Collection<String> terms, PageText[] words) {
        return terms.stream().map(term -> searchSnippets(term, words)).filter(DocumentLogic.nonEmptyString).collect(Collectors.toList());
    }

    default <T> Map<T, List<String>> searchSnippets(Map<T, Collection<String>> termsMap, PageText[] words) {
        return termsMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        es -> searchSnippets(es.getValue(), words)
                ));
    }
}
