package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import org.apache.commons.beanutils.PropertyUtils;

import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.*;
import org.elasticsearch.mapping.parser.*;
import org.elasticsearch.util.MapUtil;
import org.springframework.util.ClassUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Process fields in a class to fill-in the properties entry in the class definition map.
 * 
 */
@Slf4j
public class FieldsMappingBuilder {

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
            List<IFilterBuilderHelper> filteredFields, Map<String, SourceFetchContext> fetchContexts, String pathPrefix, String nestedPrefix, boolean isAll)
            throws IntrospectionException {
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            parseFieldMappings(clazz.getSuperclass(), classDefinitionMap, facetFields, filteredFields, fetchContexts, pathPrefix, nestedPrefix, isAll);
        }

        List<Indexable> indexables = getIndexables(clazz);

        Map<String, Object> propertiesDefinitionMap = (Map<String, Object>) classDefinitionMap.get("properties");
        if (propertiesDefinitionMap == null) {
            propertiesDefinitionMap = new HashMap<String, Object>();
            classDefinitionMap.put("properties", propertiesDefinitionMap);
        }

        for (Indexable indexable : indexables) {
            parseFieldMappings(clazz, classDefinitionMap, facetFields, filteredFields, fetchContexts, propertiesDefinitionMap, pathPrefix, nestedPrefix,
                    indexable);
        }
        if (isAll) {
            Map<String, String> fieldsDefinitionMap = new HashMap<String, String>();
            fieldsDefinitionMap.put ("type", "text");
            propertiesDefinitionMap.put ("all", fieldsDefinitionMap);
        }
    }

    private void parseFieldMappings(Class<?> clazz, Map<String, Object> classDefinitionMap, List<IFacetBuilderHelper> facetFields,
            List<IFilterBuilderHelper> filteredFields, Map<String, SourceFetchContext> fetchContexts, Map<String, Object> propertiesDefinitionMap,
            String pathPrefix, String nestedPrefix, Indexable indexable) {
        String esFieldName = pathPrefix + indexable.getName();

        if (pathPrefix == null || pathPrefix.isEmpty()) {
            // Id, routing and boost are valid only for root object.
            //processIdAnnotation(classDefinitionMap, esFieldName, indexable);
            processRoutingAnnotation(classDefinitionMap, esFieldName, indexable);
            processBoostAnnotation(classDefinitionMap, esFieldName, indexable);
            // Timestamp field annotation
            //processTimeStampAnnotation(classDefinitionMap, esFieldName, indexable);
        }

        processFetchContextAnnotation(fetchContexts, esFieldName, indexable);
        processFilterAnnotation(filteredFields, nestedPrefix, esFieldName, indexable);
        processFacetAnnotation(facetFields, filteredFields, esFieldName, indexable);

        // process the fields
        if (ClassUtils.isPrimitiveOrWrapper(indexable.getType()) || indexable.getType() == String.class || indexable.getType() == Date.class) {
            processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
        } else if (indexable.getType().isEnum()) {
            StringField annotation = indexable.getAnnotation(StringField.class);
            if (annotation != null) {
                processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
            }
        } else if (Map.class.isAssignableFrom(indexable.getType())) {
            MapKeyValue annotation = indexable.getAnnotation(MapKeyValue.class);
            if (annotation != null) {
                // Create an object mapping with key and value
                processFieldAnnotation(MapKeyValue.class, new MapKeyValueAnnotationParser(this, filteredFields, facetFields), propertiesDefinitionMap,
                        pathPrefix, nestedPrefix, indexable);
            } else {
                processComplexOrArray(clazz, facetFields, filteredFields, pathPrefix, nestedPrefix, propertiesDefinitionMap, indexable);
            }
        } else {
            processComplexOrArray(clazz, facetFields, filteredFields, pathPrefix, nestedPrefix, propertiesDefinitionMap, indexable);
        }
    }

    private void processComplexOrArray(Class<?> clazz, List<IFacetBuilderHelper> facetFields, List<IFilterBuilderHelper> filteredFields, String pathPrefix,
            String nestedPrefix, Map<String, Object> propertiesDefinitionMap, Indexable indexable) {
        // mapping of a complex field
        if (indexable.isArrayOrCollection()) {
            Class<?> arrayType = indexable.getComponentType();
            // process the array type.
            if (ClassUtils.isPrimitiveOrWrapper(arrayType) || arrayType == String.class || indexable.getType() == Date.class) {
                processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
            } else if (arrayType.isEnum()) {
                // if this is an enum and there is a String
                StringField annotation = indexable.getAnnotation(StringField.class);
                if (annotation != null) {
                    processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
                }
            } else {
                processComplexType(clazz, propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable, filteredFields, facetFields);
            }
        } else {
            // process the type
            processComplexType(clazz, propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable, filteredFields, facetFields);
        }
    }

    @SuppressWarnings("unchecked")
    private void processIdAnnotation(Map<String, Object> classDefinitionMap, String esFieldName, Indexable indexable) {
        Id id = indexable.getAnnotation(Id.class);
        if (id != null) {
            if (classDefinitionMap.containsKey("_id")) {
                log.warn("An Id annotation is defined on field <" + esFieldName + "> of <" + indexable.getDeclaringClassName()
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
                log.warn("A Routing annotation is defined on field <" + esFieldName + "> of <" + indexable.getDeclaringClassName()
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
                log.warn("A Boost annotation is defined on field <" + esFieldName + "> of <" + indexable.getDeclaringClassName()
                        + "> but a boost has already be defined for <" + ((Map<String, Object>) classDefinitionMap.get("_boost")).get("name") + ">");
            } else {
                Map<String, Object> boostDef = new HashMap<String, Object>();
                boostDef.put("name", esFieldName);
                boostDef.put("null_value", boost.nullValue());
                classDefinitionMap.put("_boost", boostDef);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processTimeStampAnnotation(Map<String, Object> classDefinitionMap, String esFieldName, Indexable indexable) {
        TimeStamp timeStamp = indexable.getAnnotation(TimeStamp.class);
        if (timeStamp != null) {
            if (classDefinitionMap.containsKey("_timestamp")) {
                log.warn("A TimeStamp annotation is defined on field <" + esFieldName + "> of <" + indexable.getDeclaringClassName()
                        + "> but a boost has already be defined for <" + ((Map<String, Object>) classDefinitionMap.get("_timestamp")).get("name") + ">");
            } else {
                Map<String, Object> timeStampDefinition = new HashMap<String, Object>();
                timeStampDefinition.put("enabled", true);
                timeStampDefinition.put("path", indexable.getName());

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

    private void processFilterAnnotation(List<IFilterBuilderHelper> classFilters, String nestedPrefix, String esFieldName, Indexable indexable) {
        TermFilter termFilter = indexable.getAnnotation(TermFilter.class);
        if (termFilter != null) {
            String[] paths = termFilter.paths();
            if (termFilter.pathGenerator() != null) {
                // create an instance of the generator
                try {
                    IPathGenerator generator = termFilter.pathGenerator().newInstance();
                    paths = generator.getPaths(paths);
                } catch (InstantiationException e) {
                    e.printStackTrace(); // TODO better exception handling
                } catch (IllegalAccessException e) {
                    e.printStackTrace(); // TODO better exception handling
                }
            }
            for (String path : paths) {
                path = path.trim();
                addFilter(classFilters, nestedPrefix, esFieldName, indexable, path, isAnalyzed(indexable, null));
                for (String alternateFieldName : alternateFieldNames(indexable)) {
                    //addFilter(classFilters, nestedPrefix, alternateFieldName, indexable, path, isAnalyzed(indexable, alternateFieldName));
                    addFilter(classFilters, nestedPrefix, esFieldName + "." + alternateFieldName, indexable, path, isAnalyzed(indexable, alternateFieldName));
                }
            }
            return;
        }
        RangeFilter rangeFilter = indexable.getAnnotation(RangeFilter.class);
        if (rangeFilter != null) {
            IFilterBuilderHelper facetBuilderHelper = new RangeFilterBuilderHelper(null, esFieldName, rangeFilter);
            classFilters.add(facetBuilderHelper);
        }
    }

    private void addFilter(List<IFilterBuilderHelper> classFilters, String nestedPrefix, String esFieldName, Indexable indexable, String path,
            boolean isAnalyzed) {
        if (nestedPrefix != null) {
            esFieldName = esFieldName.substring(nestedPrefix.length() + 1);
        }
        String filterPath = getFilterPath(path, esFieldName);

        classFilters.add(new TermsFilterBuilderHelper(isAnalyzed, nestedPrefix, filterPath));
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

                addAggregation(termsFacet, indexable, esFieldName, path, isAnalyzed(indexable, null), classFacets, classFilters);
                for (String alternateFieldName : alternateFieldNames(indexable)) {
                    addAggregation(termsFacet, indexable, alternateFieldName, path, isAnalyzed(indexable, alternateFieldName), classFacets, classFilters);
                }
            }
            return;
        }
        RangeFacet rangeFacet = indexable.getAnnotation(RangeFacet.class);
        if (rangeFacet != null) {
            IFacetBuilderHelper facetBuilderHelper = new RangeAggregationBuilderHelper(null, esFieldName, rangeFacet);
            classFacets.add(facetBuilderHelper);
            if (classFilters.contains(facetBuilderHelper)) {
                classFilters.remove(facetBuilderHelper);
                log.warn("Field <" + esFieldName + "> already had a filter that will be replaced by the defined facet. Only a single one is allowed.");
            }
            classFilters.add(facetBuilderHelper);
        }
    }

    private String getFilterPath(String path, String esFieldName) {
        return path.isEmpty() ? esFieldName : esFieldName + "." + path;
    }

    private boolean isAnalyzed(Indexable indexable, String name) {
        boolean isAnalysed = true;
        StringFieldMulti multiAnnotation = indexable.getAnnotation(StringFieldMulti.class);
        if (multiAnnotation != null) {
            if (name == null) {
                return IndexType.analyzed.equals(multiAnnotation.main().indexType());
            } else {
                for (int i = 0; i < multiAnnotation.multiNames().length; i++) {
                    if (multiAnnotation.multiNames()[i].equals(name)) {
                        return IndexType.analyzed.equals(multiAnnotation.multi()[i].indexType());
                    }
                }
            }
        }

        StringField stringFieldAnnotation = indexable.getAnnotation(StringField.class);
        if (stringFieldAnnotation != null) {
            return IndexType.analyzed.equals(stringFieldAnnotation.indexType());
        }
        return isAnalysed;
    }

    private void addAggregation(TermsFacet termsFacet, Indexable indexable, String esFieldName, String path, boolean isAnalyzed,
            List<IFacetBuilderHelper> classFacets, List<IFilterBuilderHelper> classFilters) {
        String nestedPath = indexable.getAnnotation(NestedObject.class) == null ? null : esFieldName;
        String filterPath = getFilterPath(path, esFieldName);

        IFacetBuilderHelper facetBuilderHelper = new TermsAggregationBuilderHelper(isAnalyzed, nestedPath, filterPath, termsFacet);
        classFacets.add(facetBuilderHelper);
        if (classFilters.contains(facetBuilderHelper)) {
            classFilters.remove(facetBuilderHelper);
            log.warn("Field <" + esFieldName + "> already had a filter that will be replaced by the defined facet. Only a single one is allowed.");
        }
        classFilters.add(facetBuilderHelper);
    }

    private String[] alternateFieldNames(Indexable indexable) {
        StringFieldMulti multi = indexable.getAnnotation(StringFieldMulti.class);
        if (multi == null) {
            return new String[0];
        }
        return multi.multiNames();
    }

    private void processStringOrPrimitive(Class<?> clazz, Map<String, Object> propertiesDefinitionMap, String pathPrefix, String nestedPrefix,
            Indexable indexable) {
        processFieldAnnotation(IndexName.class, new IndexNameAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
        processFieldAnnotation(NullValue.class, new NullValueAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);

        // String field annotations.
        processFieldAnnotation(StringField.class, new StringFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
        processFieldAnnotation(StringFieldMulti.class, new StringFieldMultiAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
        processFieldAnnotation(Analyser.class, new AnalyserAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
        processFieldAnnotation(IndexAnalyser.class, new IndexAnalyserAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
        processFieldAnnotation(SearchAnalyser.class, new SearchAnalyserAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);

        // Numeric field annotation
        processFieldAnnotation(NumberField.class, new NumberFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);

        // Date field annotation
        processFieldAnnotation(DateField.class, new DateFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
        processFieldAnnotation(DateFormat.class, new DateFormatAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);

        // Boolean field annotation
        processFieldAnnotation(BooleanField.class, new BooleanFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
        // TODO binary type mapping
    }

    private void processComplexType(Class<?> clazz, Map<String, Object> propertiesDefinitionMap, String pathPrefix, String nestedPrefix, Indexable indexable,
            List<IFilterBuilderHelper> filters, List<IFacetBuilderHelper> facets) {
        NestedObjectFieldAnnotationParser nested = new NestedObjectFieldAnnotationParser(this, filters, facets);
        processFieldAnnotation(NestedObject.class, nested, propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);

        if (propertiesDefinitionMap.get(indexable.getName()) == null) {
            ObjectFieldAnnotationParser objectFieldAnnotationParser = new ObjectFieldAnnotationParser(this, filters, facets);
            processFieldAnnotation(ObjectField.class, objectFieldAnnotationParser, propertiesDefinitionMap, pathPrefix, nestedPrefix, indexable);
            // by default we consider the complex object as an object mapping and process recursive mapping of every field just as ES would process based on
            // dynamic
            // mapping
            if (propertiesDefinitionMap.get(indexable.getName()) == null) {
                // Define mapping as object
                Map<String, Object> fieldDefinition = (Map<String, Object>) propertiesDefinitionMap.get(indexable.getName());
                if (fieldDefinition == null) {
                    fieldDefinition = new HashMap<String, Object>();
                    propertiesDefinitionMap.put(indexable.getName(), fieldDefinition);
                }
                objectFieldAnnotationParser.parseAnnotation(null, fieldDefinition, pathPrefix, nestedPrefix, indexable);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> void processFieldAnnotation(Class<T> annotationClass, IPropertyAnnotationParser<T> propertyAnnotationParser,
            Map<String, Object> propertiesDefinitionMap, String pathPrefix, String nestedPrefix, Indexable indexable) {
        T annotation = indexable.getAnnotation(annotationClass);
        if (annotation != null) {
            Map<String, Object> fieldDefinition = (Map<String, Object>) propertiesDefinitionMap.get(indexable.getName());
            if (fieldDefinition == null) {
                fieldDefinition = new HashMap<String, Object>();
                propertiesDefinitionMap.put(indexable.getName(), fieldDefinition);
            }
            propertyAnnotationParser.parseAnnotation(annotation, fieldDefinition, pathPrefix, nestedPrefix, indexable);
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
                log.debug("Field <" + field.getName() + "> of class <" + clazz.getName() + "> has no proper setter/getter and won't be persisted.");
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

    @SuppressWarnings("unchecked")
    public String getIdValue (Object obj) throws IntrospectionException, IllegalAccessException, InvocationTargetException,
												 NoSuchMethodException  {
	String value = null;
	Class<?> clazz = obj.getClass();
	List<Indexable> indexables = getIndexables(clazz);

	for (Indexable indexable : indexables) {
		Id id = indexable.getAnnotation(Id.class);
		if (id != null) {
			String name = indexable.getName();
			value = (String)(PropertyUtils.getSimpleProperty(obj, name));
		}			 
	}

	if (value == null) {
		try  {
			Method getIdMethod = clazz.getMethod ("getId");
			Object oresult = getIdMethod.invoke(obj);
			value = (String)oresult;
		} catch (Exception e) {}
	}

	return value;
    }

}
