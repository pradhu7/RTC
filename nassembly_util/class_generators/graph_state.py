from base_generator import BaseGenerator


DEFAULT_IMPORTS = """import com.apixio.model.nassembly.GraphState"""

class GraphState(BaseGenerator):
    datatype_name = ''
    is_part = True
    is_combined = False
    is_persisted = False

    def __init__(self, datatype_name, is_part, is_combined, is_persisted):
        self.datatype_name = datatype_name
        self.is_part = is_part
        self.is_combined = is_combined
        self.is_persisted = is_persisted

    def get_class_name(self):
        return "%sGraphState" % self.datatype_name.upper()

    def get_class_declaration(self):
        return "class %s extends GraphState {" % (self.get_class_name())

    def override_is_part(self):
        return """\
  override def isPart(): Boolean = {
    %s
  }""" % str(self.is_part).lower()



    def override_is_combined(self):
        return """\
  override def isCombined(): Boolean = {
    %s
  }""" % str(self.is_combined).lower()

    def override_is_persisted(self):
        return """\
  override def isPersisted(): Boolean = {
    %s
  }""" % str(self.is_persisted).lower()

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
              self.override_is_part(),
              self.override_is_combined(),
              self.override_is_persisted())
        with open("../../bizlogic/src/main/scala/com/apixio/nassembly/%s/%s.scala" % (self.datatype_name, self.get_class_name()), "w") as f:
    	    f.write(contents)

## Example: GraphState("ffs", True, False, True)