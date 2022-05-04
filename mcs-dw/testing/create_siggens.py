import json
import requests
import sys

################
# script to create a set of ModelCombinations for signal generators
# where each of these MCs uses the 3 parts from an already-registered
# MC for a combiner


# the MCID of the (V22, for now) combiner:
combiner_mcid="B_106bb7b4-0242-4b1e-b0b5-d06600c09ab3"
#combiner_mcid="B_78583a99-626f-4732-b5de-9a577fd88c3e"

# MCS server base endpoint
server="http://localhost:8036"

# uploader/creator of blobs and parts
uploader='twang@apixio.com'


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

################################################################

def pp_json(json_thing, sort=True, indents=4):
    if type(json_thing) is str:
        print(json.dumps(json.loads(json_thing), sort_keys=sort, indent=indents))
    else:
        print(json.dumps(json_thing, sort_keys=sort, indent=indents))
    return None

def get_combiner_meta():
    r = requests.get(base_url + "/{}/metadata".format(combiner_mcid), headers=json_headers)
    if r.status_code == 200:
        meta = r.json()

        # clear out what we don't want duplicated
        del meta['id']
        del meta['core']
        del meta['search']
        del meta['createdAt']
        del meta['createdBy']
        for part in meta['parts']:
            del part['core']
        return meta

def create_blob(meta):
    if not isinstance(meta, dict):
        raise Exception("param to create_blob must be type dict")

    r = requests.post(base_url, data=json.dumps(meta), headers=json_headers)
    if r.status_code == 201:
        loc = r.headers.get('Location')
        id = loc[loc.rfind('/') + 1:]
        return id
    else:
        print("Creation failed with code {}".format(r.status_code))

def add_parts(blob_id, parts, uploaded_by):
    headers = {**json_headers, "Apixio-Uploaded-By": uploaded_by}
    r = requests.put((base_url + "/{}/parts").format(blob_id), data=json.dumps(parts), headers=headers)

    if r.status_code != 200:
        print("FAILED to put parts to blob {}".format(blob_id))

def dup_parts(creator, combiner_meta, sg_name, sg_ver, sg_type):
    sg_meta = {
        'executor'   : '',
        'outputType' : '',
        'product'    : '',
        'createdBy'  : creator,
        'name'       : sg_name,
        'version'    : sg_ver,
        'state'      : 'DRAFT',
        'core'       : { 'algorithm' : { 'type' : sg_type }, 'name' : sg_name, 'version' : sg_ver }
        }

    parts = []

    for part in combiner_meta['parts']:
        parts.append({
            'name'     : part['name'],
            'source'   : 'md5:' + part['md5Digest'],
            'mimeType' : part['mimeType']
        })

    sg_id = create_blob(sg_meta)

    for part in combiner_meta['parts']:
        add_parts(sg_id, parts, creator)

    print("Created SigGen-type MCID {} for generator {}:{}".format(sg_id, sg_name, sg_ver))

    return sg_id

def set_owner(name, ver, mcid):
    owns = [name + ':' + ver]
    r = requests.put((base_url + "/{}/owns").format(mcid), data=json.dumps(owns), headers=json_headers)

    if r.status_code != 200:
        print("FAILED to set logical id ownership for {}".format(owns))
    
################################################################

if __name__ == "__main__":
    import sys

    ################################################################
    combiner_meta = get_combiner_meta()

#    pp_json(combiner_meta)

    combiner_meta['createdBy'] = uploader

    for siggen in siggens:
        ver = siggens[siggen]['ver']
        typ = siggens[siggen]['type']
        sgmcid = dup_parts(uploader, combiner_meta, siggen, ver, typ)
        set_owner(siggen, ver, sgmcid)
