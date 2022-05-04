from base_generator import BaseGenerator

DEFAULT_IMPORTS = """\
import java.util
import scala.collection.JavaConversions._

import com.apixio.model.nassembly.Persistable
import com.apixio.model.nassembly.Persistable.ColFieldsWithPersistentField"""

class Persistable(BaseGenerator):
    datatype_name = ''
    cluster_col_names = []

    def __init__(self, datatype_name, cluster_col_names):
        self.datatype_name = datatype_name
        self.cluster_col_names = cluster_col_names

    def get_package(self):
        return "package com.apixio.nassembly.%s" % self.datatype_name

    def get_class_name(self):
        return "%sPersistable" % self.capitalize_class_name()

    def get_class_declaration(self):
        return "class %s extends Persistable {" % (self.get_class_name())

    def get_datatype_name(self):
        return """\
  override def getDataTypeName: String = {
    "%s"
  }""" % self.datatype_name

    def override_build_cluster_col(self):
      return """\
  override def useBuildInClusterCol: Boolean = {
    // TODO!!
    false
  }"""


    def override_col_to_persistable_names_for_clustered_cols(self):
      return """\
  override def fromColFieldNamesToPersistentFieldNamesForClusteredColumns(): util.List[ColFieldsWithPersistentField] = {
    val colFieldsWithPersistentField: ColFieldsWithPersistentField = new ColFieldsWithPersistentField(Array(%s), "col")
    List(colFieldsWithPersistentField)
  }""" % (', '.join('"{0}"'.format(col) for col in self.cluster_col_names))

    def override_col_to_persistable_names_for_non_clustered_cols(self):
      return """\
  override def fromColFieldNamesToPersistentFieldNamesForNonClusteredColumns(): util.List[ColFieldsWithPersistentField] = {
    List() // TODO: Confirm this isn't needed
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

}""" % (self.get_package(),
              DEFAULT_IMPORTS,
              self.get_class_declaration(),
              self.get_datatype_name(),
              self.override_build_cluster_col(),
              self.override_col_to_persistable_names_for_clustered_cols(),
              self.override_col_to_persistable_names_for_non_clustered_cols())
        with open("../../bizlogic/src/main/scala/com/apixio/nassembly/%s/%s.scala" % (self.datatype_name, self.get_class_name()), "w") as f:
          f.write(contents)

## Example: Persistable("procedureWrapper", ["dateBucket.year","dateBucket.bucket"])