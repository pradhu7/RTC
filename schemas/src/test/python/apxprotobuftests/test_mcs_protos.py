import unittest
from apxprotobufs.LambdaMessages_pb2 import AlgorithmType
from apxprotobufs.McMeta_pb2 import FullMeta, CodeMappingVersion
from google.protobuf.json_format import MessageToJson, Parse

MCS_ENSEMBLE_PROTO = r'''
{
  "core": {
    "algorithm": {
      "type": "ENSEMBLE"
    },
    "dependencies": [
      "V6:1.0.2/9.0.2-SMG4",
      "LogitFaceToFace:1.0.0/9.0.2-SMG4",
      "TextSuspects:1.0.0/9.0.2-SMG4",
      "InferredDate:1.0.0/9.0.2-SMG4",
      "CodeableSectionDict:1.1.0/9.0.2-SMG4",
      "DateIsAssumed:1.0.0/9.0.2-SMG4",
      "WordCount:1.0.0/9.0.2-SMG4",
      "AssumedDate:1.0.0/9.0.2-SMG4",
      "Junkiness:1.2.0/9.0.2-SMG4",
      "ConditionGuideLine:1.1.0/9.0.2-SMG4",
      "SlimLynty:1.1.1/9.0.2-SMG4",
      "PositionalTerm:1.0.0/9.0.2-SMG4",
      "Topic:1.1.1/9.0.2-SMG4",
      "V5:1.0.1/9.0.2-SMG4",
      "SVM:1.0.1/9.0.2-SMG4",
      "Dictionary:1.1.0/9.0.2-SMG4",
      "CharacterCount:1.0.0/9.0.2-SMG4",
      "HAR:1.1.0/9.0.2-SMG4",
      "FaceToFace:1.0.0/9.0.2-SMG4"
    ],
    "git": {
      "diff": "",
      "hash": "91cbedeb5349f146b722d0dada297975e83747da",
      "log": "91cbedeb5349f146b722d0dada297975e83747da Merge pull request #501 from Apixio/update_ensemble_1.7.7\n",
      "tag": "apx-signalmgradmin-quality_v0.0.1-663-g91cbedeb"
    },
    "name": "MA-V22:1.7.7/9.0.2-SMG4",
    "version": {
      "modelVersion": "9.0.2-SMG4",
      "name": "MA-V22",
      "snapshot": false,
      "version": "1.7.7"
    },
    "mappingVersion": "CANONICAL"
  },
  "createdAt": "2019-09-24T18:56:43Z",
  "createdBy": "twang@apixio.com",
  "deleted": false,
  "executor": "",
  "id": "B_1c73857a-c261-4f83-9517-c586b2c4a451",
  "name": "v22_1.7.7_9.0.2-SMG4",
  "outputType": "",
  "parts": [
    {
      "core": null,
      "createdAt": "2019-09-24T18:56:43Z",
      "createdBy": "twang@apixio.com",
      "md5Digest": "3719AA2B83BB2CFA5E1581B7973F2B64",
      "mimeType": "application/x-yaml; charset=UTF-8",
      "modelId": "B_1c73857a-c261-4f83-9517-c586b2c4a451",
      "name": "config.yaml",
      "s3Path": "s3://apixio-smasmodels/modelblobs/B_1c73857a-c261-4f83-9517-c586b2c4a451/config.yaml",
      "search": null
    },
    {
      "core": {
        "uri": "https://repos.apixio.com/artifactory/models/models/production_model/9.0.2/production_model_9_0_2.zip"
      },
      "createdAt": "2019-09-24T18:56:45Z",
      "createdBy": "twang@apixio.com",
      "md5Digest": "10E429266D23037B11FA0B1A5BC547E6",
      "mimeType": "application/zip",
      "modelId": "B_1c73857a-c261-4f83-9517-c586b2c4a451",
      "name": "model.zip",
      "s3Path": "s3://apixio-smasmodels/modelblobs/B_729f81a3-d26b-4245-bde9-32a803a42d2d/model.zip",
      "search": null
    },
    {
      "core": {
        "uri": "https://repos.apixio.com/artifactory/apixio-release-snapshot-jars/apixio/apixio-ensemble-benchmarks_2.11/1.7.7/apixio-ensemble-benchmarks_2.11-1.7.7-assembly.jar"
      },
      "createdAt": "2019-09-24T18:56:47Z",
      "createdBy": "twang@apixio.com",
      "md5Digest": "C2C3FD2FF378BC484DD31B4E1D135A17",
      "mimeType": "application/java-archive",
      "modelId": "B_1c73857a-c261-4f83-9517-c586b2c4a451",
      "name": "implementation.jar",
      "s3Path": "s3://apixio-smasmodels/modelblobs/B_eee97abf-0e09-43ac-8efc-f0ba289d609f/implementation.jar",
      "search": null
    }
  ],
  "pdsId": null,
  "product": "",
  "search": {
    "engine": "MA",
    "tags": [
      "COMBINER",
      "SMG",
      "COMPARE_SMUGGLER_1.6"
    ],
    "variant": "V22"
  },
  "state": "RELEASED",
  "version": null
}
'''


class TestMcMetaSerde(unittest.TestCase):

  def testEnsembleDeserialization(self):
    deserialized = Parse(MCS_ENSEMBLE_PROTO, FullMeta(),
                         ignore_unknown_fields=True)

    # Verify that we can get tne MC_ID
    self.assertEqual(deserialized.id, "B_1c73857a-c261-4f83-9517-c586b2c4a451")

    # Verify that we can get the algorithm type
    self.assertEqual(deserialized.core.algorithm.type, AlgorithmType.ENSEMBLE)

    # Verify that we can get the dependency list
    self.assertEqual(len(deserialized.core.dependencies), 19)

    # Verify that we can get the logical id
    self.assertEqual(deserialized.core.name, "MA-V22:1.7.7/9.0.2-SMG4")

    # Verify that we can get the version
    self.assertEqual(deserialized.core.version.version, "1.7.7")
    self.assertEqual(deserialized.core.version.modelVersion, "9.0.2-SMG4")

    # Verify that we can get the parts
    parts = deserialized.parts

    self.assertEqual(len(parts), 3)

    self.assertEqual(parts[0].name, "config.yaml")
    self.assertEqual(parts[0].s3Path,
                     "s3://apixio-smasmodels/modelblobs/B_1c73857a-c261-4f83-9517-c586b2c4a451/config.yaml")

    self.assertEqual(parts[1].core.uri,
                     "https://repos.apixio.com/artifactory/models/models/production_model/9.0.2/production_model_9_0_2.zip")

    # Verify that we can get the mapping version
    self.assertEqual(deserialized.core.mappingVersion, CodeMappingVersion.CANONICAL)
    serialized = MessageToJson(deserialized)
    deserialized2 = Parse(serialized, FullMeta(), ignore_unknown_fields=True)

    self.assertEqual(deserialized, deserialized2)
