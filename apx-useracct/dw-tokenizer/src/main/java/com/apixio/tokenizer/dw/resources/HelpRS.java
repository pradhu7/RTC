package com.apixio.tokenizer.dw.resources;

import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by alarocca on 8/10/16.
 */
public class HelpRS extends HttpServlet {
    private String helpHtmlContent = fetchHtmlFile();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {
        response.getWriter().write(helpHtmlContent);
        response.setContentType("text/html");
    }

    private String fetchHtmlFile() {
        String html = "";
        try {
            InputStream stream = getClass().getResourceAsStream("/raml.html");
            html = IOUtils.toString(stream, "UTF-8");
            stream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return html;
    }
}