import os
# import apxsdk.sdk as sdk
import apxsdk.sdkutil as sdkutil
import apxprotobufs.fx_pb2 as FxProtos_pb2
import requests
import subprocess

URI_SCHEME = "apxquery"
URI_BASE = URI_SCHEME + "://"  # must be followed by {domain}
URI_PATH = "/q?"
FXPROCESS_PORT = 4500
FXPROCESS_ROOT = f"http://localhost:{FXPROCESS_PORT}"

class FxDAO:
    ECC_ROOT = "http://localhost:8000"

    def __init__(self, configfile=None) -> None:
        if configfile:
            sdkutil.load_config(configfile)
        self.ECC_ROOT = sdkutil.cfg.get("fxConfig").get("featureStoreUrl") or self.ECC_ROOT

    def run_accessor(self, accessorString, requestParams):
        body = dict(requestParams)
        body["accessorString"] = accessorString
        result = requests.post(self.ECC_ROOT + "/accessor", json=body)
        response = result.json()
        if "ERROR" in response:
            raise Exception(response.get("ERROR"))
        rtype = response.get("type")
        data = response.get("data")
        mtype = sdkutil.extract_type(rtype)
        resultObject = sdkutil.get_data_as_json(mtype, data)
        return resultObject

    def initFx(self, execPath, fxName, assets):
        if not os.path.exists(execPath):
            raise FileNotFoundError(execPath + " does not exist")
        if not sdkutil.check_for_service(port=FXPROCESS_PORT):
            subprocess.Popen([execPath, str(FXPROCESS_PORT)])

        if not sdkutil.wait_for_service(FXPROCESS_PORT):
            raise Exception("""Could not start FxService.
                               Ensure that you can start it outside FxTest and
                               that it is listening to the port passed in as first argument""")
        self._post_set_assets(fxName, assets)
        return True

    def runFx(self, fxName, fxDef, *methodArgList):
        fxDefElements = sdkutil.parseFxDef(fxDef)
        outtype = fxDefElements.get("returnType")
        methodArgTypes = fxDefElements.get("argTypes")
        return self._post_invoke(fxName, outtype, methodArgTypes, methodArgList)

    # def shutdownFx(pid):
    #     pass 

    def generateUri(self, uri_element_map, domain="devel"):
        return URI_BASE + domain + URI_PATH + "&".join([f"{k}={v}" for k, v in uri_element_map.items()])

    def persist(self, fxDef, dataUri, data):
        if isinstance(fxDef, str):
            fxDefElements = sdkutil.parseFxDef(fxDef)
            dataType = fxDefElements.get("returnType")
        else:
            dataType = fxDef.returns
        outputTransport = sdkutil.produce_output(data, dataType)
        encoded_type = sdkutil.message_to_byte_string(dataType) \
            if isinstance(dataType, FxProtos_pb2.FxType) else dataType
        body = {"data": outputTransport, "dataUri": dataUri, "type": encoded_type}
        result = requests.post(self.ECC_ROOT + "/persist", json=body)
        return result.status_code == 200

    def _post_set_assets(self, fxName, assets):
        body = {"fxname": fxName, "assetmap": assets}
        result = requests.post(FXPROCESS_ROOT + "/apxsdk/setassets", json=body)
        response = result.json()
        if not response or response.get("status") != "success":
            raise Exception("Set assets call failed:" + response)
        return True

    def _post_invoke(self, fxName, outputType, argTypes, argList):
        body = {"fxname": fxName}
        body["outputtype"] = sdkutil.message_to_byte_string(outputType)
        body["arguments"] = []
        for argType, arg in zip(argTypes, argList):
            body["arguments"].append(
                {"type": sdkutil.message_to_byte_string(argType),
                 "data": sdkutil.json_to_byte_string(argType, arg)}
            )

        result = requests.post(FXPROCESS_ROOT + "/apxsdk/invokefx", json=body)
        response = result.json()
        resultObject = sdkutil.get_data_as_json(outputType, response)
        return resultObject

