class BaseGenerator():
    datatype_name = ''

    UNSUPPORTED_EXCEPTION = """throw new UnsupportedOperationException("TODO")"""

    def __init__(self, datatype_name):
    	self.datatype_name = datatype_name

    def get_package(self):
        return "package com.apixio.nassembly.%s" % self.datatype_name

    def get_exchange_name(self):
        return "%sExchange" % self.datatype_name

    def capitalize_class_name(self):
    	## Title and capitalize failing
    	first = self.datatype_name[0].upper()
    	rest = self.datatype_name[1:]
    	return first + rest

    def get_datatype_name(self):
        return """\
  override def getDataTypeName: String = {
    "%s"
  }""" % self.datatype_name