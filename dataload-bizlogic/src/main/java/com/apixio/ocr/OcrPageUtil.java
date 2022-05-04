package com.apixio.ocr;

import com.apixio.model.ocr.page.ExtractedText;
import com.apixio.model.ocr.page.ObjectFactory;
import com.apixio.model.ocr.page.Page;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OcrPageUtil {
    // diacritic :String = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜĀÁǍÀĒÉĚÈĪÍǏÌŌÓǑÒŪÚǓÙǕǗǙǛ"
    private static final String strDiacrtics = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜĀÁǍÀĒÉĚÈĪÍǏÌŌÓǑÒŪÚǓÙǕǗǙǛ";
    private static final String strDiacritcsReplace = "aaaaeeeeiiiioooouuuuuuuuAAAAEEEEIIIIOOOOUUUUUUUU";
    private static final HashMap<Character, Character> diacriticHash = new HashMap<Character, Character>();

    static {
        for (int i = 0; i < strDiacrtics.length(); ++i) {
            diacriticHash.put(strDiacrtics.charAt(i), strDiacritcsReplace.charAt(i));
        }
    }

    // ligatures: "ßæœĲĳᵫꜩꜳꜵꜷꜹꜽꝏﬀﬁﬂﬃﬄﬅﬆ"
    private static final HashMap<String, String> ligatureHash = new HashMap<String, String>();

    static {
        ligatureHash.put("ẞ", "fs");
        ligatureHash.put("ß", "fz");
        ligatureHash.put("æ", "ae");
        ligatureHash.put("œ", "oe");
        ligatureHash.put("Ĳ", "IJ");
        ligatureHash.put("ĳ", "ij");
        ligatureHash.put("ᵫ", "ue");
        ligatureHash.put("ꜩ", "tz");
        ligatureHash.put("ꜳ", "aa");
        ligatureHash.put("ꜵ", "ao");
        ligatureHash.put("ꜷ", "au");
        ligatureHash.put("ꜹ", "av");
        ligatureHash.put("ꜽ", "ay");
        ligatureHash.put("ꝏ", "oo");
        ligatureHash.put("ﬀ", "ff");
        ligatureHash.put("ﬁ", "fi");
        ligatureHash.put("ﬂ", "fl");
        ligatureHash.put("ﬃ", "ffi");
        ligatureHash.put("ﬄ", "ffl");
        ligatureHash.put("ﬅ", "ft");
        ligatureHash.put("ﬆ", "st");
    }

    private static ThreadLocal<JAXBContext> pagJaxbContext = ThreadLocal.withInitial(OcrPageUtil::createContext);

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(Page.class);
        } catch (JAXBException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static ThreadLocal<Marshaller> pageJaxbMarshaller = ThreadLocal.withInitial(OcrPageUtil::createMarshaller);

    private static Marshaller createMarshaller()  {
        Marshaller pageJaxbMarshaller = null;
        try {
            pageJaxbMarshaller = pagJaxbContext.get().createMarshaller();
            pageJaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        } catch (JAXBException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return pageJaxbMarshaller;
    }

    public static Page initPage(int pageNum) {
        Page page = new Page();
        page.setPageNumber(BigInteger.valueOf(pageNum));
        page.setImgType("PDF");
        page.setPlainText(new ObjectFactory().createPagePlainText(normalizeString("")));
        return page;
    }

    public static String normalizeString(String text) {
        return text.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD]", "");
    }

    public static String generateOcrContent(Collection<String> pageXmlList)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<ocrextraction>");
        sb.append("<pages>");
        sb.append("<numPages>").append(Integer.toString(pageXmlList.size())).append("</numPages>");

        pageXmlList
            .stream()
            .filter((pageXml) ->
                    pageXml != null && pageXml.length() > 0
                    )
            .forEach(sb::append);

        sb.append("</pages>");
        sb.append("</ocrextraction>");

        return sb.toString();
    }

    public static ExtractedText initExtractedText() {
        ExtractedText extractedText = new ExtractedText();
        extractedText.setContent("");
        return extractedText;
    }

    public static int getPage(Page page) {
        return page.getPageNumber().intValue();
    }

    public static String getPageXml(Page page) throws IOException, JAXBException {
        StringWriter      sw              = new StringWriter();
        QName             pageQName       = new QName("", "page");
        JAXBElement<Page> pageJAXBElement = new JAXBElement<Page>(pageQName, Page.class, page);
        pageJaxbMarshaller.get().marshal(pageJAXBElement, sw);
        sw.close();
        String pageXml = sw.toString();

        if (pageXml == null || pageXml.isEmpty())
            return null;

        pageXml = normalizeString(pageXml);
        pageXml = replaceLigatures(pageXml);
        pageXml = replaceDiacritics(pageXml);

        return pageXml;
    }

    public static String replaceLigatures(String text) {
        String ret = text;
        for (Map.Entry<String, String> entry : ligatureHash.entrySet()) {
            ret = ret.replaceAll(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    public static String replaceDiacritics(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            Character a = diacriticHash.get(c);
            if (a != null) {
                sb.append(a);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String stripXmlHeader(String xml)
    {
        if (xml.startsWith("<?xml"))
        {
            int offset = xml.indexOf("?>");
            if (offset >= 0)
            {
                offset += 2;
                if (offset < xml.length())
                {
                    return xml.substring(offset);
                }
            }
        }
        return xml;
    }
}
