
import apxapi
import json

def create_query_params(**kwargs):
    res = dict([(x, y) for x, y in kwargs.items() if y is not None])
    return res if res else None

sess = apxapi.APXSession('smccarty@apixio.com', environment=apxapi.PRD)

# pdsId determined manually by looking at data in cnc21_jan_capv_pilot.csv
# projectId created in create_prospective_proj-cnc.py
# capv_mcid created by MC publishing

pdsId = apxapi.user_accounts.to_xuuid('10001102')
projectId = 'PRPROSPECTIVE_d78e87b1-694e-4e4d-9055-5ab17fd235b8'
capv_mcid = 'B_53f01c0d-b1ca-4646-b5d6-d5c0f6c96fd9'

# get userOrg (customer) from pdsId
uorgId = sess.useraccounts.get_pds(pdsId).json()['ownerOrg']

# create list of patient UUIDs from master list
pat_list = []
patids = open('cnc21_jan_capv_pilot.csv', 'r')
for patuuid in patids:
    pat_list.append(patuuid.rstrip())

patids.close()

payload = create_query_params(projectDataSetName="capv_prod_sanity_cnc_8",    #<< must be unique
                              customerUuid=uorgId,
                              projectUuid=projectId,
                              pdsId=pdsId,
                              models=[{"engine": "CAPV", "variant": "V1", "mcid":capv_mcid}],
                              creator="dyee@apixio.com",
                              pdsType="PAT_SET",
                              patientList=pat_list,
                              criteriaType="PATIENT_LIST"
                             )
headers = {'content-type': 'application/json'}

# this should kick off capv(x) evaluation
res = sess.post('%s/projdatasets/V2' % sess.smas.url, data=json.dumps(payload), headers=headers)

print(res.text)
