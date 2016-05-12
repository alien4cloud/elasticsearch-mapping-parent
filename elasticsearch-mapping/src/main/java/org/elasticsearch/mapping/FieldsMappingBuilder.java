package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.*;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.parser.*;
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
            // Timestamp field annotation
            processTimeStampAnnotation(classDefinitionMap, esFieldName, indexable);
        }

        processFetchContextAnnotation(fetchContexts, esFieldName, indexable);
        processFilterAnnotation(filteredFields, esFieldName, indexable);
        processFacetAnnotation(facetFields, filteredFields, esFieldName, indexable);

        // process the fields
        if (ClassUtils.isPrimitiveOrWrapper(indexable.getType()) || indexable.getType() == String.class) {
            processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, indexable);
        } else {
            Class<?> arrayType = indexable.getComponentType();
            // mapping of a complex field
            if (arrayType != null) {
                // process the array type.
                if (ClassUtils.isPrimitiveOrWrapper(arrayType) || arrayType == String.class) {
                    processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, indexable);
                } else if (arrayType.isEnum()) {
                    // if this is an enum and there is a String
                    StringField annotation = indexable.getAnnotation(StringField.class);
                    if (annotation != null) {
                        processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, indexable);
                    }
                } else {
                    processComplexType(clazz, propertiesDefinitionMap, pathPrefix, indexable, filteredFields, facetFields);
                }
            } else {
                // process the type
                processComplexType(clazz, propertiesDefinitionMap, pathPrefix, indexable, filteredFields, facetFields);
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
                classDefinitionMap.put("_id", MapUtil.getMap(new String[] { "path" }, new Object[] { indexable }));
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
                routingDef.put("path", indexable);
                routingDef.put("required", routing.required());
                classDefinitionMap.put("_routing", routingDef);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processTimeStampAnnotation(Map<String, Object> classDefinitionMap, String esFieldName, Indexable indexable) {
        TimeStamp timeStamp = indexable.getAnnotation(TimeStamp.class);
        if (timeStamp != null) {
            if (classDefinitionMap.containsKey("_timestamp")) {
                LOGGER.warn("A TimeStamp annotation is defined on field <" + esFieldName + "> of <" + indexable.getDeclaringClassName()
                        + "> but a timestamp has already be defined for <" + ((Map<String, Object>) classDefinitionMap.get("_timestamp")).get("name") + ">");
            } else {
                Map<String, Object> timeStampDefinition = new HashMap<String, Object>();
                timeStampDefinition.put("enabled", true);
                timeStampDefinition.put("path", indexable);

                if (!timeStamp.format().isEmpty()) {
                    timeStampDefinition.put("format", timeStamp.format());
                }

                classDefinitionMap.put("_timestamp", timeStampDefinition);
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
                if (nestedPath == null) {
                    int nestedIndicator = esFieldName.lastIndexOf(".");
                    if (nestedIndicator > 0) {
                        nestedPath = esFieldName.substring(0, nestedIndicator);
                        esFieldName = esFieldName.substring(nestedIndicator + 1, esFieldName.length());
                    }
                }
                String filterPath = getFilterPath(path, esFieldName);

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

    private void processFacetAnnotation(List<IFacetBuilderHelper> classFacets, List<IFilterBuilderHelper> classFilters, String esFieldName,
            Indexable indexable) {
        TermsFacet termsFacet = indexable.getAnnotation(TermsFacet.class);
        if (termsFacet != null) {
            String[] paths = termsFacet.paths();
            if (termsFacet.pathGenerator() != null) {
                // create an instance of the generator
                try {
                    IPathGenerator generator = termsFacet.pathGenerator().newInstance();
                    paths = generator.getPaths(paths);
                } catch (InstantiationException e) {
                    e.printStackTrace(); // TODO better exception handling
                } catch (IllegalAccessException e) {
                    e.printStackTrace(); // TODO better exception handling
                }
            }
            for (String path : paths) {
                path = path.trim();
                boolean isAnalyzed = isAnalyzed(indexable);
                String nestedPath = indexable.getAnnotation(NestedObject.class) == null ? null : esFieldName;
                String filterPath = getFilterPath(path, esFieldName);

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

    private String getFilterPath(String path, String esFieldName) {
        return path.isEmpty() ? esFieldName : esFieldName + "." + path;
    }

    private boolean isAnalyzed(Indexable indexable) {
        boolean isAnalysed = true;
        StringField stringFieldAnnotation = indexable.getAnnotation(StringField.class);
        if (stringFieldAnnotation != null && !IndexType.analyzed.equals(stringFieldAnnotation.indexType())) {
            isAnalysed = false;
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

    private void processComplexType(Class<?> clazz, Map<String, Object> propertiesDefinitionMap, String pathPrefix, Indexable indexable,
            List<IFilterBuilderHelper> filters, List<IFacetBuilderHelper> facets) {
        NestedObjectFieldAnnotationParser parser = new NestedObjectFieldAnnotationParser(this, filters, facets);
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

            String pdName = field.getName();
            if (field.getType().equals(Boolean.TYPE) && field.getName().startsWith("is")) {
                pdName = field.getName().substring(2, 3).toLowerCase() + field.getName().substring(3, field.getName().length());
            }
            PropertyDescriptor propertyDescriptor = pdMap.get(pdName);

            if (propertyDescriptor == null || propertyDescriptor.getReadMethod() == null || propertyDescriptor.getWriteMethod() == null) {
                LOGGER.debug("Field <" + field.getName() + "> of class <" + clazz.getName() + "> has no proper setter/getter and won't be persisted.");
            } else {
                fdMap.put(pdName, field);
            }
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
