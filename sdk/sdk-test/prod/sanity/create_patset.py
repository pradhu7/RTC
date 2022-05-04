
import apxapi
import json

def create_query_params(**kwargs):
    res = dict([(x, y) for x, y in kwargs.items() if y is not None])
    return res if res else None

sess = apxapi.APXSession('smccarty@apixio.com', environment=apxapi.PRD)

# pdsId determined manually by looking at data in stjoes_LA_pv_20210108_part_6.csv
# projectId created in create_prospective_proj.py
# capv_mcid created by MC publishing

pdsId = apxapi.user_accounts.to_xuuid('10001062')
projectId = 'PRPROSPECTIVE_53f85a75-58ae-4cd7-90c9-4994951beb27'
capv_mcid = 'B_5e6b747c-5617-4592-8f0f-ab20336f5584'

# get userOrg (customer) from pdsId
uorgId = sess.useraccounts.get_pds(pdsId).json()['ownerOrg']

# create list of patient UUIDs from master list
pat_list = []
patids = open('stjoes_LA_pv_20210108_part_6.csv', 'r')
for patuuid in patids:
    pat_list.append(patuuid.rstrip())

patids.close()

payload = create_query_params(projectDataSetName="capv_prod_sanity_1",    #<< must be unique
                              customerUuid=uorgId,
                              projectUuid=projectId,
                              pdsId=pdsId,
                              models=[{"engine": "CAPV", "variant": "V1", "mcid":capv_mcid}],
                              creator="smccarty@apixio.com",
                              pdsType="PAT_SET",
                              patientList=pat_list,
                              criteriaType="PATIENT_LIST"
                             )
headers = {'content-type': 'application/json'}

# this should kick off capv(x) evaluation
res = sess.post('%s/projdatasets/V2' % sess.smas.url, data=json.dumps(payload), headers=headers)

print(res.text)
