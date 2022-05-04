from pprint import pprint
import sys
import apxapi

sess = apxapi.APXSession('smccarty@apixio.com', environment=apxapi.DEV)
resp = sess.useraccounts.get_system_roles()

project_role = [x for x in resp.json() if x['nameID'] == 'Project.hcc'][0]
supervisor   = [x for x in project_role['roles'] if x['name'] == 'SUPERVISOR'][0]

privs = supervisor['privileges']
privs.append({u'aclTarget':u'target-projorg', u'forMember':False, u'operation':u'ViewUsers'})

pprint(privs)

sess.useraccounts.put_role('Project.hcc', 'SUPERVISOR', privs)
