
1.  build apx-sdk on jenkins

2.  Get the URL for the apixio-fx-impls-*.jar artifact in artifactory and put the path into the 5 mc-configs/*.publish files.
    To get this path, log in to https://repos.apixio.com and do a "Repository Browser" descent into

     > apixio-release-snapshot-jars
       > apixio
         > apixio-fx-impls
	   > {version}
	     > apixio-fx-impls-{version-build}.jar

    then copy the Repository Path (right pane) to clipboard; you should see something like the following:

     apixio-release-snapshot-jars/apixio/apixio-fx-impls/0.0.2-SNAPSHOT/apixio-fx-impls-0.0.2-20210527.144943-5.jar

    If macros are being used in the .publish files, then it's likely that only the part after "apixio-release-snapshot-jars"
    is required
      
