import unittest
from apxprotobufs.FFS_pb2 import FFSClaim, AxmEditType
from google.protobuf.json_format import MessageToJson, Parse

FFS_CLAIM_PROTO = r'''
{
  "patientId": "J1207928302",
  "originalId": "180130E08709",
  "providers": [
    {
      "NPI": "1801869359"
    }
  ],
  "startDate": {
    "year": 2018,
    "month": 4,
    "day": 1
  },
  "endDate": {
    "year": 2018,
    "month": 4,
    "day": 1
  },
  "code": {
    "system": "CPT-4",
    "systemOid": "2.16.840.1.113883.6.12",
    "code": "J3420"
  },
  "diagnosisCodes": [
    {
      "system": "ICD10-CM",
      "systemOid": "2.16.840.1.113883.6.90",
      "code": "E538"
    }
  ],
  "billing": {
    "providerId": "",
    "provdiderName": "",
    "providerType": "2.25.986811684062365523470895812567751821389",
    "billType": "2.25.726424807521776741141709914703687743312",
    "transactionDate": {
      "year": 2019,
      "month": 4,
      "day": 17
    }
  },
  "editType": "ACTIVE",
  "deleteIndicator": false
}

'''


class TesstFFSClaimSerde(unittest.TestCase):

  def testFFSClaimDeserialization(self):
    deserialized = Parse(FFS_CLAIM_PROTO, FFSClaim(),
                         ignore_unknown_fields=True)

    # Verify that we can get the patientId
    self.assertEqual(deserialized.patientId, "J1207928302")

    #Verify that we can get the claimsId
    self.assertEqual(deserialized.originalId, "180130E08709")

    #Verify that we can get the providers
    self.assertEqual(deserialized.providers[0].NPI, "1801869359")

    #Verify that we can get the startDate
    startDate = deserialized.startDate
    self.assertEqual(startDate.year, 2018)
    self.assertEqual(startDate.month, 4)
    self.assertEqual(startDate.day, 1)

    #Verify that we can get the endDate
    endDate = deserialized.endDate
    self.assertEqual(endDate.year, 2018)
    self.assertEqual(endDate.month, 4)
    self.assertEqual(endDate.day, 1)

    #Verify that we can get the code
    code = deserialized.code
    self.assertEqual(code.system, "CPT-4")
    self.assertEqual(code.systemOid, "2.16.840.1.113883.6.12")
    self.assertEqual(code.code, "J3420")

    #Verify that we can get the diagnosis code
    codes = deserialized.diagnosisCodes
    code = codes[0]
    self.assertEqual(code.system, "ICD10-CM")
    self.assertEqual(code.systemOid, "2.16.840.1.113883.6.90")
    self.assertEqual(code.code, "E538")

    #Verify that we can get the billing
    billing = deserialized.billing
    self.assertEqual(billing.providerId, "")
    self.assertEqual(billing.provdiderName, "")
    self.assertEqual(billing.providerType, "2.25.986811684062365523470895812567751821389")
    self.assertEqual(billing.billType, "2.25.726424807521776741141709914703687743312")

    transactionDate = billing.transactionDate
    self.assertEqual(transactionDate.year, 2019)
    self.assertEqual(transactionDate.month, 4)
    self.assertEqual(transactionDate.day, 17)

    #Verify that we can get the editType
    self.assertEqual(deserialized.editType, AxmEditType.ACTIVE)

    #Verify that we can get the deleteIndicator
    self.assertEqual(deserialized.deleteIndicator, False)




