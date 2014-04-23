package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.annotation.Analyser;
import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.Boost;
import org.elasticsearch.annotation.DateField;
import org.elasticsearch.annotation.DateFormat;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.IndexAnalyser;
import org.elasticsearch.annotation.IndexName;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.NullValue;
import org.elasticsearch.annotation.NumberField;
import org.elasticsearch.annotation.Routing;
import org.elasticsearch.annotation.SearchAnalyser;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.RangeFacet;
import org.elasticsearch.annotation.query.RangeFilter;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.parser.AnalyserAnnotationParser;
import org.elasticsearch.mapping.parser.BooleanFieldAnnotationParser;
import org.elasticsearch.mapping.parser.DateFieldAnnotationParser;
import org.elasticsearch.mapping.parser.DateFormatAnnotationParser;
import org.elasticsearch.mapping.parser.IPropertyAnnotationParser;
import org.elasticsearch.mapping.parser.IndexAnalyserAnnotationParser;
import org.elasticsearch.mapping.parser.IndexNameAnnotationParser;
import org.elasticsearch.mapping.parser.NestedObjectFieldAnnotationParser;
import org.elasticsearch.mapping.parser.NullValueAnnotationParser;
import org.elasticsearch.mapping.parser.NumberFieldAnnotationParser;
import org.elasticsearch.mapping.parser.SearchAnalyserAnnotationParser;
import org.elasticsearch.mapping.parser.StringFieldAnnotationParser;
import org.elasticsearch.util.MapUtil;
import org.springframework.util.ClassUtils;

/**
 * Process fields in a class to fill-in the properties entry in the class definition map.
 * 
 * @author luc boutier
 */
public class FieldsMappingBuilder {
    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);

    /**
     * Parse fields from the given class to add properties mapping.
     * 
     * @param clazz The class for which to parse fields.
     * @param classDefinitionMap The map that contains the class definition (a "properties" entry will be added with field mapping content).
     * @param facetFields A list that contains all the field facet mapping.
     * @param filteredFields A list that contains all the field filters mapping.
     * @param fetchContexts A list that contains all the fetch contexts mapping.
     * @param pathPrefix A prefix which is a path (null for root object, and matching the field names for nested objects).
     * @throws IntrospectionException In case we fail to use reflexion on the given class.
     */
    @SuppressWarnings("unchecked")
    public void parseFieldMappings(Class<?> clazz, Map<String, Object> classDefinitionMap, List<IFacetBuilderHelper> facetFields,
            List<IFilterBuilderHelper> filteredFields, Map<String, SourceFetchContext> fetchContexts, String pathPrefix) throws IntrospectionException {
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            parseFieldMappings(clazz.getSuperclass(), classDefinitionMap, facetFields, filteredFields, fetchContexts, pathPrefix);
        }

        List<Indexable> indexables = getIndexables(clazz);

        Map<String, Object> propertiesDefinitionMap = (Map<String, Object>) classDefinitionMap.get("properties");
        if (propertiesDefinitionMap == null) {
            propertiesDefinitionMap = new HashMap<String, Object>();
            classDefinitionMap.put("properties", propertiesDefinitionMap);
        }

        for (Indexable indexable : indexables) {
            parseFieldMappings(clazz, classDefinitionMap, facetFields, filteredFields, fetchContexts, propertiesDefinitionMap, pathPrefix, indexable);
        }
    }

    private void parseFieldMappings(Class<?> clazz, Map<String, Object> classDefinitionMap, List<IFacetBuilderHelper> facetFields,
            List<IFilterBuilderHelper> filteredFields, Map<String, SourceFetchContext> fetchContexts, Map<String, Object> propertiesDefinitionMap,
            String pathPrefix, Indexable indexable) {
        String esFieldName = pathPrefix + indexable.getName();

        if (pathPrefix == null || pathPrefix.isEmpty()) {
            // Id, routing and boost are valid only for root object.
            processIdAnnotation(classDefinitionMap, esFieldName, indexable);
            processRoutingAnnotation(classDefinitionMap, esFieldName, indexable);
            processBoostAnnotation(classDefinitionMap, esFieldName, indexable);
        }

        processFetchContextAnnotation(fetchContexts, esFieldName, indexable);
        processFilterAnnotation(filteredFields, esFieldName, indexable);
        processFacetAnnotation(facetFields, filteredFields, esFieldName, indexable);

        // process the fields
        if (ClassUtils.isPrimitiveOrWrapper(indexable.getType()) || indexable.getType() == String.class) {
            processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, indexable);
        } else {
            // mapping of a complex field
            if (indexable.getType().isArray()) {
                // process the array type.
                Class<?> arrayType = indexable.getType().getComponentType();
                if (ClassUtils.isPrimitiveOrWrapper(arrayType) || arrayType == String.class) {
                    processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, indexable);
                } else {
                    if (arrayType.isEnum()) {
                        // if this is an enum and there is a String
                        StringField annotation = indexable.getAnnotation(StringField.class);
                        if (annotation != null) {
                            processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, indexable);
                        }
                    } else {
                        processComplexType(clazz, propertiesDefinitionMap, pathPrefix, indexable);
                    }
                }
            } else {
                // process the type
                processComplexType(clazz, propertiesDefinitionMap, pathPrefix, indexable);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processIdAnnotation(Map<String, Object> classDefinitionMap, String esFieldName, Indexable indexable) {
        Id id = indexable.getAnnotation(Id.class);
        if (id != null) {
            if (classDefinitionMap.containsKey("_id")) {
                LOGGER.warn("An Id annotation is defined on field <" + esFieldName + "> of <" + indexable.getDeclaringClassName()
                        + "> but an id has already be defined for <" + ((Map<String, Object>) classDefinitionMap.get("_id")).get("path") + ">");
            } else {
                classDefinitionMap.put("_id", MapUtil.getMap(new String[] { "path", "index", "store" }, new Object[] { esFieldName, id.index(), id.store() }));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processRoutingAnnotation(Map<String, Object> classDefinitionMap, String esFieldName, Indexable indexable) {
        Routing routing = indexable.getAnnotation(Routing.class);
        if (routing != null) {
            if (classDefinitionMap.containsKey("_routing")) {
                LOGGER.warn("A Routing annotation is defined on field <" + esFieldName + "> of <" + indexable.getDeclaringClassName()
                        + "> but a routing has already be defined for <" + ((Map<String, Object>) classDefinitionMap.get("_routing")).get("path") + ">");
            } else {
                Map<String, Object> routingDef = new HashMap<String, Object>();
                routingDef.put("path", esFieldName);
                routingDef.put("required", routing.required());
                classDefinitionMap.put("_routing", routingDef);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processBoostAnnotation(Map<String, Object> classDefinitionMap, String esFieldName, Indexable indexable) {
        Boost boost = indexable.getAnnotation(Boost.class);
        if (boost != null) {
            if (classDefinitionMap.containsKey("_boost")) {
                LOGGER.warn("A Boost annotation is defined on field <" + esFieldName + "> of <" + indexable.getDeclaringClassName()
                        + "> but a boost has already be defined for <" + ((Map<String, Object>) classDefinitionMap.get("_boost")).get("name") + ">");
            } else {
                Map<String, Object> boostDef = new HashMap<String, Object>();
                boostDef.put("name", esFieldName);
                boostDef.put("null_value", boost.nullValue());
                classDefinitionMap.put("_boost", boostDef);
            }
        }
    }

    private void processFetchContextAnnotation(Map<String, SourceFetchContext> fetchContexts, String esFieldName, Indexable indexable) {
        FetchContext fetchContext = indexable.getAnnotation(FetchContext.class);
        if (fetchContext == null) {
            return;
        }
        for (int i = 0; i < fetchContext.contexts().length; i++) {
            String context = fetchContext.contexts()[i];
            boolean isInclude = fetchContext.include()[i];

            SourceFetchContext sourceFetchContext = fetchContexts.get(context);
            if (sourceFetchContext == null) {
                sourceFetchContext = new SourceFetchContext();
                fetchContexts.put(context, sourceFetchContext);
            }
            if (isInclude) {
                sourceFetchContext.getIncludes().add(esFieldName);
            } else {
                sourceFetchContext.getExcludes().add(esFieldName);
            }
        }
    }

    private void processFilterAnnotation(List<IFilterBuilderHelper> classFilters, String esFieldName, Indexable indexable) {
        TermFilter termFilter = indexable.getAnnotation(TermFilter.class);
        if (termFilter != null) {
            String[] paths = termFilter.paths();
            for (String path : paths) {
                path = path.trim();
                boolean isAnalyzed = isAnalyzed(indexable);
                String nestedPath = indexable.getAnnotation(NestedObject.class) == null ? null : esFieldName;
                String filterPath = getFilterPath(path, nestedPath, esFieldName, indexable);
                if (filterPath == null) {
                    return;
                }

                classFilters.add(new TermsFilterBuilderHelper(isAnalyzed, nestedPath, filterPath));
            }
            return;
        }
        RangeFilter rangeFilter = indexable.getAnnotation(RangeFilter.class);
        if (rangeFilter != null) {
            IFilterBuilderHelper facetBuilderHelper = new RangeFilterBuilderHelper(null, esFieldName, rangeFilter);
            classFilters.add(facetBuilderHelper);
        }
    }

    private void processFacetAnnotation(List<IFacetBuilderHelper> classFacets, List<IFilterBuilderHelper> classFilters, String esFieldName, Indexable indexable) {
        TermsFacet termsFacet = indexable.getAnnotation(TermsFacet.class);
        if (termsFacet != null) {
            String[] paths = termsFacet.paths();
            for (String path : paths) {
                path = path.trim();
                boolean isAnalyzed = isAnalyzed(indexable);
                String nestedPath = indexable.getAnnotation(NestedObject.class) == null ? null : esFieldName;
                String filterPath = getFilterPath(path, nestedPath, esFieldName, indexable);
                if (filterPath == null) {
                    return;
                }

                IFacetBuilderHelper facetBuilderHelper = new TermsFacetBuilderHelper(isAnalyzed, nestedPath, filterPath, termsFacet);
                classFacets.add(facetBuilderHelper);
                if (classFilters.contains(facetBuilderHelper)) {
                    classFilters.remove(facetBuilderHelper);
                    LOGGER.warn("Field <" + esFieldName + "> already had a filter that will be replaced by the defined facet. Only a single one is allowed.");
                }
                classFilters.add(facetBuilderHelper);
            }
            return;
        }
        RangeFacet rangeFacet = indexable.getAnnotation(RangeFacet.class);
        if (rangeFacet != null) {
            IFacetBuilderHelper facetBuilderHelper = new RangeFacetBuilderHelper(null, esFieldName, rangeFacet);
            classFacets.add(facetBuilderHelper);
            if (classFilters.contains(facetBuilderHelper)) {
                classFilters.remove(facetBuilderHelper);
                LOGGER.warn("Field <" + esFieldName + "> already had a filter that will be replaced by the defined facet. Only a single one is allowed.");
            }
            classFilters.add(facetBuilderHelper);
        }
    }

    private String getFilterPath(String path, String nestedPath, String esFieldName, Indexable indexable) {
        String filterPath;
        if (nestedPath == null) {
            filterPath = path.isEmpty() ? esFieldName : esFieldName + "." + path;
        } else {
            if (path.isEmpty()) {
                LOGGER.warn("Unable to map filter for field <" + esFieldName + "> Nested objects requires to specify a path on the filter.");
                return null;
            }
            filterPath = path;
        }
        return filterPath;
    }

    private boolean isAnalyzed(Indexable indexable) {
        boolean isAnalysed = true;
        StringField stringFieldAnnotation = indexable.getAnnotation(StringField.class);
        if (stringFieldAnnotation != null) {
            if (!IndexType.analyzed.equals(stringFieldAnnotation.indexType())) {
                isAnalysed = false;
            }
        }
        return isAnalysed;
    }

    private void processStringOrPrimitive(Class<?> clazz, Map<String, Object> propertiesDefinitionMap, String pathPrefix, Indexable indexable) {
        processFieldAnnotation(IndexName.class, new IndexNameAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);
        processFieldAnnotation(NullValue.class, new NullValueAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);

        // String field annotations.
        processFieldAnnotation(StringField.class, new StringFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);
        processFieldAnnotation(Analyser.class, new AnalyserAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);
        processFieldAnnotation(IndexAnalyser.class, new IndexAnalyserAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);
        processFieldAnnotation(SearchAnalyser.class, new SearchAnalyserAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);

        // Numeric field annotation
        processFieldAnnotation(NumberField.class, new NumberFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);

        // Date field annotation
        processFieldAnnotation(DateField.class, new DateFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);
        processFieldAnnotation(DateFormat.class, new DateFormatAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);

        // Boolean field annotation
        processFieldAnnotation(BooleanField.class, new BooleanFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix, indexable);
        // TODO binary type mapping
    }

    private void processComplexType(Class<?> clazz, Map<String, Object> propertiesDefinitionMap, String pathPrefix, Indexable indexable) {
        // TODO check annotations
        NestedObjectFieldAnnotationParser parser = new NestedObjectFieldAnnotationParser(this);
        // process nested object
        processFieldAnnotation(NestedObject.class, parser, propertiesDefinitionMap, pathPrefix, indexable);

    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> void processFieldAnnotation(Class<T> annotationClass, IPropertyAnnotationParser<T> propertyAnnotationParser,
            Map<String, Object> propertiesDefinitionMap, String pathPrefix, Indexable indexable) {
        T annotation = indexable.getAnnotation(annotationClass);
        if (annotation != null) {
            Map<String, Object> fieldDefinition = (Map<String, Object>) propertiesDefinitionMap.get(indexable.getName());
            if (fieldDefinition == null) {
                fieldDefinition = new HashMap<String, Object>();
                propertiesDefinitionMap.put(indexable.getName(), fieldDefinition);
            }
            propertyAnnotationParser.parseAnnotation(annotation, fieldDefinition, pathPrefix, indexable);
        }
    }

    /**
     * Get all indexable member of a class (field or property)
     * 
     * @param clazz class to check
     * @return list of all indexable members
     * @throws IntrospectionException
     */
    private List<Indexable> getIndexables(Class<?> clazz) throws IntrospectionException {
        List<Indexable> indexables = new ArrayList<Indexable>();
        Map<String, PropertyDescriptor> pdMap = getValidPropertyDescriptorMap(clazz);
        Map<String, Field> fdMap = new HashMap<String, Field>();

        Field[] fdArr = clazz.getDeclaredFields();
        for (Field field : fdArr) {
            // Check not transient field
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            PropertyDescriptor propertyDescriptor = pdMap.get(field.getName());
            if (propertyDescriptor == null || propertyDescriptor.getReadMethod() == null || propertyDescriptor.getWriteMethod() == null) {
                LOGGER.debug("Field <" + field.getName() + "> of class <" + clazz.getName() + "> has no proper setter/getter and won't be persisted.");
                continue;
            }

            fdMap.put(field.getName(), field);
        }
        Set<String> allIndexablesName = new HashSet<String>();
        allIndexablesName.addAll(pdMap.keySet());
        allIndexablesName.addAll(fdMap.keySet());
        for (String name : allIndexablesName) {
            indexables.add(new Indexable(fdMap.get(name), pdMap.get(name)));
        }
        return indexables;
    }

    private Map<String, PropertyDescriptor> getValidPropertyDescriptorMap(Class<?> clazz) throws IntrospectionException {
        Map<String, PropertyDescriptor> pdMap = new HashMap<String, PropertyDescriptor>();
        PropertyDescriptor[] pdArr = Introspector.getBeanInfo(clazz, clazz.getSuperclass()).getPropertyDescriptors();

        if (pdArr == null) {
            return pdMap;
        }

        for (PropertyDescriptor pd : pdArr) {
            // Check valid getter setter
            if (pd.getReadMethod() != null && pd.getWriteMethod() != null) {
                pdMap.put(pd.getName(), pd);
            }
        }

        return pdMap;
    }
}
