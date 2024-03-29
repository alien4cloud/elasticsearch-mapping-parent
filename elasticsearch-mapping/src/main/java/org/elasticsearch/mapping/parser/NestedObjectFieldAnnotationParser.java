package org.elasticsearch.mapping.parser;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.annotation.NestedObject;
import com.google.common.collect.Maps;
import org.elasticsearch.mapping.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NestedObjectFieldAnnotationParser implements IPropertyAnnotationParser<NestedObject> {

    private final FieldsMappingBuilder fieldsMappingBuilder;
    private final List<IFilterBuilderHelper> filters;
    private final List<IFacetBuilderHelper> facets;

    public NestedObjectFieldAnnotationParser(FieldsMappingBuilder fieldsMappingBuilder, List<IFilterBuilderHelper> filters, List<IFacetBuilderHelper> facets) {
        this.fieldsMappingBuilder = fieldsMappingBuilder;
        this.filters = filters;
        this.facets = facets;
    }

    @Override
    public void parseAnnotation(NestedObject annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix, Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            log.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }

        fieldDefinition.put("type", "nested");
        Map<String, SourceFetchContext> fetchContext = Maps.newHashMap();
        // nested types can provide replacement class to be managed. This can be usefull to override map default type for example.
        Class<?> replaceClass = annotation.nestedClass().equals(NestedObject.class) ? indexable.getType() : annotation.nestedClass();
        try {
            this.fieldsMappingBuilder.parseFieldMappings(replaceClass, fieldDefinition, facets, filters, fetchContext, indexable.getName() + ".",
                    indexable.getName(), null);
        } catch (IntrospectionException e) {
            log.error("Fail to parse nested class <" + replaceClass.getName() + ">", e);
        }
    }
}
