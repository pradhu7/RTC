
# this script takes the (modified) output from getdeps.sh and creates a pom.xml-compatible
# list of <dependency> elements to be included in the parent pom.xml.  the output is left
# in /tmp/yy.  this output MUST be reviewed and modified to look for errors/messages and
# to make macros/properties out of versions that are common across a suite of related
# artifacts (e.g., jackson or springframework).
#
# note that some warnings are expected in the output for the case where an artifact is
# used for both compile and test scopes.  this condition results in output like the following:
#
#   Artifact commons-cli:commons-cli version declared more than once.  Using 1.3.1 instead of 1.3.1

cd $(dirname $0)

awk -f deps-to-xml.awk /tmp/xx | sort > /tmp/yy

