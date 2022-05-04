from pprint import pprint
import sys
import apxapi

def post_operation(session, name, description, access_types=[]):
    payload = {
        'name': name,
        'description': description,
        'accessTypes': access_types
    } 
    return session.post('%s/aclop' % session.useraccounts.url, json=payload)

sess = apxapi.APXSession('root@api.apixio.com', password='thePassword', environment=apxapi.LOC)
resp = post_operation(sess, 'ManageAclGroupMembership', 'Manage membership in ACL-maintained groups (meta acls, basically)')
if (resp.status_code != 200):
    print "Unable to create ManageAclGroupMembership operation!"
    pprint(resp.json())
    sys.exit()
else:
    print "Successfully created ManageAclGroupMembership operation"
resp = sess.useraccounts.post_role('Project.hcc', 'SUPERVISOR', 'Project Supervisor', [ { 'forMember': False, 'operation': 'ManageAclGroupMembership', 'aclTarget': 'acl-groupname'} ])
if (resp.status_code != 200):
    print "Unable to create Project.hcc/SUPERVISOR role!"
    pprint(resp.json())
    sys.exit()
else:
    print "Successfully created Supervisor role"


