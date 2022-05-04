
import apxapi
import json
import pprint

sess = apxapi.APXSession('smccarty@apixio.com', environment=apxapi.PRD)

# pdsID calculated elsewhere
pdsId = apxapi.user_accounts.to_xuuid('10001102')

uorgId = sess.useraccounts.get_pds(pdsId).json()['ownerOrg']
project_year=2021

proj_response = sess.useraccounts.post_prospective_project('CA/PV Test', 'CA/PV Test CNC', uorgId, pdsId, 1, [], project_year)
projectId = proj_response.json()['id']
print('Project ID is ', pprint.pformat(projectId))
