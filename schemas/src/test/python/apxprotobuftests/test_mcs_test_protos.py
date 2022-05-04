import unittest
from apxprotobufs.McMeta_pb2 import FullMeta, CoreMeta
from apxprotobufs.McTestMeta_pb2 import TestScope, TestDataType, COMBINER_UNIT
from google.protobuf.json_format import MessageToJson, Parse

MCS_ENSEMBLE_PROTO = r'''
{
  "core": {
    "modelTest": {
      "scope": "COMBINER_UNIT"
    },
    "name": "ExampleCombinerUnitTest",
    "version": {
      "version": "1.0.0"
    }
  },
  "createdAt": "2019-09-24T18:56:43Z",
  "createdBy": "twang@apixio.com",
  "deleted": false,
  "executor": "",
  "id": "B_ea607247-1085-4ce8-8683-edd1de17d3d3 ",
  "name": "ExampleCombinerUnitTest",
  "outputType": "",
  "parts": [
    {
      "core": {
        "testPartMeta": {
          "dataType": "SIGNALS"
        }
      },
      "createdAt": "2019-09-24T18:56:43Z",
      "createdBy": "twang@apixio.com",
      "md5Digest": "3719AA2B83BB2CFA5E1581B7973F2B64",
      "mimeType": "application/x-yaml; charset=UTF-8",
      "modelId": "B_ea607247-1085-4ce8-8683-edd1de17d3d3 ",
      "name": "DOC_1_SIGNALS",
      "s3Path": "s3://apixio-smasmodels/modelblobs/signal_file_1.json.gz",
      "search": null
    },
    {
      "core": {
        "testPartMeta": {
          "dataType": "SIGNALS"
        }
      },
      "createdAt": "2019-09-24T18:56:45Z",
      "createdBy": "twang@apixio.com",
      "md5Digest": "10E429266D23037B11FA0B1A5BC547E6",
      "mimeType": "application/zip",
      "modelId": "B_ea607247-1085-4ce8-8683-edd1de17d3d3 ",
      "name": "DOC_2_SIGNALS",
      "s3Path": "s3://apixio-smasmodels/modelblobs/signal_file_2.json.gz",
      "search": null
    }
  ],
  "pdsId": null,
  "product": "",
  "search": {
    "tags": [
      "COMBINER_UNIT_TEST"
    ]
  },
  "state": "RELEASED",
  "version": null
}
'''

UNIT_TEST_CORE = r'''
{
  "name": "FIRST_TEST",
  "version": {
    "name": "FIRST_TEST",
    "version": "0.0.1",
    "modelVersion": "",
    "snapshot": false
  },
  "modelTest": {
    "scope": "COMBINER_UNIT"
  },
  "dependencies": []
}
'''

UNIT_TEST_FULL_META = r'''
{
  "id":"B_4f327a91-9b7b-4e52-92c7-f1534593b2e7",
  "createdAt":"2019-10-21T19:34:42Z",
  "deleted":false,
  "createdBy":"twang@apixio.com",
  "executor":"",
  "name":"FIRST_TEST",
  "outputType":"",
  "product":"",
  "version":null,
  "state":"DRAFT",
  "pdsId":null,
  "core":{
    "name":"FIRST_TEST",
    "modelTest":{
      "scope":"COMBINER_UNIT"
    },
    "version":{
      "modelVersion":"",
      "name":"FIRST_TEST",
      "version":"0.0.1",
      "snapshot":false
    },
    "dependencies":[

    ]
  },
  "search":{
    "engine":"MC_TEST",
    "variant":"UNIT",
    "tags":[

    ]
  },
  "parts":[

  ]
}
'''


class TestModelTestSerde(unittest.TestCase):

  def testModelTestDeserialization(self):
    deserialized = Parse(MCS_ENSEMBLE_PROTO, FullMeta(),
                         ignore_unknown_fields=True)

    # Verify that we can get the test type
    self.assertEqual(deserialized.core.modelTest.scope, TestScope.COMBINER_UNIT)

    # Verify that we can get the parts
    parts = deserialized.parts

    self.assertEqual(len(parts), 2)

    self.assertEqual(parts[0].name, "DOC_1_SIGNALS")
    self.assertEqual(parts[0].s3Path,
                     "s3://apixio-smasmodels/modelblobs/signal_file_1.json.gz")
    self.assertEqual(parts[0].core.testPartMeta.dataType, TestDataType.SIGNALS)

    self.assertEqual(parts[1].name, "DOC_2_SIGNALS")
    self.assertEqual(parts[1].s3Path,
                     "s3://apixio-smasmodels/modelblobs/signal_file_2.json.gz")
    self.assertEqual(parts[1].core.testPartMeta.dataType, TestDataType.SIGNALS)

    serialized = MessageToJson(deserialized)
    deserialized2 = Parse(serialized, FullMeta(), ignore_unknown_fields=True)

    self.assertEqual(deserialized, deserialized2)

  def testUnitTestCoreDeserialization(self):
    core_meta = Parse(UNIT_TEST_CORE, CoreMeta(),
                      ignore_unknown_fields=True)

    print(MessageToJson(core_meta, including_default_value_fields=True))
    self.assertEqual(core_meta.modelTest.scope, COMBINER_UNIT)

  def testUnitTestFullMetaDeserialization(self):
    full_meta = Parse(UNIT_TEST_FULL_META, FullMeta(),
                      ignore_unknown_fields=True)

    print(MessageToJson(full_meta, including_default_value_fields=True))
    self.assertEqual(full_meta.core.modelTest.scope, COMBINER_UNIT)
