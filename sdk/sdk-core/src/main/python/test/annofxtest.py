import sys
sys.path.append("..")
import sdkannotest as sdkannotest
from apxsdk.fxtest import FxTest

execPath = "./sdkannotest"
annoFxName = "AnnotationFx"
transformSignalsFxDef = "list<apixio.Signal> transformSignals(list<apixio.Signal>)"
annoAccessor = "patientannotations(request('patientuuid'))"

class AnnoFxTest(FxTest):
    assets = None

    def __init__(self, config) -> None:
        super().__init__(config)
        self.assets = {"config": ""}
    
    def test_direct(self, patientuuid):
        sdkannotest.myset_assets(**self.assets)
        annos = self.run_accessor(annoAccessor, dict(patientuuid=patientuuid))
        result = sdkannotest.convertAnnotations(annos)
        dataUri = self.generateUri(dict(algo=annoFxName, patientuuid=patientuuid))
        success = self.persist(transformSignalsFxDef, dataUri, result)
        print("Persist " + "succeeded" if success else "failed")

    def test_full(self, patientuuid):
        self.initFx(execPath, annoFxName, self.assets)
        annos = self.run_accessor(annoAccessor, dict(patientuuid=patientuuid))
        result = self.runFx(annoFxName, transformSignalsFxDef, annos)
        dataUri = self.generateUri(dict(algo=annoFxName, patientuuid=patientuuid))
        success = self.persist(transformSignalsFxDef, dataUri, result)
        print("Persist " + "succeeded" if success else "failed")

if __name__ == "__main__":
    patientuuid = "e93d6b2f-c130-471b-9c25-729a405862d6"
    AnnoFxTest("../../test/python/fxtest.yaml").test_full(patientuuid)
    # AnnoFxTest("../../test/python/fxtest.yaml").test_direct(patientuuid)
