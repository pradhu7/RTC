package com.apixio.ocr;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class TesseractManagerUtil {
    private static ThreadLocal<Tesseract> instance =
        ThreadLocal.withInitial(TesseractManagerUtil::createTesseractInstance);

    public static Tesseract createTesseractInstance() {
        return createTesseractInstance(null);
    }

    /**
     * returns an instance suitable for use in Apixio OCR
     */
    public static Tesseract createTesseractInstance(String datapath) {
        String tessdataPrefix = datapath;
        Tesseract instance = new Tesseract();

        if (tessdataPrefix == null || tessdataPrefix.isEmpty()) {
            tessdataPrefix = System.getenv("TESSDATA_PREFIX");
        }

        if (tessdataPrefix != null)
            instance.setDatapath(tessdataPrefix);

        instance.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO_OSD);
        instance.setLanguage("eng");
        instance.setHocr(true);

        // black list. add ligatures and diacritics if they need to be disabled.
        instance.setTessVariable("tessedit_char_blacklist", "");

        return instance;
    }

    public static String doOCR(final BufferedImage imageI) throws TesseractException {
        return doOCR(imageI, null);
    }

    public static String doOCR(final BufferedImage imageI, final Rectangle rectangle) throws TesseractException {
        return instance.get().doOCR(imageI, rectangle);
    }

    /* Hack to access Tess4j's `protected` API which holds the shared
     * library's version.
     */
    private static class TesseractInspector extends Tesseract {
        public TesseractInspector() {
            super();
            init();
        }
        public String getVersion() {
            return this.getAPI().TessVersion();
        }
    }

    public static String getTessVersion() {
        TesseractInspector t = new TesseractInspector();
        return t.getVersion();
    }
    /* End hack */
}
