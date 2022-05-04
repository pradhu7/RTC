from pprint import pprint
import apxapi

# START FROM A JUST-INITIALIZED DB!

sess = apxapi.APXSession('root@api.apixio.com', password='thePassword', environment=apxapi.LOC)

################################################################ Create System/ROLE1 and System/ROLE2 for testing 'and' and 'or' in ACLs
resp = sess.useraccounts.post_role('System', 'ROLE1', 'Test role1', [ { 'forMember': True, 'operation': 'ManageOrganization', 'aclTarget': '*'} ])
resp = sess.useraccounts.post_role('System', 'ROLE2', 'Test role2', [ { 'forMember': True, 'operation': 'ManagePipeline', 'aclTarget': '*'} ])

################################################################ create System Org to put new users in for testing 'and' and 'or' in ACLs
resp = sess.useraccounts.post_uorg('TestOrg1', 'Test 1 organization', 'System', '001a')
orgID = resp.json()['id']
print "Created OrgID %s" % orgID

resp = sess.useraccounts.post_user('user1@localhost.com', orgID, True, False)
user1ID = resp.json()['id'];
print "Created UserID 1 %s" % user1ID
sess.useraccounts.put_user_active(user1ID, True)
sess.useraccounts.put_user_password(user1ID, 'mypass')

resp = sess.useraccounts.post_user('user2@localhost.com', orgID, True, False)
user2ID = resp.json()['id'];
print "Created UserID 2 %s" % user2ID
sess.useraccounts.put_user_active(user2ID, True)
sess.useraccounts.put_user_password(user2ID, 'mypass')

################################################################ Create Customer org
resp = sess.useraccounts.post_uorg('TestCustomer1', 'Test 1 customer organization', 'Customer', '002b')
custID = resp.json()['id']
print "Created CustomerOrgID %s" % custID

resp = sess.useraccounts.post_user('user3@customer.com', custID, True, False)
user3ID = resp.json()['id'];
print "Created Customer UserID 3 %s" % user3ID
sess.useraccounts.put_user_active(user3ID, True)
sess.useraccounts.put_user_password(user3ID, 'mypass')

resp = sess.useraccounts.post_user('user4@customer.com', custID, True, False)
user4ID = resp.json()['id'];
print "Created Customer UserID 4 %s" % user4ID
sess.useraccounts.put_user_active(user4ID, True)
sess.useraccounts.put_user_password(user4ID, 'mypass')

resp = sess.useraccounts.post_pds('thePDS', 'test pds', custID)
pdsID = resp.json()['id']
print "Created PdsID %s" % pdsID

resp = sess.useraccounts.post_project('theProj', 'test proj', 'HCC', custID, pdsID, '', '', '', 'initial', 'firstPass')
projID = resp.json()['id']
print "Created ProjID %s" % projID
