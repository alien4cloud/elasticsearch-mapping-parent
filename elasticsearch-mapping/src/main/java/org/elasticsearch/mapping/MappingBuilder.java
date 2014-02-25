package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
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
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.IndexAnalyser;
import org.elasticsearch.annotation.IndexName;
import org.elasticsearch.annotation.NullValue;
import org.elasticsearch.annotation.NumberField;
import org.elasticsearch.annotation.Routing;
import org.elasticsearch.annotation.SearchAnalyser;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.TypeName;
import org.elasticsearch.annotation.query.RangeFacet;
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
import org.elasticsearch.mapping.parser.NullValueAnnotationParser;
import org.elasticsearch.mapping.parser.NumberFieldAnnotationParser;
import org.elasticsearch.mapping.parser.SearchAnalyserAnnotationParser;
import org.elasticsearch.mapping.parser.StringFieldAnnotationParser;
import org.elasticsearch.util.AnnotationScanner;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helps to parse the ES annotations.
 * 
 * @author luc boutier
 */
@Component
@Scope("singleton")
public class MappingBuilder {
    private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);
    private Map<String, String> classesMappings = new HashMap<String, String>();
    private Map<String, String> typeByClassName = new HashMap<String, String>();
    private Map<String, List<IFacetBuilderHelper>> facetByClassName = new HashMap<String, List<IFacetBuilderHelper>>();

    /**
     * Build mapping for the given packages.
     * 
     * @param packages list of packages in which to look for {@link ESObject} annotated classes.
     * @throws IntrospectionException Java reflection usage related exception.
     * @throws IOException In case of an IO error while creating Json.
     * @throws JsonMappingException In case of an error while creating mapping json.
     * @throws JsonGenerationException In case of an error while creating mapping json.
     */
    public void initialize(String... packages) throws IntrospectionException, JsonGenerationException,
            JsonMappingException, IOException {
        if (packages != null && packages.length > 0) {
            for (String packageName : packages) {
                initialize(packageName);
            }
        }
    }

    /**
     * Get the mapping json string for a given class.
     * 
     * @param clazz The class for which to get the mapping.
     * @return The json mapping for this class.
     * @throws JsonGenerationException In case the mapping failed to be serialized to json.
     * @throws JsonMappingException In case we fail to build the mapping.
     * @throws IntrospectionException instrospection error.
     * @throws IOException io error.
     */
    public String getMapping(Class<?> clazz) throws JsonGenerationException, JsonMappingException,
            IntrospectionException, IOException {
        String classMapping = classesMappings.get(clazz.getName());
        if (classMapping == null) {
            parseClassMapping(clazz, "");
        }
        classMapping = classesMappings.get(clazz.getName());
        return classMapping;
    }

    /**
     * Get the name of the type in elastic search for the given class.
     * 
     * @param clazz The class for which to get the type.
     * @return The type name in elastic search.
     */
    public String getTypeName(Class<?> clazz) {
        return this.typeByClassName.get(clazz.getName());
    }

    /**
     * Get the list of es fields that should be faceted in a facet search for the given class.
     * 
     * @param clazz The class for which to get the facets.
     * @return The list of facet builders for this class.
     */
    public List<IFacetBuilderHelper> getFacets(Class<?> clazz) {
        return this.facetByClassName.get(clazz.getName());
    }

    /**
     * Get the list of es fields that should be faceted in a facet search for the given class.
     * 
     * @param className The name for which to get the facets.
     * @return The list of facet builders for this class.
     */
    public List<IFacetBuilderHelper> getFacets(String className) {
        return this.facetByClassName.get(className);
    }

    private void initialize(String packageName) throws IntrospectionException, JsonGenerationException,
            JsonMappingException, IOException {
        Set<Class<?>> classSet = org.elasticsearch.util.AnnotationScanner.scan(packageName, ESObject.class);
        for (Class<?> clazz : classSet) {
            parseClassMapping(clazz, "");
        }
    }

    private void parseClassMapping(Class<?> clazz, String pathPrefix) throws IntrospectionException,
            JsonGenerationException, JsonMappingException, IOException {
        ESObject esObject = AnnotationScanner.getAnnotation(ESObject.class, clazz);
        TypeName typeName = clazz.getAnnotation(TypeName.class);
        String typeNameStr;
        if (typeName == null) {
            typeNameStr = clazz.getSimpleName().toLowerCase();
        } else {
            typeNameStr = typeName.typeName();
        }

        Map<String, Object> typeDefinitionMap = new HashMap<String, Object>();
        Map<String, Object> classDefinitionMap = new HashMap<String, Object>();
        List<IFacetBuilderHelper> facetFields = new ArrayList<IFacetBuilderHelper>();

        typeDefinitionMap.put(typeNameStr, classDefinitionMap);

        classDefinitionMap.put("_all", getMap("enabled", esObject.all()));
        classDefinitionMap.put("_source", getMap("enabled", esObject.source()));
        classDefinitionMap.put("_type",
                getMap(new String[] { "store", "index" }, new Object[] { esObject.store(), esObject.index() }));

        parseFieldMappings(clazz, classDefinitionMap, facetFields, pathPrefix);

        ObjectMapper mapper = new ObjectMapper();
        String jsonMapping = mapper.writeValueAsString(typeDefinitionMap);
        this.classesMappings.put(clazz.getName(), jsonMapping);
        this.typeByClassName.put(clazz.getName(), typeNameStr);
        this.facetByClassName.put(clazz.getName(), facetFields);
    }

    private Map<String, Object> getMap(String key, Object value) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key, value);
        return map;
    }

    private Map<String, Object> getMap(String[] keys, Object[] values) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private void parseFieldMappings(Class<?> clazz, Map<String, Object> classDefinitionMap,
            List<IFacetBuilderHelper> facetFields, String pathPrefix) throws IntrospectionException {
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            parseFieldMappings(clazz.getSuperclass(), classDefinitionMap, facetFields, pathPrefix);
        }

        List<Indexable> indexables = getIndexables(clazz);

        Map<String, Object> propertiesDefinitionMap = (Map<String, Object>) classDefinitionMap.get("properties");
        if (propertiesDefinitionMap == null) {
            propertiesDefinitionMap = new HashMap<String, Object>();
            classDefinitionMap.put("properties", propertiesDefinitionMap);
        }

        for (Indexable indexable : indexables) {
            parseFieldMappings(clazz, classDefinitionMap, facetFields, propertiesDefinitionMap, pathPrefix, indexable);
        }
    }

    private void parseFieldMappings(Class<?> clazz, Map<String, Object> classDefinitionMap,
            List<IFacetBuilderHelper> facetFields, Map<String, Object> propertiesDefinitionMap, String pathPrefix,
            Indexable indexable) {
        String esFieldName = pathPrefix + indexable.getName();

        processIdAnnotation(classDefinitionMap, esFieldName, indexable);
        processRoutingAnnotation(classDefinitionMap, esFieldName, indexable);
        processBoostAnnotation(classDefinitionMap, esFieldName, indexable);

        // process facet annotation
        processFacetAnnotation(facetFields, esFieldName, indexable);

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
                LOGGER.warn("An Id annotation is defined on field <" + esFieldName + "> of <"
                        + indexable.getDeclaringClassName() + "> but an id has already be defined for <"
                        + ((Map<String, Object>) classDefinitionMap.get("_id")).get("path") + ">");
            } else {
                classDefinitionMap.put(
                        "_id",
                        getMap(new String[] { "path", "index", "store" },
                                new Object[] { esFieldName, id.index(), id.store() }));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processRoutingAnnotation(Map<String, Object> classDefinitionMap, String esFieldName, Indexable indexable) {
        Routing routing = indexable.getAnnotation(Routing.class);
        if (routing != null) {
            if (classDefinitionMap.containsKey("_routing")) {
                LOGGER.warn("A Routing annotation is defined on field <" + esFieldName + "> of <"
                        + indexable.getDeclaringClassName() + "> but a routing has already be defined for <"
                        + ((Map<String, Object>) classDefinitionMap.get("_routing")).get("path") + ">");
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
                LOGGER.warn("A Boost annotation is defined on field <" + esFieldName + "> of <"
                        + indexable.getDeclaringClassName() + "> but a boost has already be defined for <"
                        + ((Map<String, Object>) classDefinitionMap.get("_boost")).get("name") + ">");
            } else {
                Map<String, Object> boostDef = new HashMap<String, Object>();
                boostDef.put("name", esFieldName);
                boostDef.put("null_value", boost.nullValue());
                classDefinitionMap.put("_boost", boostDef);
            }
        }
    }

    private void processFacetAnnotation(List<IFacetBuilderHelper> classFacets, String esFieldName, Indexable indexable) {
        TermsFacet termsFacet = indexable.getAnnotation(TermsFacet.class);
        if (termsFacet != null) {
            classFacets.add(new TermsFacetBuilderHelper(esFieldName, termsFacet));
            return;
        }
        RangeFacet rangeFacet = indexable.getAnnotation(RangeFacet.class);
        if (rangeFacet != null) {
            classFacets.add(new RangeFacetBuilderHelper(esFieldName, rangeFacet));
        }
    }

    private void processStringOrPrimitive(Class<?> clazz, Map<String, Object> propertiesDefinitionMap,
            String pathPrefix, Indexable indexable) {
        processFieldAnnotation(IndexName.class, new IndexNameAnnotationParser(), propertiesDefinitionMap, pathPrefix,
                indexable);
        processFieldAnnotation(NullValue.class, new NullValueAnnotationParser(), propertiesDefinitionMap, pathPrefix,
                indexable);

        // String field annotations.
        processFieldAnnotation(StringField.class, new StringFieldAnnotationParser(), propertiesDefinitionMap,
                pathPrefix, indexable);
        processFieldAnnotation(Analyser.class, new AnalyserAnnotationParser(), propertiesDefinitionMap, pathPrefix,
                indexable);
        processFieldAnnotation(IndexAnalyser.class, new IndexAnalyserAnnotationParser(), propertiesDefinitionMap,
                pathPrefix, indexable);
        processFieldAnnotation(SearchAnalyser.class, new SearchAnalyserAnnotationParser(), propertiesDefinitionMap,
                pathPrefix, indexable);

        // Numeric field annotation
        processFieldAnnotation(NumberField.class, new NumberFieldAnnotationParser(), propertiesDefinitionMap,
                pathPrefix, indexable);

        // Date field annotation
        processFieldAnnotation(DateField.class, new DateFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix,
                indexable);
        processFieldAnnotation(DateFormat.class, new DateFormatAnnotationParser(), propertiesDefinitionMap, pathPrefix,
                indexable);

        // Boolean field annotation
        processFieldAnnotation(BooleanField.class, new BooleanFieldAnnotationParser(), propertiesDefinitionMap,
                pathPrefix, indexable);
        // TODO binary type mapping
    }

    private void processComplexType(Class<?> clazz, Map<String, Object> propertiesDefinitionMap, String pathPrefix, Indexable indexable) {
        // TODO check annotations
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> void processFieldAnnotation(Class<T> annotationClass,
            IPropertyAnnotationParser<T> propertyAnnotationParser, Map<String, Object> propertiesDefinitionMap,
            String pathPrefix, Indexable indexable) {
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
            if (propertyDescriptor == null || propertyDescriptor.getReadMethod() == null
                    || propertyDescriptor.getWriteMethod() == null) {
                LOGGER.debug("Field <" + field.getName() + "> of class <" + clazz.getName()
                        + "> has no proper setter/getter and won't be persisted.");
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