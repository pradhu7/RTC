package com.apixio.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.ghost4j.Ghostscript;
import org.ghost4j.GhostscriptRevision;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.ghost4j.document.PDFDocument;

@Ignore("OCR Integration Test")
public class ImageConvertToolsTest {

    @Test
    public void convertPdfPage2Image() throws Exception {
        GhostscriptRevision gsr = Ghostscript.getRevision();
        String gsVersion = gsr.getNumber();
        String gsDate = "" + gsr.getRevisionDate().toInstant().getEpochSecond();
        System.out.println("Using gsVersion: " + gsVersion);
        System.out.println("Using gsDate: " + gsDate);
        InputStream stream = getClass().getResourceAsStream("/test.pdf");
        PDFDocument document = new PDFDocument();
        document.load(stream);
        BufferedImage actual = ImageConvertTools.convertPdfPage2Image(document, 0, Integer.parseInt(ImageConvertTools.DEFAULT_OCR_RESOLUTION), Executors.newSingleThreadExecutor(), 180, TimeUnit.SECONDS);
        ImageIO.write(actual, "bmp", new File("test_scaled_actual_" + gsVersion + "_" + gsDate + ".bmp"));
        String refbmp = "/test_scaled_" + gsVersion + "_" + gsDate + ".bmp";
        System.out.println("Using reference bitmap:" + refbmp);
        BufferedImage expected = ImageIO.read(getClass().getResourceAsStream(refbmp));
        Assert.assertEquals(3300, actual.getHeight());
        Assert.assertEquals(2550, actual.getWidth());
        Assert.assertEquals(3300, expected.getHeight());
        Assert.assertEquals(2550, expected.getWidth());
        for (int x = 0; x < expected.getRaster().getWidth(); x++) {
            for (int y = 0; y < expected.getRaster().getHeight(); y++) {
                int[] valExpected = expected.getRaster().getPixel(x, y, new int[3]);
                int[] valActual = actual.getRaster().getPixel(x, y, new int[3]);
                Assert.assertArrayEquals(valExpected, valActual);
            }
        }
    }

    @Test
    public void convertPdfPage2ImageNoScaling() throws Exception {
        GhostscriptRevision gsr = Ghostscript.getRevision();
        String gsVersion = gsr.getNumber();
        String gsDate = "" + gsr.getRevisionDate().toInstant().getEpochSecond();
        System.out.println("Using gsVersion: " + gsVersion);
        System.out.println("Using gsDate: " + gsDate);
        InputStream stream = getClass().getResourceAsStream("/test.pdf");
        PDFDocument document = new PDFDocument();
        document.load(stream);
        BufferedImage actual = ImageConvertTools.convertPdfPage2ImageNoScaling(document, 0, Executors.newSingleThreadExecutor(), 180, TimeUnit.SECONDS);
        ImageIO.write(actual, "bmp", new File("test_unscaled_actual_"  + gsVersion + "_" + gsDate + ".bmp"));
        String refbmp = "/test_unscaled_" + gsVersion + "_" + gsDate + ".bmp";
        System.out.println("Using reference bitmap:" + refbmp);
        BufferedImage expected = ImageIO.read(getClass().getResourceAsStream(refbmp));
        Assert.assertEquals(825, actual.getHeight());
        Assert.assertEquals(638, actual.getWidth());
        Assert.assertEquals(825, expected.getHeight());
        Assert.assertEquals(638, expected.getWidth());
        for (int x = 0; x < expected.getRaster().getWidth(); x++) {
            for (int y = 0; y < expected.getRaster().getHeight(); y++) {
                int[] valExpected = expected.getRaster().getPixel(x, y, new int[3]);
                int[] valActual = actual.getRaster().getPixel(x, y, new int[3]);
                Assert.assertArrayEquals(valExpected, valActual);
            }
        }
    }

}
