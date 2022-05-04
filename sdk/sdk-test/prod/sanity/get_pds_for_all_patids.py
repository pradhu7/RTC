
import apxapi
import json

sess = apxapi.APXSession('smccarty@apixio.com', environment=apxapi.PRD)

patids = open('stjoes_LA_pv_20210108_part_6.csv', 'r')

for patuuid in patids:
    patuuid = patuuid.rstrip()
    print('Patient ', patuuid, '; pds ', sess.dataorchestrator.patient_org_id(patuuid).text)

patids.close()
