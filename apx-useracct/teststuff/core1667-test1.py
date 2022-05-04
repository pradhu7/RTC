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

def get_uorg_id(sess, orgName):
    for org in sess.useraccounts.get_uorg_all().json():
        if (org['name'] == orgName):
            return org['id']

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

user1ID = get_user_id(rootsess, 'user1@localhost.com')
user2ID = get_user_id(rootsess, 'user2@localhost.com')
orgID   = get_uorg_id(rootsess, 'TestOrg1')

test_info("user1=%s\nuser2=%s\norgID=%s\n\n" % (user1ID, user2ID, orgID))

# clean up from last run
resp = rootsess.useraccounts.delete_uorg_user_role(orgID, user1ID, 'ROLE1');
expect(200, resp, "Setup:  'delete_urog_user_role")
resp = rootsess.useraccounts.delete_uorg_user_role(orgID, user1ID, 'ROLE2');
expect(200, resp, "Setup:  'delete_urog_user_role")


################

sess1 = apxapi.APXSession('user1@localhost.com', password='mypass', environment=apxapi.LOC)

test_info("testing /junk with NO ROLE assigned")

resp = sess1.useraccounts.get_junk('protectedbyor/' + orgID)
expect(403, resp, "Test #1:  'protectedbyor/" + orgID + "'")

resp = sess1.useraccounts.get_junk('protectedbyand/' + orgID)
expect(403, resp, "Test #2:  'protectedbyand/" + orgID + "'")


################ now assign 1 role
test_info("Assigning ROLE1 to user %s for Org %s" % (user1ID, orgID))
resp = rootsess.useraccounts.put_uorg_user_role(orgID, user1ID, 'ROLE1');
expect(200, resp, "Setup:  'put_urog_user_role")

test_info("testing /junk with ROLE assigned")

resp = sess1.useraccounts.get_junk('protectedbyor/' + orgID)
expect(404, resp, "Test #3:  'protectedbyor/" + orgID + "'")

resp = sess1.useraccounts.get_junk('protectedbyand/' + orgID)
expect(403, resp, "Test #4:  'protectedbyand/" + orgID + "'")

################ now assign second role
rootsess.useraccounts.put_uorg_user_role(orgID, user1ID, 'ROLE2');

test_info("Testing /junk with ROLE assigned")

resp = sess1.useraccounts.get_junk('protectedbyor/' + orgID)
expect(404, resp, "Test #5:  'protectedbyor/" + orgID + "'")

resp = sess1.useraccounts.get_junk('protectedbyand/' + orgID)
expect(404, resp, "Test #6:  'protectedbyand/" + orgID + "'")
