package com.apixio.bizlogic.document;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PageTextWithoutCoords implements PageText {
    public String text;

    public PageTextWithoutCoords(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageTextWithoutCoords that = (PageTextWithoutCoords) o;
        return text.equals(that.text);
    }

    @Override
    public List<PageTextWithCoords> searchHighlights(String term, PageText[] words) {
        return Collections.emptyList();
    }

    @Override
    public String searchSnippets(String term, PageText[] words) {
        String fullText = Arrays.stream((PageTextWithoutCoords[]) words).map(pageText -> pageText.text).collect(Collectors.joining(" "));

        String prefix = " <span class=\"highlight\">";
        String suffix = "</span> ";
        int snippetPadding = 30;
        String joinChars = "...";

        // This method finds our term either at start or preceeded by a non-alphanumeric character to handle whitespace, bullets, etc.
        // it then computes whole surrounding words for matching terms. Should we include the non-alphanumeric character in the output??
        Pattern termRegex = Pattern.compile("(?i)(^|[^a-zA-Z0-9])(" + term + ")");
        Pattern whitespaceSeparatedRegex = Pattern.compile("\\s(.+)\\s");

        return eagerMatchResultStream(termRegex.matcher(fullText)).map(termMatch -> {
                    int preTextStart = (termMatch.start() >= snippetPadding) ? termMatch.start() - snippetPadding : 0;
                    String preText = eagerMatchResultStream(whitespaceSeparatedRegex.matcher(fullText.substring(preTextStart, termMatch.start()))).findFirst().map(MatchResult::group).orElse("").trim();
                    int postTextEnd = (termMatch.end(2) + snippetPadding < fullText.length()) ? termMatch.end(2) + snippetPadding : fullText.length();
                    String postText = eagerMatchResultStream(whitespaceSeparatedRegex.matcher(fullText.substring(termMatch.end(), postTextEnd))).findFirst().map(MatchResult::group).orElse("").trim();
                    return preText + prefix + termMatch.group(2) + suffix + postText;
                }
        ).collect(Collectors.joining(joinChars));
    }

    private Stream<MatchResult> eagerMatchResultStream(Matcher m) {
        // A quick and dirty one-liner so I can transliterate `apx-documentsearch` code more directly.
        return m.find() ? Stream.concat(Stream.of(m.toMatchResult()), eagerMatchResultStream(m)) : Stream.empty();
    }
}
