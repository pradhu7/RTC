# Running End-to-end tests for SDK

These are temporary-ish instructions on getting current Face2Face f(x) working against staging machines, with the goal being that a successful test run means that you're ready to use the SDK for real.

Four repositories (so far) have been modified as part of the SDK project and until I can figure out why the maven artifacts for these changes aren't in artifactory, all four of these repositories will need to be built locally.

They're listed in this document:  https://docs.google.com/spreadsheets/d/1QlTjUAII_5fO7lrPMXPUmRRcqQmmICDQOl6oDcmXUHs/edit#gid=0

and the order of building them is:  mono, apx-modelcatalogsvc, apx-signalmanager, and apx-sdk.  For the first three, use the "sdk-project" branch and for the last one use "dev" branch.

Once these have been built successfully, then the staging test can be done.

## Setup (IMPORTANT!)

1. Download this artificact into `./test/resources` as `model.zip':
`https://repos.apixio.com/artifactory/webapp/#/artifacts/browse/tree/General/models/models/production_model/9.4.0/production_model_9_4_0.zip`

2. Ensure `/tmp` exists

3. Modify `test/staging.yaml` to add s3access and s3secret keys.  The current config there is for unencrypted keys but change the yaml key name to use encrypted keys.

4. A valid vault token should be in an exported shell variable APX_VAULT_TOKEN

## Running the tests

### Face to face

$ sh TEST-f2f-staging.sh

### HAR2

$ sh TEST-har2-staging.sh

## Notes

The staging.yaml has some old Cassandra IP addresses (how do we get up to date ones??) so you'll see some timeouts on Cassandra init.

