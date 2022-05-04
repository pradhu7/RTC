package com.apixio.useracct.email;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

import com.apixio.SysServices;
import com.apixio.useracct.dao.TextBlobs;
import com.apixio.useracct.entity.TextBlob;

/**
 * TemplateManager is a thin layer over the freemarker system in order to
 * pull template files from the classpath easily.
 *
 */
public class TemplateManager {

    private Configuration fmConfiguration;
    private String        templateBase;

    /**
     *
     */
    public TemplateManager(SysServices sysServices, String baseDir) throws IOException
    {
        fmConfiguration = new Configuration();

        fmConfiguration.setDefaultEncoding("UTF-8");

        fmConfiguration.setTemplateLoader(new BlobTemplateLoader(sysServices.getTextBlobs()));
    }

    Template getRawTemplate(String name)
    {
        try
        {
            return fmConfiguration.getTemplate(name);
        }
        catch (IOException iox)
        {
            iox.printStackTrace();

            return null;
        }
    }


    public EmailTemplate getEmailTemplate(String templateName)
    {
        return new EmailTemplate(this, templateName);
    }

    /**
     *
     */
    private static class BlobTemplateLoader implements TemplateLoader {

        private TextBlobs  textBlobs;

        private BlobTemplateLoader(TextBlobs textBlobs)
        {
            this.textBlobs = textBlobs;
        }

        //Finds the template in the backing storage and returns an object that identifies the storage location where the template can be loaded from.
        public Object findTemplateSource(String name)
        {
            // return null if not found.
            // expect name to have slashes...

            return textBlobs.getBlob(name);
        }

        // Closes the template source, releasing any resources held that are only required for reading the template and/or its metadata.
        public void closeTemplateSource(Object templateSource)
        {
            // do nothing
        }

        //Returns the time of last modification of the specified template source.
        public long getLastModified(Object templateSource)
        {
            return ((TextBlob) templateSource).getBlobModified();
        }

        //Returns the character stream of a template represented by the specified template source.
        public Reader getReader(Object templateSource, String encoding)
        {
            TextBlob tb = (TextBlob) templateSource;

            return new StringReader(tb.getBlobContents());
        }
    }

}
