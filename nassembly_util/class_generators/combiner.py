from base_generator import BaseGenerator

DEFAULT_IMPORTS = """\
import java.util

import scala.collection.JavaConverters._
import com.apixio.model.nassembly.{Exchange, Combiner}"""

class Combiner(BaseGenerator):
    datatype_name = ''
    combined_datatype_names = []
    combine_by_field_names = []

    def __init__(self, datatype_name, combined_datatype_names, combine_by_field_names):
        self.datatype_name = datatype_name
        self.combined_datatype_names = combined_datatype_names
        self.combine_by_field_names = combine_by_field_names

    def get_class_name(self):
        return "%sCombiner" % self.capitalize_class_name()

    def get_class_declaration(self):
        return "class %s extends Combiner[%s] {" % (self.capitalize_class_name(), self.get_exchange_name())

    def override_combined_datatype_names(self):
      return """\
  override def getCombinedDataTypeNames: Array[String] = {
    Array(%s)
  }""" % (', '.join('"{0}"'.format(datatype) for datatype in self.combined_datatype_names))

    def override_combine(self):
      return """\
  override def combine(dataTypeNameToExchanges: util.Map[String, _ <: Array[Exchange]]): %s = {
    val wrapper = new %s
    // TODO!!
    %s
  }""" % (self.get_exchange_name(), self.get_exchange_name(), self.UNSUPPORTED_EXCEPTION)


    def override_combine_by_fields(self):
      return """\
  override def getCombineByFieldNames: Array[String] = {
    Array(%s)
  }""" % (', '.join('"{0}"'.format(field_name) for field_name in self.combine_by_field_names))


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
              self.override_combined_datatype_names(),
              self.override_combine(),
              self.override_combine_by_fields())
        with open("../../bizlogic/src/main/scala/com/apixio/nassembly/%s/%s.scala" % (self.datatype_name, self.get_class_name()), "w") as f:
    	    f.write(contents)

## Example: Combiner("procedureWrapper", ["ffs"], ["patientId", "dateBucket"])