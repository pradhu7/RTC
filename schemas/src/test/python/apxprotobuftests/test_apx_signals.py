import unittest

from apxprotobufs.MessageMetadata_pb2 import XUUID
from apxprotobufs.Signals_pb2 import PatientSource, DocumentSource, Generator, Signal, SignalType
from google.protobuf.json_format import MessageToJson, Parse

PATIENT_ID = XUUID(type="PAT", uuid="f33be7b2-9c5e-11e9-a2a3-2a2ae2dbcce4")
DOCUMENT_ID = XUUID(type="DOC", uuid="fd88868f-f9ff-410a-985b-b31cb4da5717")
PATIENT_SOURCE = PatientSource(patientID=PATIENT_ID)
DOCUMENT_SOURCE = DocumentSource(patientID=PATIENT_ID, documentID=DOCUMENT_ID)

ASSUMED_DATE = Generator(name="AssumedDate", jarVersion="1.4.0", version="1.0.0")

SLIM_LYNTY = Generator(name="SlimLynty", jarVersion="1.4.0", version="1.1.0")

DOCUMENT_SIGNAL = Signal(
    generator=ASSUMED_DATE,
    name="AssumedDate",
    signalType=SignalType.CATEGORY,
    documentSource=DOCUMENT_SOURCE,
    categoryValue="2017-01-05T00:00:00.000Z",
    weight=1.0
)

PATIENT_SIGNAL = Signal(
    generator=SLIM_LYNTY,
    name="SlimLynty-V22_19",
    signalType=SignalType.CATEGORY,
    patientSource=PATIENT_SOURCE,
    categoryValue="proc:2016-10-04",
    weight=1.0
)


class TestCreateSignals(unittest.TestCase):

    def testDocumentSignalSerde(self):
        serialized = MessageToJson(DOCUMENT_SIGNAL)

        print("Serialized response_envelope is %s" % serialized)

        deserialized = Parse(serialized, Signal(), ignore_unknown_fields=True)

        self.assertEqual(deserialized, DOCUMENT_SIGNAL)

    def testPatientSignalSerde(self):
        serialized = MessageToJson(PATIENT_SIGNAL)

        print("Serialized response_envelope is %s" % serialized)

        deserialized = Parse(serialized, Signal(), ignore_unknown_fields=True)

        self.assertEqual(deserialized, PATIENT_SIGNAL)
