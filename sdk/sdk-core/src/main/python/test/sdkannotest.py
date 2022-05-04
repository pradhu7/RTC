import sys
sys.path.insert(0,"..")

from apxsdk import sdk

@sdk.register_set_assets("AnnotationFx")
def myset_assets(config):
    print("AnnotationFx assets", config)

@sdk.register_fx("AnnotationFx")
def convertAnnotations(signals):
    print("called AnnotationFx ", signals)
    return signals

def start():
    sdk.init_app()

if __name__ == "__main__":
    start()
    # To test:
    # curl -v --insecure -H "Content-Type: application/json" -H "Accept: application/json" --data-binary @/Users/bpeintner/IdeaProjects/apx-sdk/scripts/init.json http://localhost:4500/apxsdk/initialize 
