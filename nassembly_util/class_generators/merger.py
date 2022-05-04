from base_generator import BaseGenerator

DEFAULT_IMPORTS = """\
import com.apixio.model.nassembly.GarbageCollector
import com.apixio.model.nassembly.GarbageCollector.SortedFieldOrder"""

class Merger(BaseGenerator):
    datatype_name = ''
    group_by_field_names = []
    sorted_fields = []
    use_built_in_merge = False

    def __init__(self, datatype_name, group_by_field_names, sorted_fields, use_built_in_merge):
        self.datatype_name = datatype_name
        self.group_by_field_names = group_by_field_names
        self.sorted_fields = sorted_fields
        self.use_built_in_merge = use_built_in_merge

    def get_class_name(self):
        return "%sMerger" % self.capitalize_class_name()

    def get_class_declaration(self):
        return "class %s extends Merger[%sExchange] {" % (self.get_class_name(), self.capitalize_class_name())

    def override_group_by(self):
        return """\
  override def getGroupByFieldNames: Array[String] = {
    Array(%s)
  }""" % (', '.join('"{0}"'.format(cid) for cid in self.group_by_field_names))

    def override_sorted(self):
        return """\
  override def getSortedFields: Array[SortedFieldOrder] = {
    Array(%s) 
  }""" % (", ".join(['''new SortedFieldOrder("%s",%s)''' % (field_order[0], str(field_order[1]).lower()) for field_order in self.sorted_fields]))

    def override_built_in_merge(self):
      exchange_class = self.datatype_name.upper() + "Exchange"
      return """\
  override def useBuildInMerge(): Boolean = false

  override def merge(exchange1: %s, exchange2: %s): %s = {
    throw new UnsupportedOperationException("TODO!!!")
  }""" % (exchange_class, exchange_class, exchange_class)



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
              self.override_group_by(),
              self.override_sorted(),
              self.override_built_in_merge() if self.override_built_in_merge else "")
        with open("../../bizlogic/src/main/scala/com/apixio/nassembly/%s/%s.scala" % (self.datatype_name, self.get_class_name()), "w") as f:
    	    f.write(contents)

## Example: Merger("ffs", ["patientId", "code", "endDate"], [("billing.transactionDate", False)], False)