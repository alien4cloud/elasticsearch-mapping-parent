package org.elasticsearch.mapping.parser;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.*;

/**
 * Parse a {@link org.elasticsearch.annotation.ObjectField} to enrich the mapping
 */
public class ObjectFieldAnnotationParser implements IPropertyAnnotationParser<ObjectField> {
    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);

    private final FieldsMappingBuilder fieldsMappingBuilder;
    private final List<IFilterBuilderHelper> filters;
    private final List<IFacetBuilderHelper> facets;

    public ObjectFieldAnnotationParser(FieldsMappingBuilder fieldsMappingBuilder, List<IFilterBuilderHelper> filters, List<IFacetBuilderHelper> facets) {
        this.fieldsMappingBuilder = fieldsMappingBuilder;
        this.filters = filters;
        this.facets = facets;
    }

    @Override
    public void parseAnnotation(ObjectField annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix, Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            LOGGER.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }

        Class<?> objectClass = annotation == null ? ObjectField.class : annotation.objectClass();
        Boolean enabled = annotation == null ? true : annotation.enabled();

        fieldDefinition.put("type", "object");
        fieldDefinition.put("enabled", enabled);
        if (enabled) {
            Map<String, SourceFetchContext> fetchContext = Maps.newHashMap();
            // nested types can provide replacement class to be managed. This can be usefull to override map default type for example.
            Class<?> replaceClass = objectClass.equals(ObjectField.class) ? indexable.getType() : objectClass;
            try {
                this.fieldsMappingBuilder.parseFieldMappings(replaceClass, fieldDefinition, facets, filters, fetchContext, indexable.getName() + ".",
                        nestedPrefix);
            } catch (IntrospectionException e) {
                LOGGER.error("Fail to parse object class <" + replaceClass.getName() + ">", e);
            }
        }
    }
}
