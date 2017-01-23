package org.elasticsearch.mapping.parser;

import java.beans.IntrospectionException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.annotation.MapKeyValue;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.*;

/**
 * Created by lucboutier on 15/06/2016.
 */
public class MapKeyValueAnnotationParser implements IPropertyAnnotationParser<MapKeyValue> {
    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);

    private final FieldsMappingBuilder fieldsMappingBuilder;
    private final List<IFilterBuilderHelper> filters;
    private final List<IFacetBuilderHelper> facets;

    public MapKeyValueAnnotationParser(FieldsMappingBuilder fieldsMappingBuilder, List<IFilterBuilderHelper> filters, List<IFacetBuilderHelper> facets) {
        this.fieldsMappingBuilder = fieldsMappingBuilder;
        this.filters = filters;
        this.facets = facets;
    }

    @Override
    public void parseAnnotation(MapKeyValue annotation, Map<String, Object> fieldDefinition, String pathPrefix, String nestedPrefix, Indexable indexable) {
        if (fieldDefinition.get("type") != null) {
            LOGGER.info("Overriding mapping for field {} for class {} was defined as type {}", indexable.getName(), indexable.getDeclaringClassName(),
                    fieldDefinition.get("type"));
            fieldDefinition.clear();
        }
        // the key of the map is a spring mapping.
        Map<String, Object> properties = Maps.newHashMap();
        fieldDefinition.put("type", "object");
        fieldDefinition.put("enabled", "true");
        fieldDefinition.put("properties", properties);

        Map<String, Object> keyFieldDefinition = Maps.newHashMap();
        properties.put("key", keyFieldDefinition);

        keyFieldDefinition.put("type", "string");
        keyFieldDefinition.put("store", annotation.store());
        keyFieldDefinition.put("index", annotation.indexType());
        // TODO doc_values
        keyFieldDefinition.put("term_vector", annotation.termVector());
        keyFieldDefinition.put("boost", annotation.boost());
        if (!StringField.NULL_VALUE_NOT_SPECIFIED.equals(annotation.nullValue())) {
            keyFieldDefinition.put("null_value", annotation.nullValue());
        }
        if (!NormEnabled.DEFAULT.equals(annotation.normsEnabled())) {
            Map<String, Object> norms = new HashMap<String, Object>();
            norms.put("enabled", annotation.normsEnabled().name().toLowerCase());
            if (!NormLoading.DEFAULT.equals(annotation.normsLoading())) {
                norms.put("loading", annotation.normsLoading());
            }
            keyFieldDefinition.put("norms", norms);
        }
        if (!IndexOptions.DEFAULT.equals(annotation.indexOptions())) {
            keyFieldDefinition.put("index_options", annotation.indexOptions());
        }
        if (!annotation.analyser().isEmpty()) {
            keyFieldDefinition.put("analyzer", annotation.analyser());
        }
        if (!annotation.indexAnalyzer().isEmpty()) {
            keyFieldDefinition.put("index_analyzer", annotation.indexAnalyzer());
        }
        if (!annotation.searchAnalyzer().isEmpty()) {
            keyFieldDefinition.put("search_analyzer", annotation.searchAnalyzer());
        }

        keyFieldDefinition.put("include_in_all", annotation.includeInAll());
        if (annotation.ignoreAbove() > 0) {
            keyFieldDefinition.put("ignore_above", annotation.ignoreAbove());
        }

        Map<String, Object> valueFieldDefinition = Maps.newHashMap();
        properties.put("value", valueFieldDefinition);

        // we need to process the map value type recursively
        Class<?> mapValueType = indexable.getComponentType(1);
        if (mapValueType != null) {
            Map<String, SourceFetchContext> fetchContext = Maps.newHashMap();
            try {
                this.fieldsMappingBuilder.parseFieldMappings(mapValueType, valueFieldDefinition, facets, filters, fetchContext,
                        indexable.getName() + ".value.", nestedPrefix);
            } catch (IntrospectionException e) {
                LOGGER.error("Fail to parse object class <" + mapValueType.getName() + ">", e);
            }
        } else {
            LOGGER.warn("Cannot find value class for map with annotation MapKeyValue");
        }
    }
}