import json
import requests
import sys
import urllib.parse

################
# script to set up the logical dependencies 

# MCS server base endpoint
server="http://localhost:8036"

#### derived

base_url = server + "/mcs/models"
part_url = server + "/mcs/parts"
logs_url = server + "/mcs/logids"
json_headers = {"Accepts": "application/json", "Content-Type": "application/json"}


# siggens to create MCs for; list is maintained by hand
siggens = {
    'AssumedDate'            : { 'ver' : '1.0.0', 'type' : 'PAGE_WINDOW' },
    'ConditionGuideLine'     : { 'ver' : '1.1.0', 'type' : 'PAGE_WINDOW' },
    'InferredDate'           : { 'ver' : '1.0.0', 'type' : 'PAGE_WINDOW' },
    'Junkiness'              : { 'ver' : '1.1.0', 'type' : 'PAGE_WINDOW' },
    'PositionalTerm'         : { 'ver' : '1.0.0', 'type' : 'PAGE_WINDOW' },
    'LogitFaceToFace'        : { 'ver' : '1.0.0', 'type' : 'PAGE_WINDOW' },
    'V5'                     : { 'ver' : '1.0.1', 'type' : 'PAGE_WINDOW' },
    'V6'                     : { 'ver' : '1.0.2', 'type' : 'PAGE_WINDOW' },
    'SVM'                    : { 'ver' : '1.0.1', 'type' : 'PAGE_WINDOW' },
    'CodeableSectionDictHit' : { 'ver' : '1.1.0', 'type' : 'PAGE_WINDOW' },
    'Dictionary'             : { 'ver' : '1.1.0', 'type' : 'PAGE_WINDOW' },
    'Topics'                 : { 'ver' : '1.1.1', 'type' : 'PAGE_WINDOW' },
    'HAR'                    : { 'ver' : '1.1.0', 'type' : 'PAGE_WINDOW' },
    'WordCount'              : { 'ver' : '1.0.0', 'type' : 'PAGE_WINDOW' },
    'CharacterCount'         : { 'ver' : '1.0.0', 'type' : 'PAGE_WINDOW' },
    'TextSuspects'           : { 'ver' : '1.0.0', 'type' : 'WHOLE_DOC' },
    'DateIsAssumed'          : { 'ver' : '1.0.0', 'type' : 'PATIENT' },
    'SlimLynty'              : { 'ver' : '1.1.1', 'type' : 'PATIENT' }
    }

logical_combiner_id='MA-V22:1.6.0/8.0.5'

################################################################

def pp_json(json_thing, sort=True, indents=4):
    if type(json_thing) is str:
        print(json.dumps(json.loads(json_thing), sort_keys=sort, indent=indents))
    else:
        print(json.dumps(json_thing, sort_keys=sort, indent=indents))
    return None


def add_logdeps():
    right = []

    for sg in siggens:
        right.append(sg + ':' + siggens[sg]['ver'])

    json_data = json.dumps(right)
    print(json_data)

    r = requests.put((logs_url + "/{}/deps").format(urllib.parse.quote(logical_combiner_id, safe='')), data=json_data, headers=json_headers)

    if r.status_code != 200:
        print("FAILED to put logical dependencies to id {}".format(logical_combiner_id))


################################################################

if __name__ == "__main__":

    add_logdeps()
