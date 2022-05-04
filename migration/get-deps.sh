
# this script does a "mvn dependency:tree" on all subdirs (minus a few) and massages
# the output to get a unique list of maven artifact IDs used by the module.  the
# aggregate list of dependencies is left in /tmp/xx.
#
# the intent of this script is to help to create the list of dependencies that need
# to be specified by the parent pom.xml (in <dependencyManagement> element).  the
# results of this need to be examined and cleaned up (e.g., remove Apixio artifacts
# that are produced by this mono repo).
#
# once the list of dependencies looks good, run deps-to-xml.sh to produce a pom.xml-compatible
# list of dependencies.

dirs=$(find . -maxdepth 1 -type d | grep -v '\.d$' | grep -v '^\.$')

rm -f /tmp/uu

for i in $dirs
do
    (cd $i
     mvn -o dependency:tree > dt

     # ugly:  need both jar and pom deps and the "] +-" is an inner top-level dep and "] \-" is the last one
     grep "\] +-"   dt | grep :jar | awk '{print $NF}' | sort -u  > dt.uniq
     grep "\] \\\-" dt | grep :jar | awk '{print $NF}' | sort -u >> dt.uniq
     grep "\] +-"   dt | grep :pom | awk '{print $NF}' | sort -u >> dt.uniq
     grep "\] \\\-" dt | grep :pom | awk '{print $NF}' | sort -u >> dt.uniq
     cat dt.uniq >> /tmp/uu
     )
done
    
sort -u /tmp/uu > /tmp/xx
