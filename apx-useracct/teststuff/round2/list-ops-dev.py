from pprint import pprint
import sys
import apxapi

sess = apxapi.APXSession('smccarty@apixio.com', environment=apxapi.DEV)
resp = sess.useraccounts.get_operations()

pprint(resp.json())
