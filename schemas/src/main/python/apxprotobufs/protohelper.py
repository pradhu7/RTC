import socket
from uuid import uuid4
from enum import Enum
import random
from google.protobuf.timestamp_pb2 import Timestamp
from apxprotobufs.MessageMetadata_pb2 import MessageHeader, MessageType, XUUID, SenderID

# TODO : jos determin if these should move these types somewhere global
class XUUIDTypes(Enum):
    MESSAGE  = "M"
    SENDERID = "MS"
    SIGNAL   = "S"
    PATIENT  = "PAT"
    DOCUMENT = "DOC"
    MODEL    = "MDL"
    DOCSET   = "DS"

    def __str__(self):
        return self.value

# TODO : jos, these should be created from source somewhere...
class SenderIDs(Enum):
    SIGGEN  = SenderID(name="SigGen", version="834ae096-7ed8-45b5-a3ae-5333cf742c4f")
    CEREBRO = SenderID(name="Cerebro", version="e8d84163-f8d4-4df5-bb52-968c58e1b3ef")
    LAMBDA  = SenderID(name="AlgoCloud", version="81f3dade-c36f-4450-8984-84bd9acfc2b1")
    APPMGMT = SenderID(name="AppMgmt", version="5717c2a6-4082-4153-aa14-15bc0f3e132b")


def make_message_header(msg_type: MessageType, sender_id: SenderID) -> MessageHeader:
    timestamp = Timestamp()
    timestamp.GetCurrentTime()
    msg_xuuid = XUUID(type=XUUIDTypes.MESSAGE.value, uuid=str(uuid4()))

    # note: MYPY users this is not going pass muster with mypy due to the means
    #  by which protobuf creates objects
    return MessageHeader(
        messageID = msg_xuuid, messageType = msg_type,
        senderID = sender_id, hostFqdn = socket.getfqdn(),
        messageDateTime = timestamp)

