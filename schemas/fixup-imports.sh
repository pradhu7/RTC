#!/bin/bash
set -x
set -e
set -o pipefail
if [[ "$(uname -s)" == "Darwin" ]];then
  if ! which gsed;then
    echo "coreutils sed required on OSX"
    echo "run 'brew install gsed'"
    exit 1
  fi
  SED=gsed
else
  SED=sed
fi
find ./apxschemas -name '*.py' | xargs -n1 $SED -i 's|from com\.apixio|from apxschemas.com.apixio|g' && find ./apxschemas -type d | xargs -I DIR touch DIR/__init__.py
