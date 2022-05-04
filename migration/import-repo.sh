
echo 'THIS IS AN INFORMATIONAL-ONLY SCRIPT!  It should be viewed as a template for future migrations'
exit 0

function move_to_subdir() {
    if [ "$1" = "" ]; then echo "Usage:  $0 git-repo-dir subdirname"; exit 1; fi
    if [ "$2" = "" ]; then echo "Usage:  $0 git-repo-dir subdirname"; exit 1; fi
    if [ ! -d "$1/.git" ] ; then echo "$1/.git must be a directory"; exit 1; fi

    subdirname=$2
    (
	cd $1

	git filter-branch -f --prune-empty --tree-filter "
mkdir -p .sub;
mv -f .gitignore * .sub;
mv .sub $subdirname
    " -- --all

	git gc --aggressive
    )
}    

function merge() {
    if [ "$1" = "" ]; then echo "Usage:  $0 dirname branchname"; exit 1; fi
    if [ "$2" = "" ]; then echo "Usage:  $0 dirname branchname"; exit 1; fi
    if [ ! -d "$1/.git" ] ; then echo "$1/.git must be a directory"; exit 1; fi

    (
	cd mono

	git remote add $1 ../$1
	git fetch $1

	git checkout $2
	git merge -m"Import merge from old repository $1/$2" $1/$2 --allow-unrelated-histories

	git remote rm $1
    )
}

REPOS="common model datasource"

rm -rf $REPOS mono

# pull fresh copies of source repos from github
for i in $REPOS
do
    git clone git@github.com:Apixio/$i.git
    (cd common; git checkout master)
done

# to start with, github has an empty "mono" repo; clone and set info
git clone git@github.com:Apixio/mono.git
(cd mono; git checkout -b dev; git checkout master; git config user.email smccarty@apixio.com)

# real work starts here and assumes that "mono" is in a pristine state and that
# the source repos are clean and up-to-date with respect to github contents and
# that both "master" and "dev" branches exist and are also up-to-date
for i in $REPOS
do
    move_to_subdir $i $i.d  # HACK:  .d is so that we can do a "git mv " from a repo/subdir without a naming conflict; see git moves below
done

for i in $REPOS
do
    merge $i master
    merge $i dev
done

# mono left in dev branch; now move each repos subdirs up

cd mono

# now move the duplicated subdirs up to be a direct child of main mono directory
git mv common.d/buildable/common .
git mv common.d/buildable/security .

git mv model.d/buildable/model .
git mv model.d/buildable/APOConverter .
git mv model.d/buildable/ensemble-converter .
git mv model.d/buildable/sciencetest .

git mv datasource.d/awslambda .
git mv datasource.d/bizlogic .
git mv datasource.d/dao .
git mv datasource.d/ds .
git mv datasource.d/entity-common .
git mv datasource.d/filtered-patient-events .

git commit -m"Moved module directories up from merged repositories"

# PUSH to github ONLY when all looks good

# todo:
# merge top level .gitignore
# single copy of pull_request_template.md
# deal with Jenkinsfile
# create master pom.xml and module pom.xmls
