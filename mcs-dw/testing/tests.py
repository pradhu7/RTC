import requests
import json
import sys

# tests to add:
#  * duplicate part content reuses the existing S3 object URL
#  * search by "tag"; i.e., query params are "search.tags=contains(v22)"

################

server = "localhost:8036"
base_url = "http://" + server + "/mcs/models"
part_url = "http://" + server + "/mcs/parts"
json_headers = {"Accepts": "application/json", "Content-Type": "application/json"}

# use % operator with these because we're using braces that confuse str.format()
json_fullmeta = '''
{
"createdBy": "scott%(id)s",
"name": "a test model file hullabaloo %(id)s",
"executor": "say what executor",
"outputType":  "who knows what output type",
"state":  "DRAFT",
"pdsId": "pds_00%(id)s",
"product": "123%(id)s",
"core": {"important": "stuff", "tags": ["v22","v23", "v_%(id)s"]},
"search": {"thisiscool": "custfield"}
}
'''

json_partialmeta = '''
{
"name": "a test model file hullabaloo %(id)s",
"executor": "say what executor",
"outputType":  "who knows what output type"
}
'''

# this is a subset of meta data that contains only json 'extra' data
json_alljson = '''
{
  "core": {
    "subkeycore": "subvalue%(val)s"
  },
  "search": {
    "subkeysearch": "subvalue%(val)s"
  }
}
'''

json_jsonnokey = '''
{
  "subkey1": "subvalue1%(val)s"
}
'''

json_jsonnokey2 = '''
{
  "subkey2": "subvalue2%(val)s"
}
'''

empty_array = json.loads("[]")

################################################################

# useful for testing via ordered(a) == ordered(b)
def ordered(obj):
    if isinstance(obj, dict):
        return sorted((k, ordered(v)) for k, v in obj.items())
    if isinstance(obj, list):
        return sorted(ordered(x) for x in obj)
    else:
        return obj

################ 

def pp_json(json_thing, sort=True, indents=4):
    if type(json_thing) is str:
        print(json.dumps(json.loads(json_thing), sort_keys=sort, indent=indents))
    else:
        print(json.dumps(json_thing, sort_keys=sort, indent=indents))
    return None

################################################################ make json meta

def make_full_meta(id):
    return json.loads(json_fullmeta % {'id': id})

def make_partial_meta(id):
    return json.loads(json_partialmeta % {'id': id})

def make_json_all(val):
    return json.loads(json_alljson % {'val': val})

def make_json_no_key(val):
    return json.loads(json_jsonnokey % {'val': val})

def make_json_no_key2(val):
    return json.loads(json_jsonnokey2 % {'val': val})

# adds d2 on top of d1; python3.5+
def merge_dicts(d1, d2):
    return {**d1, **d2}

# remove all that server adds so we can compare json
def remove_server_fields(blob_meta):
    del blob_meta['createdAt']
    del blob_meta['id']
    del blob_meta['parts']
    del blob_meta['deleted']
    del blob_meta['version']
    return blob_meta

def compare_json(meta1, meta2, message):
    if ordered(meta1) != ordered(meta2):
        print(message + ": " + str(meta1) + " :: " + str(meta2))

################################################################ actual tests
def create_blob(meta):
    if not isinstance(meta, dict):
        raise Exception("param to create_blob must be type dict")

    r = requests.post(base_url, data=json.dumps(meta), headers=json_headers)
    if r.status_code == 201:
        loc = r.headers.get('Location')
        id = loc[loc.rfind('/') + 1:]
        print("created blob with " + id)
        return id
    else:
        print("Creation failed with code {}".format(r.status_code))

def update_meta(blob_id, meta, method="replace"):
    if not isinstance(meta, dict):
        raise Exception("param to update_meta must be type dict")

    qp = {"method": method}
    r = requests.put(base_url + "/{}/metadata".format(blob_id), params=qp, data=json.dumps(meta), headers=json_headers)
    if r.status_code == 200:
        print("SUCCESSFUL update metadata for blob {}".format(blob_id))
    else:
        print("FAILED to update metadata for blob {}".format(blob_id))

def update_json(blob_id, jsonmeta, type):
    if not isinstance(jsonmeta, dict):
        raise Exception("param to update_json must be type dict")

    r = requests.put(base_url + "/{}/metadata/{}".format(blob_id, type), data=json.dumps(jsonmeta), headers=json_headers)
    if r.status_code == 200:
        print("SUCCESSFUL update json for blob {}".format(blob_id))
    else:
        print("FAILED to update json for blob {}".format(blob_id))

# content is just a string for now...
def add_part(blob_id, part_name, uploaded_by, content_type, content):
    headers = {**json_headers, "Apixio-Uploaded-By": uploaded_by, "Content-Type": content_type}
    r = requests.put((base_url + "/{}/parts/{}").format(blob_id, part_name), data=content, headers=headers)

    if r.status_code == 200:
        print("SUCCESSFUL upload of part {} to blob {}".format(part_name, blob_id))
    else:
        print("FAILED to upload part {} to blob {}".format(part_name, blob_id))

def add_part_md5(blob_id, part_name, uploaded_by, md5):
    headers = {**json_headers, "Apixio-Uploaded-By": uploaded_by}
    r = requests.put((base_url + "/{}/parts/{}?ref={}").format(blob_id, part_name, md5), headers=headers)

    if r.status_code == 200:
        print("SUCCESSFUL put of part {} to blob {}".format(part_name, blob_id))
    else:
        print("FAILED to put part {} to blob {}".format(part_name, blob_id))

def update_part_json(blob_id, part_name, jsonmeta, type):
    if not isinstance(jsonmeta, dict):
        raise Exception("param to update_json must be type dict")

    r = requests.put(base_url + "/{}/parts/{}/metadata/{}".format(blob_id, part_name, type), data=json.dumps(jsonmeta), headers=json_headers)
    if r.status_code == 200:
        print("SUCCESSFUL update json for part {}".format(part_name))
    else:
        print("FAILED to update json for part {}".format(part_name))

################################################################

def get_blob_meta_by_id(id):
    r = requests.get(base_url + "/{}/metadata".format(id), headers=json_headers)
    if r.status_code == 200:
        return r.json()
    else:
        print("Failed to get blob by id {}".format(id))

def get_blob_meta_by_like_field(field, like):
    qp = {"{}".format(field): "like({})".format(like)}
    r = requests.get(base_url, params=qp, headers=json_headers)
    if r.status_code == 200:
        return r.json()

def get_blob_meta_by_json(field, expr):
    qp = {"{}".format(field): "{}".format(expr)}
    r = requests.get(base_url, params=qp, headers=json_headers)
    if r.status_code == 200:
        return r.json()

def get_blob_json_by_id(id, type):
    r = requests.get((base_url + "/{}/metadata/{}").format(id, type), headers=json_headers)
    if r.status_code == 200:
        return r.json()

def get_part_json_by_id(id, part_name, type):
    r = requests.get((base_url + "/{}/parts/{}/metadata/{}").format(id, part_name, type), headers=json_headers)
    if r.status_code == 200:
        return r.json()

def get_parts_json_by_md5(md5):
    r = requests.get((part_url + "?md5={}").format(md5), headers=json_headers)
    if r.status_code == 200:
        return r.json()

################################################################
# run tests:
if __name__ == "__main__":
    import sys

    ################################################################
    print("\n\n#### test 1:  create blob, compare metadata")
    meta1 = make_full_meta('this is from test 1')
    blob_id1 = create_blob(meta1)
    blob_meta = get_blob_meta_by_id(blob_id1)

    remove_server_fields(blob_meta)
    compare_json(blob_meta, meta1, "Creation of blob resulted in different metadata")


    ################################################################
    print("\n\n#### test 2:  replace meta while in DRAFT state, compare metadata")
    meta2 = make_full_meta('this is from test 2')
    update_meta(blob_id1, meta2, method="replace")
    blob_meta2 = get_blob_meta_by_id(blob_id1)

    remove_server_fields(blob_meta2)
    del meta2['createdBy']
    del blob_meta2['createdBy']
    compare_json(blob_meta2, meta2, "Update blob resulted in different metadata")


    ################################################################
    print("\n\n#### test 3:  update parti meta while in DRAFT state, compare metadata")
    meta3 = make_partial_meta('this is from test 3')
    update_meta(blob_id1, meta3, method="update")
    blob_meta3 = get_blob_meta_by_id(blob_id1)

    remove_server_fields(blob_meta3)
    del blob_meta3['createdBy']         # not removed by remove_server_fields as we wanted to compare in test2
    meta3 = merge_dicts(meta2, meta3)   # since this is partial only
    compare_json(blob_meta3, meta3, "Partial update of blob meta resulted in different metadata")


    ################################################################
    print("\n\n#### test 4:  update json core and search via model meta")
    meta4 = make_json_all('this is from test 4')
    update_meta(blob_id1, meta4, method="update")  # MUST be 'update' for json-only
    blob_meta4 = get_blob_meta_by_id(blob_id1)

    remove_server_fields(blob_meta4)
    del blob_meta4['createdBy']         # not removed by remove_server_fields as we wanted to compare in test2
    meta4 = merge_dicts(meta3, meta4)   # since this is partial only
    compare_json(blob_meta4, meta4, "update of just json blob meta resulted in different metadata")

    
    ################################################################
    print("\n\n#### test 5:  update json core via direct address")
    json5 = make_json_no_key('this is from test 5');
    update_json(blob_id1, json5, 'core')
    json_meta5 = get_blob_json_by_id(blob_id1, 'core')

    compare_json(json5, json_meta5, "update of just core json resulted in different json")


    ################################################################
    print("\n\n#### test 6:  update json search via direct address")
    json6 = make_json_no_key('this is from test 6');
    update_json(blob_id1, json6, 'search')
    json_meta6 = get_blob_json_by_id(blob_id1, 'search')

    compare_json(json6, json_meta6, "update of just search json resulted in different json")


    ################################################################
    print("\n\n#### test 7:  create part 'aPart1'")
    add_part(blob_id1, "aPart1", "personWhoUploaded", "text/plain", "now is the time")
    add_part(blob_id1, "aPart2", "personWhoUploaded", "text/plain", "now is the time")  # duplicate content -> share s3 url

    ################################################################
    print("\n\n#### test 8:  update part's 'core' json")
    json8 = make_json_no_key2('this is from test 8');
    update_part_json(blob_id1, 'aPart1', json8, 'core')
    json_meta8 = get_part_json_by_id(blob_id1, 'aPart1', 'core')

    compare_json(json8, json_meta8, "update of part's core json resulted in different json")


    ################################################################
    print("\n\n#### test 9:  update part's 'search' json")
    json9 = make_json_no_key2('this is from test 9');
    update_part_json(blob_id1, 'aPart1', json9, 'search')
    json_meta9 = get_part_json_by_id(blob_id1, 'aPart1', 'search')

    compare_json(json9, json_meta9, "update of part's search json resulted in different json")


    ################################################################
    print("\n\n#### test 10:  attempt search on part json")
    #### note that "subkey2" should only appear in json on a part:
    blobs = get_blob_meta_by_like_field("core.subkey2", "subvalue2%")
    compare_json(empty_array, blobs, "Search on core.subkey2 = subvalue2")

    blobs = get_blob_meta_by_like_field("search.subkey2", "subvalue2%")
    compare_json(empty_array, blobs, "Search on search.subkey2 subvalue2%")

    ################################################################
    print("\n\n#### test 11:  attempt search on non-part json")
    #### note that "subkey1" should only appear in json on top-level blob
    final_blob = get_blob_meta_by_id(blob_id1)
    del final_blob['parts']
    final_blob_list = [final_blob]
    blobs = get_blob_meta_by_like_field("core.subkey1", "subvalue1%")
    compare_json(final_blob_list, blobs, "Search on search.subkey2 subvalue2%; make sure you start with an empty db")

    blobs = get_blob_meta_by_like_field("search.subkey1", "subvalue1%")
    compare_json(final_blob_list, blobs, "Search on search.subkey2 subvalue2%; make sure you start with an empty db")

    ################################################################
    print("\n\n#### full metadata of blob {}".format(blob_id1))

    pp_json(get_blob_meta_by_id(blob_id1))


    ################################################################
    print("\n\n#### test 12:  get parts by md5 hash match")
    md5 = get_blob_meta_by_id(blob_id1)['parts'][0]['md5Digest']
    md5match = get_parts_json_by_md5(md5)

    pp_json(md5match)


    ################################################################
    print("\n\n#### test 13:  put part by md5 hash match")

    add_part_md5(blob_id1, "added_by_md5_match", "yolo", md5)

