package com.apixio.useracct.email;

import java.io.StringWriter;
import java.util.Map;
import freemarker.template.Template;

/**
 * EmailTemplate represents the three parts required for sending an email to an
 * email address.  These parts are kept as freemarker template files in the
 * filesystem (initially, but packaged as part of the classpath in the end).
 *
 * The three parts of the template are:
 *
 *  * the subject; this is plaintext
 *  * the content for the MIME type "text/plain" portion of the email
 *  * the content for the MIME type "text/html" portion of the email.
 *
 * As these are freemarker templates, the content can have replaceable
 * fields as well as more general if/then/else constructs, as needed.
 *
 * The overall use model of the email template system is for the client to
 * call templateManagerInstance.getEmailTemplate("name") and then
 * call emailTemplateInstance.getSubject(model), etc.
 *
 * Templates live under the directory (before the system build) defined in the
 * templateBase value.  Template names can have "/" in them to provide another
 * level of organization (e.g., tm.getEmailTemplate("templateSet1/someTemplate").
 *
 * The actual names of the files are formed by adding ".subject", ".plain", and
 * ".html" to the template name.
 */
public class EmailTemplate {

    private TemplateManager tplManager;
    private String          base;

    EmailTemplate(TemplateManager tplManager, String name)
    {
        this.tplManager = tplManager;
        this.base       = name;
    }

    public String getSubject(Map<String, Object> model)
    {
        return getText(".subject", model);
    }

    public String getPlain(Map<String, Object> model)
    {
        return getText(".plain", model);
    }

    public String getHtml(Map<String, Object> model)
    {
        return getText(".html", model);
    }

    /**
     * Get the freemarker template and apply the model to it and return the
     * text.
     */
    private String getText(String type, Map<String, Object> model)
    {
        Template tpl = tplManager.getRawTemplate(base + type);

        if (tpl != null)
        {
            StringWriter sw = new StringWriter();

            // translate the checked exception to a runtime
            try
            {
                tpl.process(model, sw);
            }
            catch (Exception x)
            {
                throw new RuntimeException("Unable to get text from template " + base + type, x);
            }

            return sw.toString();
        }
        else
        {
            return "";
        }
    }

}
