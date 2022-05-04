import json
import requests
import sys
import urllib.parse

################
# script to set up the logical dependencies 

# MCS server base endpoint
server="http://localhost:8036"

#### derived

mods_url = server + "/mcs/models"
part_url = server + "/mcs/parts"
logs_url = server + "/mcs/logids"

json_headers = {"Accepts": "application/json", "Content-Type": "application/json"}
text_headers = {"Accepts": "application/json", "Content-Type": "text/plain"}


################################################################

def pp_json(json_thing, sort=True, indents=4):
    if type(json_thing) is str:
        print(json.dumps(json.loads(json_thing), sort_keys=sort, indent=indents))
    else:
        print(json.dumps(json_thing, sort_keys=sort, indent=indents))
    return None


def add_logdeps(left, rights):

    # deps are:  left -> [right1, right2]
    json_data = json.dumps(rights)

    r = requests.put((logs_url + "/{}/deps").format(left), data=json_data, headers=json_headers)

    if r.status_code != 200:
        print("FAILED to put logical dependencies to id {}".format(left))


def set_blobstate(blobid, state):
    r = requests.put((mods_url + "/{}/state").format(blobid), data=state, headers=text_headers)

    if r.status_code != 200:
        print("FAILED to release blob")



def create_blob(meta):
    if not isinstance(meta, dict):
        raise Exception("param to create_blob must be type dict")

    r = requests.post(mods_url, data=json.dumps(meta), headers=json_headers)
    if r.status_code == 201:
        loc = r.headers.get('Location')
        id = loc[loc.rfind('/') + 1:]
        return id
    else:
        print("Creation failed with code {}".format(r.status_code))


def make_meta(creator, name, ver, alg_type):
    meta = {
        'executor'   : '',
        'outputType' : '',
        'product'    : '',
        'createdBy'  : creator,
        'name'       : name,
        'version'    : ver,
        'state'      : 'DRAFT',
        'core'       : { 'algorithm' : { 'type' : alg_type }, 'name' : name, 'version' : ver }
        }

    return meta


def set_owner(mcid, logids):
    r = requests.put((mods_url + "/{}/owns").format(mcid), data=json.dumps(logids), headers=json_headers)

    if r.status_code != 200:
        print("FAILED to set logical id ownership for {}".format(owns))
    

################################################################

if __name__ == "__main__":

    left1 = 'leftid';
    rights1 = ['right1','right2']
    rights2 = ['right3','right4']

    add_logdeps(left1, rights1);

    # 1. define logical deps
    # 2. make 2 MCIDs
    # 3. mcid1 owns 

    meta1 = make_meta('smccarty@apixio.com', 'comb1', '1.0.0', 'combiner');
    meta2 = make_meta('smccarty@apixio.com', 'comb2', '2.0.0', 'combiner');

    blobid1 = create_blob(meta1)
    blobid2 = create_blob(meta2)

    set_owner(blobid1, [left1])  # should work fine
    set_owner(blobid2, [left1])  # should work fine but will reparent left1

    # takes 2 transitions to release it
    set_blobstate(blobid1, 'ACCEPTED')
    set_blobstate(blobid1, 'RELEASED')
    set_blobstate(blobid1, 'ARCHIVED')

    # this should fail
    add_logdeps(left1, rights2);

