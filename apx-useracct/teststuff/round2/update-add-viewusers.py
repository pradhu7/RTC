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

sess = apxapi.APXSession('smccarty@apixio.com',  environment=apxapi.DEV)

resp = post_operation(sess, 'ViewUsers', 'View users')

if (resp.status_code != 200):
    print "Unable to create ViewUsers operation!"
    pprint(resp.json())
    sys.exit()
else:
    print "Successfully created ViewUsers operation"

