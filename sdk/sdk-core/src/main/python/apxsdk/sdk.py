"""This is the main class for Apixio's python SDK.

This SDK relies on a language-independent F(x) definition language. See the README.md for details.

The SDK assumes that you have a single initialization function that prepares your application to receive
an infinite number of F(x) invokation calls afterward.

The initialization function receives a set of key/value pairs. The 'value' will either be a constant or
a pointer to a local file path to more complex resoures (e.g., learned models). These resources are defined
during the "Publish" step when registering your F(x). (See README.md).

There are several ways to connect this SDK to your code, all of which supply this information:
1. A port to which the SDK should bind and listen for invokation messages
2. An fxname that is used to identify this fx (should match the Fx definition)
3. A 'set_assets' (a.k.a. setup) function that receives a set of key/value pairs needed to initialize your application.
4. A 'fx' function that does the real work.

Each F(x) function can only receive and emit pre-defined data types, which are either constants or protobuf messages
(passed around in JSON/dict form). The execution system will ensure that all input data can be converted to this form.
However, you are responsible for ensuring the F(x) output adhere's to this constraint. See FxTest for a way to test
this aspect.

To allow the port to be supplied by the system (#1 above), you must ensure that your application does not accept
command line parameters. This SDK class will extract the port (and other info) from the command line.

There are several ways to supply items 2-4 above, all of which require importing the sdk:
`from apxsdk import sdk`

1. Direct registration
    def mypredictor_set_assets_fn(arg1, arg2):
        .....

    sdk.register_set_assets("mypredictor", mypredictor_set_assets_fn)

    def mypredictor_fx(arg1, arg2):
        .....

    sdk.register_fx("mypredictor", mypredictor_fx)

2. Function annotations
    @sdk.register_set_assets("mypredictor")
    def mypredictor_set_assets_fn(arg1, arg2):
        .....


    @sdk.register_fx("mypredictor")
    def mypredictor_fx(arg1, arg2):
        .....

3. Class annotations

    class MyPredictorsBase:

        @sdk.register_fxclass
        def __init__(self):
            ...

        def name(self) -> str:
            return "mypredictor"

        def set_assets(self, c1, c2) -> str:
            ...

        def invoke(self, arg1, arg2) -> str:
            ...


"""
import inspect
import logging
import os
import sys
import time

import apxprotobufs.eval_pb2 as Eval_pb2
import apxprotobufs.fx_pb2 as FxProtos_pb2
import apxprotobufs.Signals_pb2 as Signals_pb2
import awswrangler as wr
import boto3
import graypy
import json
import requests
from collections import defaultdict
from flask import Blueprint, jsonify, request

from apxsdk.sdkutil import (byte_string_to_fxtype, cfg, parseFxDef, 
                            check_for_service, json_to_byte_string, 
                            load_config, get_data_as_json, split_eval_string)
from apxsdk.fxdao import FxDAO


def s3connect():
    s3cfg = cfg["storageConfig"]["s3Config"]
    session = boto3.Session(aws_access_key_id=s3cfg["accessKeyUnencrypted"],
                            aws_secret_access_key=s3cfg["secretKeyUnencrypted"])
    return session

from . import create_app

sdkbp = Blueprint('sdkbp', __name__, url_prefix='/apxsdk')
logger = logging.getLogger('pysdk_logger')

# TODO create an object for all of these elements and just have one registry
class RegEntry():
    fx_def = None
    asset_map = None
    fx = None
    set_assets = None
    eval = None

registry = defaultdict(RegEntry)
# asset_registry = {}
# assetmap_registry = {}
# fx_registry = {}
# fxdef_registry = {}
# eval_registry = {}


class ApxSdk:
    port = 4500
    processes = 1

    def __init__(self, port=None) -> None:
        if port:
            self.port = port
        if len(sys.argv) > 1:
            self.port = int(sys.argv[1])
        if len(sys.argv) > 2:
            self.processes = int(sys.argv[2])
        if len(sys.argv) > 3:
            load_config(sys.argv[3])

        logger.info(f"Initializing SDK service for port {self.port} and {self.processes} processes")
                
    def init_app(self, existingapp=None, prerun_cb=None):
        if existingapp is None:
            app = create_app()
        else:
            app = existingapp
        app.register_blueprint(sdkbp)
        if existingapp is None:
            logger.info(f"Starting SDK service on port {self.port}")
            if prerun_cb:
                logger.info("Calling Pre-run callback")
                prerun_cb(app)
            app.run(port=self.port, host='0.0.0.0', debug=False, processes=self.processes)
        return app
    
def init_app(existingapp=None, prerun_cb=None):
    return ApxSdk().init_app(existingapp, prerun_cb)


# @sdkbp.route('/init', methods=["POST"])
# def init_environment():
#     all = requestasjson(request)
#     logger_url = all.get("logger_url")
#     logger_port = all.get("logger_port")
#     add_graylog_handler(logger_url, logger_port)


def add_graylog_handler(logger_url, logger_port):
    handler = graypy.GELFTLSHandler(logger_url, logger_port)
    logger.addHandler(handler)
    logger.info(f"Initializing SDK service graylog logger: {logger_url}, {logger_port}")

@sdkbp.route('/initialize', methods=["POST"])
def init_node():
    logger.info("Enter Initialize")
    all = requestasjson(request)
    # Connect to modelcatalog to download all resources
    fxDef, assets, fxname, eval = pull_from_modelcatalog(all)
    # Set assets
    _set_assets(fxname, assets)
    # Register fxdef
    if fxDef:
        registry[fxname].fx_def = fxDef
        # fxdef_registry[fxname] = fxDef
    if eval:
        registry[fxname].eval = eval
    logger.info(f"{fxname}::Initialization success.")
    return jsonify({"status": "success"})

@sdkbp.route('/setassets', methods=["POST"])
def set_assets():
    logger.info("Enter Set assets")
    fxname, assets = extract_setassets_data(request)
    return _set_assets(fxname, assets)

def _set_assets(fxname, assets):
    logger.info(f"{fxname}::Set assets")
    safunct = registry[fxname].set_assets
    argspec = inspect.getfullargspec(safunct)
    stripped_assets = {k: assets.get(k) for k in argspec[0] if k != "self" }
    safunct(**stripped_assets)
    logger.info(f"{fxname}::Set assets success")
    return jsonify({"status": "success"})


@sdkbp.route('/invokefx', methods=["POST"])
def invokefx():
    logger.info("Enter invokefx")
    fxname, arglist, outputtype = extract_fxinvoke_data(request)
    logger.info(f"{fxname}::invokefx")
    fxfunct = registry[fxname].fx
    result = fxfunct(*arglist)
    logger.info(f"{fxname}::invokefx success")
    output = produce_output(result, outputtype)
    logger.info(f"{fxname}::invokefx gen output success")
    return jsonify(output)

@sdkbp.route('/execute', methods=["POST"])
@sdkbp.route('/execute/<fxname>', methods=["POST"])
def execute(fxname=None):
    logger.info("Enter execute fx")
    fxname2, requestmap = extract_execute_data(request)
    if fxname2:
        fxname = fxname2
    if not fxname:
        if len(registry) != 1:
            raise Exception("'fxname' needs to be supplied when more than one fx is initialized")
        fxname = list(registry.keys)[0]
    requestmap["algo"] = fxname
    logger.info(f"{fxname}::execute")
    fxdao = FxDAO()

    arglist = pull_arglist(fxdao, fxname, requestmap)
    logger.info(f"{fxname}::execute")
    fxfunct = registry[fxname].fx
    fxdef = registry[fxname].fx_def
    result = fxfunct(*arglist)
    logger.info(f"{fxname}::execute success")
    data_uri = fxdao.generateUri(requestmap)
    success = fxdao.persist(fxdef, data_uri, result)
    return jsonify({"resultcount": len(result), "success": success, "dataUri": data_uri})

def register_assetmap(fxname, amap={}):
    registry[fxname].asset_map = amap


def register_set_assets(fxname, funct=None):
    logger.info(f"{fxname}::register_set_assets")
    if funct:
        logger.info(f"{fxname}::register_set_assets:function provided manually")
        registry[fxname].set_assets = funct
    
    def sa(f):
        logger.info(f"{fxname}::register_set_assets:function provided automatically")
        registry[fxname].set_assets = f
        return f
    return sa


def register_fx(fxname, funct=None):
    if funct:
        logger.info(f"{fxname}::register_fx:function provided manually")
        registry[fxname].fx = funct

    def fxwrap(f):
        logger.info(f"{fxname}::register_fx:function provided automatically")
        registry[fxname].fx = f
        return f
    return fxwrap


def register_fxdef(fxname, fxdef):
    logger.info(f"{fxname}::register_fxdef:function provided manually:{fxdef}")
    registry[fxname].fx_def = fxdef
    
    def fxwrap(f):
        registry[fxname].fx_def = fxdef
    return fxwrap

def register_fxclass(fxobj):
    """
    Add this annotation to the __init__ function of a class that has 3 functions:
        - name: the fxname used when registering this Fx
        - set_assets: used to receive the the init parameters for the function
        - invoke: main Fx function
    """
    def method_wrapper(self, *args, **kwargs):
        if not self.set_assets or not self.invoke:
            errortext = """register_fxclass is only valid on an __init__ function for a class with 3 functions: 
                            name, set_assets and invoke."""
            logger.error(errortext)
            if not self.set_assets:
                logger.error("Missing set_assets function")
            if not self.invoke:
                logger.error("Missing invoke function")
            if not self.name:
                logger.error("Missing name function")
            raise NotImplementedError(errortext)
        registry[self.name].set_assets = self.set_assets
        registry[self.name].fx = self.invoke
    return method_wrapper


def register_pyensemble_fxclass(fxobj):
    """
    Add this annotation to the __init__ function of a class that has 3 functions:
        - get_generator_name: the fxname used when registering this Fx
        - setup: used to receive the the init parameters for the function
        - predict: main Fx function
    """
    def method_wrapper(self, *args, **kwargs):
        if not self.setup or not self.get_generator_name or not self.predict:
            errortext = """register_pyensemble_fxclass is only valid on an __init__ function for a class with 3 functions:
                         get_generator_name, setup, and predict."""
            logger.error(errortext)
            if not self.get_generator_name:
                logger.error("Missing get_generator_name function")
            if not self.setup:
                logger.error("Missing setup function")
            if not self.predict:
                logger.error("Missing predict function")
            raise NotImplementedError(errortext)
        registry[self.get_generator_name()].set_assets = self.setup
        registry[self.get_generator_name()].fx = self.predict
    return method_wrapper


def extract_setassets_data(request):
    """Convert set_assets post payload into a usable json structure"""
    all = requestasjson(request)
    fxname = all.get("fxname")
    argmap = all.get("assetmap")
    return fxname, argmap

def extract_execute_data(request):
    """Convert execute post payload into a usable json structure"""
    all = requestasjson(request)
    fxname = all.get("fxname")
    return fxname, all

def extract_fxinvoke_data(request):
    """Convert fxinvoke post payload into a usable json structure"""
    all = requestasjson(request)
    fxname = all.get("fxname")
    outputtype = byte_string_to_fxtype(all.get("outputtype"))
    arglist = all.get("arguments")
    finalargs = []
    for arg in arglist:
        atype = byte_string_to_fxtype(arg.get("type"))
        data = arg.get("data")
        dataJson = get_data_as_json(atype, data)
        finalargs.append(dataJson)
    return fxname, finalargs, outputtype


def pull_arglist(fxdao, fxname, requestkvpairs):
    eval = registry[fxname].eval
    if not eval:
        raise Exception(fxname + " does not have a registered eval string")
    arglist = []
    # get list of accessor strings
    if isinstance(eval, list):
        for acc_string in eval:
            arglist.append(fxdao.run_accessor(acc_string, requestkvpairs))
    else:
        for protoarg in eval.args:
            acc_string = construct_accessor_string(protoarg)
            arglist.append(fxdao.run_accessor(acc_string, requestkvpairs))
    return arglist

def construct_accessor_string(arg):
    if arg.isConst:
        dtype = arg.constValue.WhichOneof("value")
        val = arg.constValue.__getattribute__(dtype)
        return f'"{val}"' if dtype == "stringValue" else val
    else:
        arg_strings = [construct_accessor_string(a) for a in arg.accCall.args]
        return f"{arg.accCall.accessorName}({','.join(arg_strings)})"


def emptyMessage(messageName):
    if messageName == "apixio.Signal":
        return Signals_pb2.Signal()
    if messageName == "apixio.EventType":
        return Signals_pb2.Signal()


def requestasjson(request):
    return request.get_json() or {x: v for x, v in request.form.items()}


def wait_for_service(port=4500, seconds_to_wait=30, host="127.0.0.1"):
    for _ in range(seconds_to_wait):
        if check_for_service(host, port):
            print("Port is open")
            return True
        else:
            print("Port is not open")
            time.sleep(1)
    return False

def pull_from_modelcatalog(initrequest):
    modelcataloghostport = initrequest.get("modelCatalog") or "https://modelcatalogsvc.apixio.com:8443"
    algorithmID = initrequest.get("algorithmID")
    fxDef, fxname, eval = None, None, None
    if algorithmID:
        mc_url = f"{modelcataloghostport}/mcs/models/{algorithmID}/metadata"
        mc_metadata = requests.get(mc_url).json()
        session = s3connect()
        assets = {}
        for part in mc_metadata.get("parts"):
            name = part.get("name")
            if name == "implementation.jar" or name == "fxdefs.jar":
                continue
            tmp_path = f"/tmp/{algorithmID}.{name}"
            save_from_s3(session, part.get("s3Path"), tmp_path)
            if name == "FxImpl.pb":
                fxDef, fxname = load_fx_def_from_impl(tmp_path)
            elif name == "evalString":
                eval = load_eval(tmp_path)
            else:
                assets[name] = tmp_path
    else:
        fxname = initrequest.get("fxname")
    fxdefstr = initrequest.get("fxdef")
    if fxdefstr:
        fxDef = parseFxDef(fxdefstr)
    evalText = initrequest.get("evalText")
    if evalText:
        eval = split_eval_string(evalText)
    assets = registry[fxname].asset_map or {}

    return fxDef, assets, fxname, eval

def load_eval(tmp_path):
    in_file = open(tmp_path, "rb")
    data = in_file.read()
    in_file.close()
    message = Eval_pb2.ArgList()
    message.ParseFromString(data)
    return message

def load_fx_def_from_impl(tmp_path):
    in_file = open(tmp_path, "rb")
    data = in_file.read()
    in_file.close()
    message = FxProtos_pb2.FxImpl()
    message.ParseFromString(data)
    fxname = message.entryName.split("::")[-1]
    return message.fxDef, fxname

def save_from_s3(session, s3_url, out_file):
    if not os.path.exists(out_file):
        wr.s3.download(path=s3_url, local_file=out_file, boto3_session=session)

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
            return json_to_byte_string(atype, data)
    raise Exception(f"Data type {atype} is not supported by the SDK")

