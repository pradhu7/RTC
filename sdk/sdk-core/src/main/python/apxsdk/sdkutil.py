import base64
import socket
import yaml
import json

import apxprotobufs.fx_pb2 as FxProtos_pb2
import apxprotobufs.PredictionBin_pb2 as PredictionBin_pb2
import apxprotobufs.Signals_pb2 as Signals_pb2
from google.protobuf.json_format import MessageToJson, ParseDict
import apxprotobufs.basetypes_pb2 as BaseTypes_pb2

# TODO should we pass these in via setup?
message_map = {
    "apixio.EventType": PredictionBin_pb2.PredictionBin,
    "apixio.Signal": Signals_pb2.Signal,
    "apixio.String": BaseTypes_pb2.FxString
}

base_types_map = {
    "FxString": "apixio.String"
}
cfg = {}

def message_to_byte_string(fxType):
    """Convert a proto object into a base64 encoded byte string """
    return base64.encodebytes(fxType.SerializeToString()).decode('ascii')


def byte_string_to_fxtype(typeBase64):
    """Convert base64 encoded byte string into an FxType proto object """
    fxt = FxProtos_pb2.FxType()
    return byte_string_to_message(fxt, typeBase64)


def byte_string_to_message(message, base64data):
    """Parse base64 encoded byte string into the supplied proto Message """
    messagebytes = bytes(base64data, "ascii")
    rawstring = base64.decodebytes(messagebytes)
    message.ParseFromString(rawstring)
    return message


def json_to_byte_string(atype, jsonData, expandListItems=True):
    """Convert json into a base64 encoded proto message using supplied type"""
    islist = atype.tag == FxProtos_pb2.FxTag.SEQUENCE
    if expandListItems and islist:
        atype = atype.sequenceInfo.ofType
        name = atype.containerInfo.name
        message = message_map.get(name)()
        return [_json_to_byte_string(message, el) for el in jsonData]
    if atype.tag == FxProtos_pb2.FxTag.CONTAINER:
        name = atype.containerInfo.name
        message = message_map.get(name)()
        return _json_to_byte_string(message, jsonData)
    raise NotImplementedError("json_to_byte_string currently works only on containers")


def _json_to_byte_string(message, jsonData):
    ParseDict(jsonData, message)
    return base64.encodebytes(message.SerializeToString()).decode('ascii')


def check_for_service(host="127.0.0.1", port=4500):
    location = ("127.0.0.1", port)
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    result_of_check = a_socket.connect_ex(location)
    return result_of_check == 0


def load_config(configfile):
    with open(configfile, "r") as stream:
        try:
            cfg.update(yaml.safe_load(stream))
        except yaml.YAMLError as exc:
            print(exc)
    print("config:")
    print(cfg)

def get_data_as_json(atype, data):
    if atype == "string":
        return data
    if atype == "float":
        return float(data)
    if atype == "int":
        return int(data)
    if atype == "json":
        return json.loads(data)
    if isinstance(atype, FxProtos_pb2.FxType):
        islist = atype.tag == FxProtos_pb2.FxTag.SEQUENCE
        if islist:
            atype = atype.sequenceInfo.ofType
            all = [get_data_as_json(atype, datum) for datum in data]
            if atype.containerInfo.name == "apixio.String":
                return "\n".join([a.get("stringValue") for a in all if a])

            return all
        elif atype.tag == FxProtos_pb2.FxTag.CONTAINER:
            name = atype.containerInfo.name
            message = message_map.get(name)()
            byte_string_to_message(message, data)
            return json.loads(MessageToJson(message))
    raise Exception(f"Data type {atype} is not supported by the SDK")


def produce_output(data, atype):
    if atype == "string":
        return data
    if atype == "float":
        return float(data)
    if atype == "int":
        return int(data)
    if atype == "json":
        return json.loads(data)
    if isinstance(atype, FxProtos_pb2.FxType):
        islist = atype.tag == FxProtos_pb2.FxTag.SEQUENCE
        if islist:
            atype = atype.sequenceInfo.ofType
            all = [produce_output(datum, atype) for datum in data]
            return all
        elif atype.tag == FxProtos_pb2.FxTag.CONTAINER:
            if atype.containerInfo.name == "apixio.String":
                return json_to_byte_string(atype, {"stringValue": data})
            return json_to_byte_string(atype, data)
    raise Exception(f"Data type {atype} is not supported by the SDK")

def split_eval_string(eval):
    elist = []
    parendiff = 0
    newel = ""
    for ch in eval:
        if ch == "(":
            parendiff += 1
        if ch == ")":
            parendiff -= 1
        if ch == "," and parendiff == 0:
            elist.append(newel)
            newel = ""
        else:
            newel += ch
    elist.append(newel)
    return elist

def extract_type(rtype):
    if "BaseTypesProtos" in rtype:  # TODO create a map or object
        if rtype.startswith("list"):
            innerType = rtype[len("list("):-1].split(".")[-1]
        mType = FxProtos_pb2.FxType()
        mType.tag = FxProtos_pb2.FxTag.SEQUENCE
        ofType = mType.sequenceInfo.ofType
        ofType.tag = FxProtos_pb2.FxTag.CONTAINER
        ofType.containerInfo.name = base_types_map.get(innerType) # TODO error catch
        ofType.containerInfo.isStruct = True
        return mType
    else:
        return byte_string_to_fxtype(rtype)


def parseFxDef(fxdefidl):
    returnType, rest = fxdefidl.split()
    functName, rest = rest.split("(")
    argtypes = []
    for arg in rest[:-1].split(","):
        mType = FxProtos_pb2.FxType()
        if arg.startswith("list"):
            innerType = arg[len("list("):-1]
            mType.tag = FxProtos_pb2.FxTag.SEQUENCE
            ofType = mType.sequenceInfo.ofType
            ofType.tag = FxProtos_pb2.FxTag.CONTAINER
            ofType.containerInfo.name = innerType
            ofType.containerInfo.isStruct = True
            # msgType = message_map.get(innerType)
        argtypes.append(mType)
    if returnType.startswith("list"):
        innerType = returnType[len("list("):-1]
        mType = FxProtos_pb2.FxType()
        mType.tag = FxProtos_pb2.FxTag.SEQUENCE
        ofType = mType.sequenceInfo.ofType
        ofType.tag = FxProtos_pb2.FxTag.CONTAINER
        ofType.containerInfo.name = innerType
        ofType.containerInfo.isStruct = True
        # msgType = message_map.get(innerType)
        return {"returnType": mType, "function": functName, "argTypes": argtypes}

    return {"returnType": message_map.get(returnType) or returnType, "islist": False, "function": functName}
