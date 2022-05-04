package com.apixio.ocr;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.ghost4j.document.Document;
import org.ghost4j.document.DocumentException;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.RendererException;
import org.ghost4j.renderer.SimpleRenderer;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.imageio.ImageIO;

public class ImageConvertTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageConvertTools.class);
    public static final String DEFAULT_OCR_RESOLUTION            = "300";

    private static SimpleRenderer renderer;

    public static BufferedImage convertPdfPage2Image(PDFDocument document,
                                                     int pageNo,
                                                     int resolution,
                                                     ExecutorService exec,
                                                     int limit,
                                                     TimeUnit unit) throws IOException,
                                                                           RendererException,
                                                                           DocumentException,
                                                                           InterruptedException,
                                                                           ExecutionException,
                                                                           TimeoutException
    {
        return convertPdfPage2Image(document,
                                    pageNo,
                                    resolution,
                                    exec,
                                    limit,
                                    unit,
                                    true);
    }

    public static BufferedImage convertPdfPage2Image(PDFDocument document,
                                                     int pageNo,
                                                     int resolution,
                                                     ExecutorService exec,
                                                     int limit,
                                                     TimeUnit unit,
                                                     boolean singleImageAsPage) throws IOException,
                                                                                       RendererException,
                                                                                       DocumentException,
                                                                                       InterruptedException,
                                                                                       ExecutionException,
                                                                                       TimeoutException
    {
        if (singleImageAsPage)
        {
            org.apache.pdfbox.pdmodel.PDDocument pdfDocImage = null;
            try
                {
                    // load pdf file via PDFBox
                    pdfDocImage = org.apache.pdfbox.pdmodel.PDDocument.load(new ByteArrayInputStream(document.getContent()));
                    PDPageTree pdPageTree  = pdfDocImage.getDocumentCatalog().getPages();
                    PDPage     currentPage = pdPageTree.get(pageNo);
                    double     currentW    = currentPage.getMediaBox().getWidth();
                    double     currentH    = currentPage.getMediaBox().getHeight(); // this is not being used?
                    int        imageWidth  = 0;
                    int        imageHeight = 0;

                    PDResources pdResources = currentPage.getResources();

                    // get the number of images in current page and dpi
                    int numOfImages = 0;

                    Iterable<COSName> xObectsIterable = null;
                    if (pdResources != null)
                        {
                            xObectsIterable = pdResources.getXObjectNames();
                        }


                    if (xObectsIterable != null)
                        {
                            for (COSName xObjectName : xObectsIterable)
                                {
                                    PDXObject pdxObject = pdResources.getXObject(xObjectName);
                                    if (pdxObject instanceof PDImageXObject)
                                        {
                                            PDImageXObject image = (PDImageXObject) pdxObject;
                                            if (image != null)
                                                {
                                                    imageWidth = Math.max(imageWidth, image.getWidth());
                                                    imageHeight = Math.max(imageHeight, image.getHeight());
                                                    numOfImages++;
                                                }
                                        }
                                    LOGGER.info("convertPdfPage2Image: numOfImages = " + numOfImages);
                                }

                            double dpi = imageWidth / currentW * 72.0;
                            LOGGER.info("convertPdfPage2Image: native dpi = " + dpi);

                            // if one image per page, and DPI is above 190, return raw image
                            if (numOfImages == 1 && dpi >= 190)
                                {
                                    for (COSName xObjectName : xObectsIterable)
                                        {
                                            PDXObject pdxObject = pdResources.getXObject(xObjectName);
                                            if (pdxObject instanceof PDImageXObject)
                                                {
                                                    PDImageXObject retImage = (PDImageXObject) pdxObject;
                                                    if (retImage != null && retImage.getWidth() == imageWidth && retImage.getHeight() == imageHeight)
                                                        {
                                                            LOGGER.info("convertPdfPage2Image: return raw image");
                                                            BufferedImage retBufImage = retImage.getImage();
                                                            if (retBufImage != null)
                                                                {
                                                                    return retBufImage;
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }

                } catch (Exception ex)
                {
                    LOGGER.error("Exception caught in convertPdfPage2Image while using pdfbox", ex);
                } finally
                {
                    if (pdfDocImage != null)
                        {
                            pdfDocImage.close();
                        }
                }
        }

        // else render pages as desired image type & resolution
        LOGGER.info("convertPdfPage2Image: return gs rendered image at " + resolution + "dpi");
        final PDFDocument pdfdoc = document;
        final int         page   = pageNo;
        final int         res    = resolution;
        FutureTask<BufferedImage> future = new FutureTask<>(() -> ((BufferedImage) ghostscriptRender(pdfdoc, page, page, res).get(0)));
        exec.execute(future);
        return future.get(limit, unit);

    }

    public static BufferedImage convertPdfPage2ImageNoScaling(PDFDocument document, int pageNo, ExecutorService exec, int limit, TimeUnit unit) throws IOException, RendererException, DocumentException, InterruptedException, ExecutionException, TimeoutException
    {
        final PDFDocument pdfdoc = document;
        final int         page   = pageNo;
        FutureTask<BufferedImage> future = new FutureTask<>(() -> ((BufferedImage) ghostscriptRender(pdfdoc, page, page, null).get(0)));
        exec.execute(future);
        return future.get(limit, unit);
    }

    public static synchronized List<Image> ghostscriptRender(Document d, int begin, int end, Integer res) throws RendererException, DocumentException, IOException {
        renderer = new SimpleRenderer();
        if (res != null)
            renderer.setResolution(res);
        return renderer.render(d, begin, end);
    }

    /*
     * This is a memory non-efficient hack to copy a `BufferedImage`
     * in order to address EN-7944. When an instance with `getType()`
     * equal to `0` (`TYPE_CUSTOM`) is passed to Tess4j, we get a
     * segfault.

     * However, when writing the `BufferedImage` to disk as a png, the
     * CLI tesseract had no such issue. Further, using `ImageIO.read`
     * to reconstitute back to an image and passing to Tess4j
     * encountered no segfault at all!

     * It was observed that the re-constituted `BufferedImage`'s
     * `getType()` is `5`, a `TYPE_3BYTE_BGR`. This *strongly*
     * correlates with the core dump pointing to:
     * https://github.com/tesseract-ocr/tesseract/blob/4.0.0/src/ccmain/thresholder.cpp#L95
     */
    public static BufferedImage cloneBIwithMemoryString(BufferedImage biIn) {
        final String imageFormat = "png";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(biIn, imageFormat, baos);
            LOGGER.info("Copying image of byte size: " + baos.size());
            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
                return ImageIO.read(bais);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
