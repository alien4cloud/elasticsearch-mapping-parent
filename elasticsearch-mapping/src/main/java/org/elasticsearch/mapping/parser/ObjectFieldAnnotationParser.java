package org.elasticsearch.mapping.parser;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.annotation.ObjectField;
import com.google.common.collect.Maps;
import org.elasticsearch.mapping.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Parse a {@link org.elasticsearch.annotation.ObjectField} to enrich the mapping
 */
@Slf4j
public class ObjectFieldAnnotationParser implements IPropertyAnnotationParser<ObjectField> {

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
            log.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
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
                String newPrefix = pathPrefix == null ? indexable.getName() + "." : pathPrefix + indexable.getName() + ".";
                this.fieldsMappingBuilder.parseFieldMappings(replaceClass, fieldDefinition, facets, filters, fetchContext, newPrefix, nestedPrefix, false);
            } catch (IntrospectionException e) {
                log.error("Fail to parse object class <" + replaceClass.getName() + ">", e);
            }
        }
    }
}
