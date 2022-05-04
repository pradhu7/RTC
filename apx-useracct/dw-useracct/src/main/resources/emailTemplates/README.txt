Email template files are grouped together by base template name.  There should be
a subject file, and there must be a text/plain file.  The last file, an HTML file, is
optional but recommended.

The filename format is:

  * subject file:     {basename}.subject
  * text/plain file:  {basename}.plain
  * text/html file:   {basename}.html

They are processed as freemarker template files.

Splitting things up this way allows any (text) content to be included in the file
without worrying about special delimiting formatting and parsing.

Subject files should contain a single line of text, and will have the model used
when processing.


From the Java side:

* base class is

   public TextTemplate {
     public TextTemplate(String basename, Map<String, Object> params)
	 {}

     public String makePlain()  // uses file {configuredBase}/{basename}.plain
	 {}

	 public String makeHtml()   // uses file {configuredBase}/{basename}.html
	 {}
	 
   }


* sample use of the above:

    Map<String, Object> params = new HashMap<String, Object>();
	params.put("link", theLink);
    TextTemplate newUserEmail = new TextTemplate("newAccount", params);
	sendEmail(newUserEmail.makePlain(), newUserEmail.makeHtml());

* template text for the above (note required blank line between subject and body)

    ## file is {basename}/newAccount.plain
    Subject: Please confirm new account

	An Apixio account has been set up.  Click on the following link to confirm,
	or copy the URL into a browser address bar:

	  ${link}


    ## file is {basename}/newAccount.html
    Subject: Please confirm new account

    <html><body>
	An Apixio account has been set up.  Click on the following link to confirm,
	or copy the URL into a browser address bar:<p>

	  <a href="${link}">${link}

	<p></body></html>

