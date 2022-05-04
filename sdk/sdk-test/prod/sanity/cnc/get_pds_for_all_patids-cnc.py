
import apxapi
import json

sess = apxapi.APXSession('smccarty@apixio.com', environment=apxapi.PRD)

patids = open('cnc21_jan_capv_pilot.csv', 'r')

for patuuid in patids:
    patuuid = patuuid.rstrip()
    print('Patient ', patuuid, '; pds ', sess.dataorchestrator.patient_org_id(patuuid).text)

patids.close()
