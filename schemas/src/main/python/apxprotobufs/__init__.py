import os, sys

# This is a workaround for
# https://github.com/protocolbuffers/protobuf/issues/881
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))
