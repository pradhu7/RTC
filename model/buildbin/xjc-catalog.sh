
srcloc=src/main/resources/catalog.xsd
package=com.apixio.model.file.catalog.jaxb.generated

# look in a couple of locations for the .xsd...

xjc -p ${package} -d src/main/java $srcloc
