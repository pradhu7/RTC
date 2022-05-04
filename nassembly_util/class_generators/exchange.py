from base_generator import BaseGenerator

DEFAULT_IMPORTS = """\
import java.io.InputStream
import com.google.protobuf.util.JsonFormat
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
"""


class Exchange(BaseGenerator):
    proto_outer_class = ''
    proto_name = ''
    datatype_name = ''

    def __init__(self, proto_outer_class, proto_name, datatype_name):
        self.proto_outer_class = proto_outer_class
        self.proto_name = proto_name
        self.datatype_name = datatype_name


    def get_class_name(self):
        return "%sExchange" % self.capitalize_class_name()

    def get_class_declaration(self):
        return "class %s extends Exchange {" % self.get_class_name()

    def get_imports(self):
        return """\
%s
import com.apixio.datacatalog.%s
import com.apixio.datacatalog.%s.%s""" % (DEFAULT_IMPORTS, self.proto_outer_class, self.proto_outer_class, self.proto_name)

    def protos(self):
        return "  private var protos: Array[%s] = Array.empty" % self.proto_name

    def get_proto(self):
        return "  def get%s: %s = protos(0)" % (self.capitalize_class_name(), self.proto_name)

    def get_cid(self):
        return """\
  override def getCid: String = {
    // protos(0).getPatientId
    %s
  }""" % self.UNSUPPORTED_EXCEPTION


    def from_protobytes(self):
        return """\
  override def fromProto(protoBytes: Array[Byte]): Unit = {
    protos = Array(%s.parseFrom(protoBytes))
  }""" % self.proto_name

    def from_stream(self):
        return """\
  override def fromProto(inputStream: InputStream): Unit = {
    protos = Array(%s.parseFrom(inputStream))
  }""" % self.proto_name

    def parse(self):
        return """\
  override def parse(pdsId: String, uploadBatchId: String, fileType: String, fromNameToValue: java.util.Map[String, AnyRef]): Unit = {
    protos = Array() // TODO
    %s
  }""" % self.UNSUPPORTED_EXCEPTION

    def parse_apo(self):
        return """\
  override def parse(uploadBatchId: String, apo: Patient): Unit = {
    protos = Array() // TODO
    %s
  }""" % self.UNSUPPORTED_EXCEPTION

    def proto_envelope(self):
        return """\
  override def getProtoEnvelops: Array[ProtoEnvelop] = {
    protos.map(proto => new ProtoEnvelop("TODO! GET OID from proto (patientID?)",
      proto.toByteArray,
      JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(proto))
    )
  }"""

    def generate_class(self):
        contents = """\
%s

%s

%s

%s

%s

%s

%s

%s

%s

%s

%s

%s

}""" % (self.get_package(),
              self.get_imports(),
              self.get_class_declaration(),
              self.protos(),
              self.get_proto(),
              self.get_datatype_name(),
              self.get_cid(),
              self.from_protobytes(),
              self.from_stream(),
              self.parse(),
              self.parse_apo(),
              self.proto_envelope())
        with open("../../bizlogic/src/main/scala/com/apixio/nassembly/%s/%s.scala" % (self.datatype_name, self.get_class_name()), "w") as f:
    	    f.write(contents)

## Example: Exchange("ProcedureProto", "Procedure", "ffs")