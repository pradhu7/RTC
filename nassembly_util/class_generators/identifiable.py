from base_generator import BaseGenerator

DEFAULT_IMPORTS = """import com.apixio.model.nassembly.Identifiable"""

class Identifiable(BaseGenerator):
    datatype_name = ''
    oid_field_name = ''
    cid_field_names = []
    unique_id_field_names = []

    def __init__(self, datatype_name, oid_field_name, cid_field_names, unique_id_field_names):
        self.datatype_name = datatype_name
        self.oid_field_name = oid_field_name
        self.cid_field_names = cid_field_names
        self.unique_id_field_names = unique_id_field_names

    def get_class_name(self):
        return "%sIdentifiable" % self.datatype_name.upper()

    def get_class_declaration(self):
        return "class %s extends Identifiable {" % (self.get_class_name())

    def override_oid_field(self):
        return '''override def oIdFieldName: String = "%s"''' % self.oid_field_name

    def override_cid_computation(self):
        return """\
  override def cIdComputationFieldNames(): Array[String] = {
    Array(%s)
  }""" % (', '.join('"{0}"'.format(cid) for cid in self.cid_field_names))

    def override_unique_field_names(self):
        return """\
  override def getUniqueIdFieldNames(): Array[String] = {
    Array(%s)
  }""" % (', '.join('"{0}"'.format(uid) for uid in self.unique_id_field_names))

    def generate_class(self):
        contents = """\
%s

%s

%s

%s

%s

%s

%s

}""" % (self.get_package(),
              DEFAULT_IMPORTS,
              self.get_class_declaration(),
              self.get_datatype_name(),
              self.override_oid_field(),
              self.override_cid_computation(),
              self.override_unique_field_names())
        with open("../../bizlogic/src/main/scala/com/apixio/nassembly/%s/%s.scala" % (self.datatype_name, self.get_class_name()), "w") as f:
    	    f.write(contents)

## Example: Identifiable("ffs", "PATIENT_ID", ["PATIENT_ID"], ["patientId","originalId","performedOn","endDate","code","supportingDiagnosis","billing","deleteIndicator","editType"])