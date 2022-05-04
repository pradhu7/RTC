from pprint import pprint
import sys
import apxapi

################################################################
#
# Note that this script REQUIRES that there be no caching on ACLs
# or it will fail (since it changes ACLs (assign/remove) quicker
# than the default cache tiemout).  Make sure that user-account.yaml
# has the following:
#
#   aclHpCacheTimeout: 0
#
# It also requires that the apiacls.json used has declarations for
# /junk/... that use 'or' and 'and' constructs.

def get_user_id(sess, email):
    for user in sess.useraccounts.get_user_all().json():
        if (user['emailAddress'] == email):
            return user['id']

def get_uorg_id(sess, name):
    for org in sess.useraccounts.get_uorg_all().json():
        if (org['name'] == name):
            return org['id']

def get_proj_id(sess, name):
    for prj in sess.useraccounts.get_projects_all().json():
        if (prj['name'] == name):
            return prj['id']

def test_info(message):
    print "INFO:    %s" % message

def expect(expected, response, description):
    if (expected != response.status_code):
        print "FAILED:  %s.  Expected %d but got %d" % (description, expected, response.status_code)
        sys.exit()
    else:
        print "PASSED:  " + description

################################################################
rootsess = apxapi.APXSession('root@api.apixio.com', password='thePassword', environment=apxapi.LOC)

user3ID = get_user_id(rootsess, 'user3@customer.com')    # will be supervisor
user4ID = get_user_id(rootsess, 'user4@customer.com')    # won't be supervisor
orgID   = get_uorg_id(rootsess, 'TestCustomer1')
prjID   = get_proj_id(rootsess, 'theProj')

test_info("user3=%s\user4=%s\ncustID=%s\nprojID=%s\n" % (user3ID, user4ID, orgID, prjID))

# clean up from last run
resp = rootsess.useraccounts.delete_project_user_role(prjID, user3ID, 'SUPERVISOR');
expect(200, resp, "Setup:  delete_urog_user_role")


################ user3 login

sess3 = apxapi.APXSession('user3@customer.com', password='mypass', environment=apxapi.LOC)

test_info("testing /junk/proj with NO ROLE assigned")

resp = sess3.useraccounts.get_junk('proj/' + prjID)
expect(403, resp, "Test #1:  'proj/" + prjID + "'")

################ user4 login

sess4 = apxapi.APXSession('user4@customer.com', password='mypass', environment=apxapi.LOC)

resp = sess4.useraccounts.get_junk('proj/' + prjID)
expect(403, resp, "Test #2:  'proj/" + prjID + "'")



################ now assign 1 role
test_info("Assigning ROLE1 to user %s for Org %s" % (user3ID, orgID))
resp = rootsess.useraccounts.put_project_user_role(prjID, user3ID, 'SUPERVISOR');
expect(200, resp, "Setup:  'put_project_user_role")


################ should work now
test_info("testing /junk/proj with ROLE assigned")

resp = sess3.useraccounts.get_junk('proj/' + prjID)
expect(404, resp, "Test #3:  'proj/" + prjID + "'")


################ 

