package com.apixio.ocr;

import org.junit.Assert;
import org.junit.Test;

public class OcrPageUtilTest {
    @Test
    public void testLigatures() {
        String ligatures = "ßæœĲĳᵫꜩꜳꜵꜷꜹꜽꝏﬀﬁﬂﬃﬄﬅﬆ";
        Assert.assertEquals(ligatures, OcrPageUtil.normalizeString(ligatures));
    }

    @Test
    public void testReplaceDiatritics() {
        String strDiacrtics  = "abcāáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜĀÁǍÀĒÉĚÈĪÍǏÌŌÓǑÒŪÚǓÙǕǗǙǛ123";
        String strReplaced   = "abcaaaaeeeeiiiioooouuuuuuuuAAAAEEEEIIIIOOOOUUUUUUUU123";

        Assert.assertEquals(strReplaced, OcrPageUtil.replaceDiacritics(strDiacrtics));
    }

    @Test
    public void testReplaceLigatures() {
        String strLigatures = "abcẞßæœĲĳᵫꜩꜳꜵꜷꜹꜽꝏﬀﬁﬂﬃﬄﬅﬆ123";
        String strReplaced  = "abcfsfzaeoeIJijuetzaaaoauavayoofffiflffifflftst123";
        Assert.assertEquals(strReplaced, OcrPageUtil.replaceLigatures(strLigatures));
    }
}
