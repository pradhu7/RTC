package com.apixio.model.chart.elastic.util;

import com.apixio.model.chart.elastic.NestedES;
import com.apixio.model.chart.elastic.TypeWrapperES;
import com.apixio.model.chart.elastic.exception.ElasticMappingDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MappingUtil {

    private static final Logger logger = LoggerFactory.getLogger(MappingUtil.class);

    private MappingUtil() {
    }

    public static Map<String, Object> getElasticMappingFromTypeWrapper(Class<?> clazz) throws Exception {
        Method mappingMethod = clazz.getMethod("getElasticTypeMapping");
        return (Map<String, Object>) mappingMethod.invoke(null);
    }

    public static Map<String, Object> getElasticMappingFromNestedClass(Class<?> clazz) throws Exception {
        Method mappingMethod = clazz.getMethod("getElasticMapping");
        return (Map<String, Object>) mappingMethod.invoke(null);
    }

    public static Map<String, Object> extractESFieldProperties(Field[] fields) throws ElasticMappingDefinitionException {
        Map<String, Object> properties = new HashMap<>();
        for (Field field : fields) {
            if (NestedES.class.isAssignableFrom(field.getType())) {
                //nested class - call 'getElasticMapping()'
                try {
                    properties.put(field.getName(), MappingUtil.getElasticMappingFromNestedClass(field.getType()));
                } catch (Exception e) {
                    String errorMessage = String.format("Exception while invoking getElasticMapping() function on nested document class of type=%s", field.getType());
                    logger.error(errorMessage, e);
                    throw new ElasticMappingDefinitionException(errorMessage, e);
                }
            } else if (TypeWrapperES.class.isAssignableFrom(field.getType())) {
                //ESValue class - call 'getElasticTypeMapping()'
                try {
                    properties.put(field.getName(), MappingUtil.getElasticMappingFromTypeWrapper(field.getType()));
                } catch (Exception e) {
                    String errorMessage = String.format("Exception while invoking getElasticTypeMapping() function on ESValue class of type=%s", field.getType());
                    logger.error(errorMessage, e);
                    throw new ElasticMappingDefinitionException(errorMessage, e);
                }
            } else if (Collection.class.isAssignableFrom(field.getType())) {
                //collection type - get base class
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                Class<?> collectionClass = (Class<?>) type.getActualTypeArguments()[0];
                if (TypeWrapperES.class.isAssignableFrom(collectionClass)) {
                    //list<ESValue> type - call 'getElasticTypeMapping()'
                    try {
                        properties.put(field.getName(), MappingUtil.getElasticMappingFromTypeWrapper(collectionClass));
                    } catch (Exception e) {
                        String errorMessage = String.format("Exception while invoking getElasticTypeMapping() function on ESValue class of type=%s", field.getType());
                        logger.error(errorMessage, e);
                        throw new ElasticMappingDefinitionException(errorMessage, e);
                    }
                } else if (NestedES.class.isAssignableFrom(collectionClass)) {
                    //list<NestedES> class - call 'getElasticMapping()'
                    try {
                        properties.put(field.getName(), MappingUtil.getElasticMappingFromNestedClass(collectionClass));
                    } catch (Exception e) {
                        String errorMessage = String.format("Exception while invoking getElasticMapping() function on nested document class of type=%s", field.getType());
                        logger.error(errorMessage, e);
                        throw new ElasticMappingDefinitionException(errorMessage, e);
                    }
                } else {
                    //found invalid type
                    String errorMessage = String.format("Found invalid collection type=%s", collectionClass);
                    logger.error(errorMessage);
                    throw new ElasticMappingDefinitionException(errorMessage);
                }
            } else {
                //found invalid type
                String errorMessage = String.format("Found invalid type=%s for ES class", field.getType());
                logger.error(errorMessage);
                throw new ElasticMappingDefinitionException(errorMessage);
            }
        }
        return properties;
    }
}
