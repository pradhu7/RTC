package com.apixio.bizlogic.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PageTextWithCoords extends PageTextWithoutCoords {
    public final UUID id;
    public final int page;
    public double top;
    public double left;
    public double height;
    public double width;

    public PageTextWithCoords(UUID id,
                              int page,
                              String text,
                              double top,
                              double left,
                              double height,
                              double width) {
        super(text);
        this.id = id;
        this.page = page;
        this.top = top;
        this.left = left;
        this.height = height;
        this.width = width;
    }

    @Override
    public String toString() {
        // Consider: Should a sort be implemented for PageText,
        // implementing the `Comparable`'s `compareTo` method will
        // need to figure out precedence order for which values to
        // sort by (e.g., `top` first, then `left`?)
        return "PageTextWithCoords{" +
                "id=" + id +
                ", page=" + page +
                ", top=" + top +
                ", left=" + left +
                ", height=" + height +
                ", width=" + width +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageTextWithCoords that = (PageTextWithCoords) o;
        return (
                id.equals(that.id)
                && page == that.page
                && text.equals(that.text)
                && top == that.top
                && left == that.left
                && height == that.height
                && width == that.width
                );
    }

    @Override
    public List<PageTextWithCoords> searchHighlights(String term, PageText[] words) {
        String cleanTerm = term.trim().toLowerCase();
        if (cleanTerm.isEmpty())
            return Collections.emptyList();
        // To match multi-word terms we split on space and then move forward until sequence is complete
        // If the sequence fails, we start over
        List<String> termParts = Arrays
                .stream(cleanTerm.split("\\W"))
                .filter(DocumentLogic.nonEmptyString)
                .collect(Collectors.toList());
        List<PageTextWithCoords> currentTermSequence = new ArrayList<>();
        List<PageTextWithCoords> matchingWords = new ArrayList<>();
        List<PageTextWithCoords> splitPageWords = Arrays
                .stream(words)
                .flatMap(word -> {
                    PageTextWithCoords coords = (PageTextWithCoords) word;
                    List<String> splitWords = Arrays.asList(coords.text.split("\\W"));
                    return splitWords
                            .stream()
                            .filter(DocumentLogic.nonEmptyString)
                            .map(subWord ->
                                    new PageTextWithCoords(
                                            coords.id,
                                            coords.page,
                                            subWord,
                                            coords.top,
                                            coords.left,
                                            coords.height,
                                            coords.width
                                    ));
                })
                .collect(Collectors.toList());
        for (PageTextWithCoords pageWord : splitPageWords) {
            String cleanWord = pageWord.text.toLowerCase();
            String cleanTermPart = termParts.get(currentTermSequence.size());
            boolean finalWord = currentTermSequence.size() == termParts.size() - 1;
            // If this word matches the correct portion of the term, add it to the current sequence
            if (cleanWord.equals(cleanTermPart)) {
                currentTermSequence.add(pageWord);
            }
            // If we were looking for a later part, but this matches the first part, add to a new sequence
            // (we can assume first part must be an exact match)
            else if (!currentTermSequence.isEmpty() && cleanWord.equals(termParts.get(0))) {
                currentTermSequence = new ArrayList<>(Collections.singleton(pageWord));
            }
            // if not then reset the sequence
            else {
                currentTermSequence = new ArrayList<>();
            }

            // Now check if we need to close the sequence
            if (currentTermSequence.size() == termParts.size()) {
                // The coordinates for the combined term need to be the min or max of each dimension
                String fullText = currentTermSequence.stream().map(sequencePart -> sequencePart.text).collect(Collectors.joining(" "));
                Double minX = currentTermSequence.stream().map(sequencePart -> sequencePart.left).reduce(Math::min).get();
                Double maxX = currentTermSequence.stream().map(sequencePart -> sequencePart.left + sequencePart.width).reduce(Math::max).get();
                Double minY = currentTermSequence.stream().map(sequencePart -> sequencePart.top).reduce(Math::min).get();
                Double maxY = currentTermSequence.stream().map(sequencePart -> sequencePart.top + sequencePart.height).reduce(Math::max).get();
                matchingWords.add(new PageTextWithCoords(id, page,
                        fullText,
                        minY,
                        minX,
                        (maxY - minY),
                        (maxX - minX)
                ));
                // And of course, reset the current sequence
                currentTermSequence = new ArrayList<>();
            }
        }
        return matchingWords;
    }
}
