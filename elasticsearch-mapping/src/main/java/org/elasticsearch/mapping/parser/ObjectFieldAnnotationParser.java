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
    public void parseAnnotation(ObjectField annotation, Map<String, Object> fieldDefinition, String pathPrefix, Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            throw new MappingException("A field cannot have more than one Elastic Search type. Parsing ComplexObject on <" + indexable.getDeclaringClassName()
                    + "." + indexable.getName() + "> type is already set to <" + fieldDefinition.get("type") + ">");
        }

        fieldDefinition.put("type", "object");
        fieldDefinition.put("enabled", annotation.enabled());
        if (annotation.enabled()) {
            Map<String, SourceFetchContext> fetchContext = Maps.newHashMap();
            // nested types can provide replacement class to be managed. This can be usefull to override map default type for example.
            Class<?> replaceClass = annotation.objectClass().equals(ObjectField.class) ? indexable.getType() : annotation.objectClass();
            try {
                this.fieldsMappingBuilder.parseFieldMappings(replaceClass, fieldDefinition, facets, filters, fetchContext, indexable.getName() + ".");
            } catch (IntrospectionException e) {
                LOGGER.error("Fail to parse object class <" + replaceClass.getName() + ">", e);
            }
        }
    }
}
