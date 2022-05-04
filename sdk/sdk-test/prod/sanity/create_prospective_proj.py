
import apxapi
import json

sess = apxapi.APXSession('smccarty@apixio.com', environment=apxapi.PRD)

# 10001062 calculated elsewhere
pdsId = apxapi.user_accounts.to_xuuid('10001062')

uorgId = sess.useraccounts.get_pds(pdsId).json()['ownerOrg']
project_year=2021

proj_response = sess.useraccounts.post_prospective_project('CA/PV Test', 'CA/PV Test', uorgId, pdsId, 1, [], project_year)
projectId = proj_response.json()['id']
pprint('Project ID is  {}'.format(projectId))
