package com.apixio.signals;


import com.apixio.ensemble.ifc.DocCacheElement;
import com.apixio.ensemble.ifc.PageWindow;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Patient;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.classification.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.apixio.jobs.util.ApixioUuidUtils.toDocumentXUuid;
import static com.apixio.jobs.util.ApixioUuidUtils.toPatientXUuid;

//
//  !!! WARN !!!
//  Please make sure to use classes from com.apixio.ensemble ONLY.
//  Do not use classes from com.apixio.sdao
//


//
// this class is not expected to deal with generation of doc cache elements.
//
public class PageWindowUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageWindowUtils.class);

    public static final int WINDOW_LOW_LIMIT  = 2;
    public static final int WINDOW_HIGH_LIMIT = 2;

    /**
     * Given a page window, creates new page windows
     *
     * @param fullDocPageWindow
     * @return
     */
    public static Iterator<Pair<Integer, PageWindow>> generatePageWindows(PipelinePageWindow fullDocPageWindow)
    {
        return generatePageWindowsLoHi(fullDocPageWindow, WINDOW_LOW_LIMIT, WINDOW_HIGH_LIMIT);
    }

    public static PipelinePageWindow generateFullDocumentPageWindow(Patient apo, Document d, Iterable<DocCacheElement> pages)
    {
        PipelinePageWindow pw                = new PipelinePageWindow();
        final boolean      fullDocPageWindow = true;
        pw.setPages(pages, fullDocPageWindow);
        pw.setTitle(d.getDocumentTitle());
        pw.setDocumentUuid(toDocumentXUuid(d.getInternalUUID()));
        pw.setPatientUuid(toPatientXUuid(apo.getPatientId()));
        return pw;
    }

    /**
     * Usually whole document page window is provided as input to this function
     *
     * @return
     */
    public static Iterator<Pair<Integer, PageWindow>> generatePerPagePageWindows(PipelinePageWindow sourcePageWindow)
    {

        // convert to this map so that we can use the iterator below

        Map<Integer, Map<String, Object>> map = new HashMap<>();
        for (DocCacheElement element : sourcePageWindow.getPages())
        {
            List<DocCacheElement> docCacheElements = new ArrayList<>();
            Map<String, Object>   infoMap          = new HashMap<>();
            infoMap.put("docCacheElements", docCacheElements);

            docCacheElements.add(element);
            map.put(element.getPageNumber(), infoMap);
        }

        return new PageWindowIter(sourcePageWindow, map);
    }


    @InterfaceAudience.Private
    /**
     * Given a full doc page window, generates the various other page windows
     */
    public static Iterator<Pair<Integer, PageWindow>> generatePageWindowsLoHi(PipelinePageWindow fullDocPageWindow, int lowerLimit, int higherLimit)
    {
        Map<Integer, Map<String, Object>> bucketizer = new HashMap<>();

        // this list must be sorted.
        List<DocCacheElement> docCacheElements = fullDocPageWindow.getPages();
        List<Integer> pageNumbers =
                docCacheElements.stream().map(DocCacheElement::getPageNumber).collect(Collectors.toList());
        int maxPageNum = pageNumbers.stream().max(Integer::compare).orElse(0);
        int minPageNum = pageNumbers.stream().min(Integer::compare).orElse(0);

        for (DocCacheElement elem : docCacheElements)
        {
            final int centroid  = elem.getPageNumber();
            int       sp        = (centroid - lowerLimit);
            final int startPage = sp < minPageNum ? minPageNum : sp;

            int       ep      = centroid + higherLimit;
            final int endPage = ep > maxPageNum ? maxPageNum : ep;

            List<DocCacheElement> pwList = new ArrayList<>();
            docCacheElements.forEach(e -> {
                if (e.getPageNumber() >= startPage && e.getPageNumber() <= endPage)
                {
                    pwList.add(e);
                }
            });

            Map<String, Object> map = new HashMap<>();
            map.put("centroid", centroid);
            map.put("start", startPage);
            map.put("end", endPage);
            map.put("docCacheElements", pwList);

            bucketizer.put(centroid, map);
        }

        return new PageWindowIter(fullDocPageWindow, bucketizer);

    }

}

@InterfaceAudience.Private
class PageWindowIter implements Iterator<Pair<Integer, PageWindow>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageWindowIter.class);

    private final PageWindow                        sourcePageWindow;
    private final Map<Integer, Map<String, Object>> map;
    private final Iterator<Integer>                 it;

    public PageWindowIter(PageWindow sourcePageWindow, Map<Integer, Map<String, Object>> map)
    {
        this.sourcePageWindow = sourcePageWindow;
        this.map = map;
        this.it = map.keySet().iterator();
    }

    @Override
    public boolean hasNext()
    {
        return it.hasNext();
    }

    @Override
    public Pair<Integer, PageWindow> next()
    {
        if (!hasNext())
        {
            throw new RuntimeException("no more page windows");
        }

        Integer               key     = it.next();
        Map<String, Object>   infoMap = map.get(key);
        List<DocCacheElement> pages   = (List<DocCacheElement>) infoMap.get("docCacheElements");

        PipelinePageWindow pw = new PipelinePageWindow();

        if (infoMap.containsKey("centroid"))
        {
            int centroid  = (Integer) infoMap.get("centroid");
            int startPage = (Integer) infoMap.get("start");
            int endPage   = (Integer) infoMap.get("end");


            List<Integer> pageNumbers = pages.stream().map(DocCacheElement::getPageNumber).collect(Collectors.toList());
            LOGGER.debug("Creating PW for {} with page numbers: {} centroid: {} start page: {} end page: {}",
                    key,
                    pageNumbers,
                    centroid,
                    startPage,
                    endPage);

            pw.setCentroid(centroid);
            pw.setStartPage(startPage);
            pw.setEndPage(endPage);

            //LOGGER.debug("Returning PW: {}", pw);
        }

        if (infoMap.containsKey("centroid")) {  // this is a centroid based pw.
            pw.setCentroidPageWindow(true);
        }

        // TODO: setPages relies on a bunch of data to be available. This can be improved.
        pw.setPages(pages, false); // because we're creating page windows of fixed length here.
        pw.setPatientUuid(sourcePageWindow.getPatientId());
        pw.setDocumentUuid(sourcePageWindow.getDocumentId());
        pw.setTitle(sourcePageWindow.getTitle());

        return new ImmutablePair<>(key, pw);
    }
}