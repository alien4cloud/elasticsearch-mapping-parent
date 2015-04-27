package org.elasticsearch.mapping.parser;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.*;

public class NestedObjectFieldAnnotationParser implements IPropertyAnnotationParser<NestedObject> {
    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);

    private final FieldsMappingBuilder fieldsMappingBuilder;
    private final List<IFilterBuilderHelper> filters;
    private final List<IFacetBuilderHelper> facets;

    public NestedObjectFieldAnnotationParser(FieldsMappingBuilder fieldsMappingBuilder, List<IFilterBuilderHelper> filters, List<IFacetBuilderHelper> facets) {
        this.fieldsMappingBuilder = fieldsMappingBuilder;
        this.filters = filters;
        this.facets = facets;
    }

    @Override
    public void parseAnnotation(NestedObject annotation, Map<String, Object> fieldDefinition, String pathPrefix, Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            throw new MappingException("A field cannot have more than one Elastic Search type. Parsing NestedObject on <" + indexable.getDeclaringClassName()
                    + "." + indexable.getName() + "> type is already set to <" + fieldDefinition.get("type") + ">");
        }

        fieldDefinition.put("type", "nested");
        Map<String, SourceFetchContext> fetchContext = Maps.newHashMap();
        // nested types can provide replacement class to be managed. This can be usefull to override map default type for example.
        Class<?> replaceClass = annotation.nestedClass().equals(NestedObject.class) ? indexable.getType() : annotation.nestedClass();
        try {
            this.fieldsMappingBuilder.parseFieldMappings(replaceClass, fieldDefinition, facets, filters, fetchContext, indexable.getName() + ".");
        } catch (IntrospectionException e) {
            LOGGER.error("Fail to parse nested class <" + replaceClass.getName() + ">", e);
        }
    }
}