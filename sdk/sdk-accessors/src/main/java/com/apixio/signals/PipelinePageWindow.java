package com.apixio.signals;

import com.apixio.XUUID;
import com.apixio.ensemble.ifc.*;
import com.apixio.ensemble.impl.common.SmartXUUID;
import com.apixio.ensemble.impl.source.Location;
import com.apixio.ensemble.impl.source.PageWindowSource;
import com.apixio.jobs.util.NoPageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

//
//  !!! WARN !!!
//  Please make sure to use classes from com.apixio.ensemble ONLY.
//  Do not use classes from com.apixio.sdao
//


public class PipelinePageWindow implements PageWindow {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelinePageWindow.class);

    private Source                source;
    private List<DocCacheElement> sortedPages; // WARN: must be sorted before store
    private String                title;
    private int[]                 pageNumbers;
    private int                   startPage;
    private int                   endPage;
    private int                   centroid;

    private XUUID documentUuid;
    private XUUID patientUuid;

    private boolean fullDocPageWindow  = false;
    private boolean centroidPageWindow = false;


    @Override
    public String toString()
    {
        String content = getContent();
        int    len     = content == null ? -1 : content.length();

        String sourceClass = source == null ? "null" : source.getClass().getCanonicalName();

        return "PipelinePageWindow{" +
                "contentLen=" + len +
                ", sourceClass=" + sourceClass +
                ", source=" + source +
                ", sortedPages=" + sortedPages +
                ", title='" + title + '\'' +
                ", documentUuid=" + documentUuid +
                ", patientUuid=" + patientUuid +
                ", fullDocPageWindow=" + fullDocPageWindow +
                '}';
    }

    public boolean isCentroidPageWindow()
    {
        return centroidPageWindow;
    }

    public void setCentroidPageWindow(boolean centroidPageWindow)
    {
        this.centroidPageWindow = centroidPageWindow;
    }

    public void setStartPage(int startPage)
    {
        this.startPage = startPage;
    }

    public void setEndPage(int endPage)
    {
        this.endPage = endPage;
    }

    public int getCentroid()
    {
        return centroid;
    }

    public void setCentroid(int centroid)
    {
        this.centroid = centroid;
    }

    public int getStartPage()
    {
        return startPage;
    }

    public int getEndPage()
    {
        return endPage;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public boolean containsPage(int page)
    {
        return Arrays.binarySearch(getPageNumbers(), page) >= 0;
    }

    public boolean isFullDocPageWindow()
    {
        return fullDocPageWindow;
    }

    public void setPages(Iterable<DocCacheElement> pages, boolean fullDocPageWindow)
    {
        List<DocCacheElement> list = new ArrayList<>();
        pages.forEach(list::add);
        this.setPages(list, fullDocPageWindow);
    }

    public void setPages(List<DocCacheElement> pages, boolean fullDocPageWindow)
    {
        this.fullDocPageWindow = fullDocPageWindow;

        if (pages == null || pages.size() == 0)
        {
            throw new NoPageException("null page object or page object with zero length");
        }

        // sort first based on the page
        pages.sort(new DocCacheElemPageSorter());

        this.sortedPages = pages;
        this.pageNumbers = new int[pages.size()];
        for (int i = 0; i < pages.size(); i++)
        {
            this.pageNumbers[i] = pages.get(i).getPageNumber();
        }

        if (documentUuid == null)
        {
            setDocumentUuid(sortedPages.get(0).getDocumentId());
        }

        if (patientUuid == null)
        {
            setPatientUuid(sortedPages.get(0).getPatientId());
        }


        if (pageNumbers.length > 1)
        {
            setCentroidPageWindow(true);
        }

        // what sort of source it this?
        if (this.fullDocPageWindow)
        {
            DocumentSource wholeDocSource = new com.apixio.ensemble.impl.source.DocumentSource(
                    SmartXUUID.apply(documentUuid),
                    SmartXUUID.apply(patientUuid),
                    this.pageNumbers.length, // how many pages in the document
                    Location.Default());

            setSource(wholeDocSource);

        } else if (isCentroidPageWindow() || pageNumbers.length > 1)
        {
            // Pre condition: if this is centroid page, getCentroid() should work.
            PageWindowSource pageWindowSource = new com.apixio.ensemble.impl.source.PageWindowSource(
                    SmartXUUID.apply(documentUuid),
                    SmartXUUID.apply(patientUuid),
                    this.getCentroid(),
                    this.getStartPage(),
                    this.getEndPage(),
                    Location.Default());

            setSource(pageWindowSource);
        } else if (pageNumbers.length == 1)
        {
            PageSource pageSource = new com.apixio.ensemble.impl.source.PageSource(
                    SmartXUUID.apply(documentUuid),
                    SmartXUUID.apply(patientUuid),
                    this.pageNumbers[0],
                    Location.Default()
            );

            setSource(pageSource);
        }

    }

    public void setSource(Source source)
    {
        this.source = source;
    }


    public void setPatientUuid(XUUID patientUuid)
    {
        this.patientUuid = patientUuid;
    }

    public void setDocumentUuid(XUUID documentUuid)
    {
        this.documentUuid = documentUuid;
    }

    @Override
    public List<DocCacheElement> getPages()
    {
        return sortedPages;
    }

    @Override
    public Source getSource()
    {
        return source;
    }

    @Override
    public int[] getPageNumbers()
    {
        return this.pageNumbers;
    }

    @Override
    public int getPageCentroid()
    {
        Source source = getSource();
        if (source instanceof PageWindowSource)
        {
            return ((PageWindowSource) source).getCentroid();
        }

        if (source instanceof PageSource)
        {
            return ((PageSource) source).getPage();
        }

        if (source instanceof DocumentSource)
        {
            return ((DocumentSource) source).getNumPages();
        }

        throw new RuntimeException("Cannot determine page centroid for source: " + source);
    }

    /**
     * Get the concatenated contents of the page window.
     *
     * @return A string containing the concatenated contents.
     */
    @Override
    public String getContent()
    {
        final StringBuilder sb = new StringBuilder();
        if (sortedPages == null)
        {
            LOGGER.debug("Empty sortedPages !!");
            return "";
        }


        for (DocCacheElement e : sortedPages)
        {
            if (e != null)
            {
                sb.append(e.getContent());
                sb.append("\n");
            } else
            {
                LOGGER.warn("empty doc cache elem: in {}", getDocumentId());
            }
        }

        return sb.toString();
    }


    @Override
    /*
      Get the title string for the document as a whole.
      @return A string containing the title.
     */
    public String getTitle()
    {
        return title;
    }

    @Override
    /**
     * Not implemented
     */
    public List<String> getTags()
    {
        return null;
    }

    @Override
    public XUUID getDocumentId()
    {
        return documentUuid;
    }

    @Override
    public XUUID getPatientId()
    {
        return patientUuid;
    }

}

class DocCacheElemPageSorter implements Comparator<DocCacheElement> {
    /**
     * return a doc cache element with lower page number
     *
     * @param o1
     * @param o2
     * @return -1, 0 or 1
     */
    @SuppressWarnings("JavaDoc")
    @Override
    public int compare(DocCacheElement o1, DocCacheElement o2)
    {
        return Integer.compare(o1.getPageNumber(), o2.getPageNumber());
    }
}

