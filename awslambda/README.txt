June 1, 2020
============

IMPORTANT NOTE:  the Java code for the ENCRYPT_REPLACE is intentionally disabled in that
it doesn't actually copy and remove S3 objects!!!  Look in S3BaseLambdaFunc.java for
these operations!

This module implements the functionality of rekeying S3 objects.  This rekeying at a high level is
just a read and a write of an S3 object.  As S3 objects are supposed to be encrypted (well, at
least the ones that are to be rekeyed), the reading actually does the decryption, and the
writing is done via an encrypted stream so what's written back to S3 is encrypted.

This code is meant to be run as an AWS Lambda Function, as part of an S3 Batch Operation.
Lambda functions that are meant to be invoked via an S3 batch operation have a different
input/output object signature than the typical Lambda Function--specifically the parameters
are a simple translation from JSON to standard Java objects such as Map and List (as compared
to an AWS-defined POJO for event-invoked Lambda Functions).  See
https://docs.aws.amazon.com/AmazonS3/latest/dev/batch-ops-invoke-lambda.html for details.

####

Overview of running these AWS functions

There is no standalone test driver for this code (although it is possible in theory to create
one but it would leave out the actual AWS operations) and so the testing of it has been done
via the functionality available in the AWS console. Similarly, there is no "production" driver
for this code outside of the actual AWS S3 Batch Operation system.  Because of this, it's
necessary to know some about how to configure and run the functions from the console (or via
AWS command line, although that's not covered in this document).

In general there are two steps necessary to run this code: create (and configure) the AWS
Lambda Function, and then run it, either via a Test mode in the console or via creating an
actual S3 batch job and running it.

####

Creating and configuring the AWS function is done through the "AWS Lambda" console.  Lambda is
one of the top-level AWS services so getting to that console is as simple as searching for
"lambda" in the "Services" panel of the main AWS console.  Make sure the Region selected
(upper right of screen) matches the region of the VPC that has the Vault server (Oregon,
currently).

While in the Lambda console GUI, it's a matter of selecting the "Functions" link from the
navigation bar on the left, and then using the "Create Function" button on the upper right.
The choices shown there are "Author from scratch", "Use a blueprint", and "Browse serverless
app repository".  Choose "Author from scratch" (which is the default) and enter a unique and
meaningful Function name.  Then:

* choose Java 11 as the Runtime (Java 8 has restricted encryption capabilities by default)

* open up "Choose or create an execution role" and select "Use an existing role" and select the
  role specified by dev-ops (and/or an arbitrary role from the list as this _should_ end up being
  overridden by the S3 Batch Operation role)

* click on Create Function

The configuration page for the function is now displayed.  The following elements of that page
need to be specified:

* Upload:  select the jar file produced from "mvn -DskipTests clean install"; currently that's
  target/apixio-s3-lambdafuncs-3.0.0-SNAPSHOT.jar

* Handler:  com.apixio.s3lambda.S3BatchLambdaFunc::handleRequest (or, for debugging it should be
            com.apixio.s3lambda.S3DebugLambdaFunc::handleRequest)

* Environment variables:  add the following:

  * VAULT_TOKEN:  Vault server token; get the value from dev-ops; optional if some other vars are defined
  * APX_VAULT_TOKEN:  alternate var for Vault server token; get the value from dev-ops; optional if some other vars are defined
  * APX_VAULT_ROLE_ID:  alternate auth method for Vault server; get the value from dev-ops; optional if some other vars are defined
  * APX_VAULT_SECRET_ID:  alternate auth method for Vault server; get the value from dev-ops; optional if some other vars are defined
  * RUN_MODE:  one of DECRYPT_ONLY, ENCRYPT_ONLY, ENCRYPT_REPLACE; optional; defaults to DECRYPT_ONLY
  * S3ACCESSKEY:  access key for bucket that contains S3 objects to be processed; get the value from dev-ops; required
  * S3SECRETKEY:  secret key for bucket that contains S3 objects to be processed; get the value from dev-ops; required
  * bcfipsname:  filename of BouncyCastle FIPS resource; must match filename in src/main/resources/bc-fips-*.jar; required
  * bcprovname:  filename of BouncyCastle non-FIPS resource; must match filename in src/main/resources/bcbcprov-jdk15on-1.*.jar; required

  Note that the last two BouncyCastle items are filenames (not paths) that are for jar files retrievable
  via classpath resource fetching.

* Network:  select the correct VPC, subnets, and security groups for access to Vault server; get values from dev-ops

Click on "Save" to upload the .jar and save the configuration.

Note that the security library requires a valid Vault token and there are two ways for it to
have such a token:  directly supplying it via an environment variable (either VAULT_TOKEN or
APX_VAULT_TOKEN) or by specifying authentication information that will be used to retrieve a
token from Vault (both APX_VAULT_ROLE_ID and APX_VAULT_SECRET_ID are needed for this).

####

The Lambda Function can be tested from the configuration page by creating a named test.  This
is done by choosing "Configure test events" in the dropdown to the left of the "Test" button
on the upper part of the page.  This will bring up a dialog that lets you create or edit an
event.

The current test code in S3DebugLambdaFunc is compiled with S3EventNotification as its input
type so select "Amazon S3 Put" from the list of Event templates, name it something meaningful
(e.g., "s3puttest"), and Create it.

Make sure that the Handler class is com.apixio.s3lambda.S3DebugLambdaFunc::handleRequest
(change it and Save if it isn't).  Then click on the Test button.  A test success/failure box
is shown at the top of the page.  Just click on the "logs" link to get more information.


####

Once the Lambda Function has been created and configured, it can be used by an S3 Batch
Operation job.  In order to create a batch operation, the S3 Console can be used.  Choose the
Batch Operations link from the left navigation area and then Create Job.

The Region the job is created in must match the region the Lambda Function was created in (or
it won't show up in the list of Functions).

An S3 batch operation requires a source list of S3 objects to process.  This can be either a
manifest file that S3 creates through an Inventory Report, or it can be a .csv file that's
manually created.  The S3 objects listed in these sources must be in the S3 bucket given by
the S3ACCESSKEY and S3SECRETKEY configured in the Lambda Function.  Once the source list has
been entered, click on Next to specify the Operation.

The operation chosen must be "Invoke AWS Lambda Function" and the function chosen should be
the name of the Function created and configured above.  Click on Next to finish job
configuration.  The critical field here is the Permissions value--it should be an IAM role
whose name is supplied by dev-ops as one that has permission to read and write on the S3
bucket (e.g., BatchOperationsLambdaRole).

Click on Next will show a summary page.  Create the job and then wait for a few seconds and
refresh the page.  When the Status of the new Job ID shows "Awaiting your confirmation to
run".  Confirm the total number of objects listed in manifest as what is expected/reasonable
as S3 Batch Operations jobs cost real money.  If satisfied, click on the job and then on "Run
Job".

The Status should change to "Active".  Refresh as necessary until the status is Completed or
Failed.  Review logs as necessary but note that the log data written by the actual Lambda
Function are *not* directly accessible from this S3 Batch Operations area.  In order to review
Function logs, go back to the "AWS Lambda" console, click on the Function name to get into the
configuration (etc.) page and then click on the "Monitoring" tab.  Scroll down to the
"CloudWatch Logs Insights" section and the log streams from the batch job will show up.

####

Notes:

* cancelling an S3 Batch Operation can take some time; also, some jobs can't be cancelled

* failures to invoke the Lambda Function will slow down the job quite a bit as the batch
  operation engine will perform retries

* common reasons for failures include:

  * expired VAULT_TOKEN
  * permissions on S3 buckets

* a successful S3 Batch Operation job does *not* mean that the actual rekeying operation
  worked (although an exception at the Function level should pass that failure back up to
  the batch operation log file, but there could be some failures that don't make it there).
  it's important to spot check the actual Function logs

* a job-level persistent failure could end up not processing all S3 objects, leading to some
  confusion as to what's really going on

