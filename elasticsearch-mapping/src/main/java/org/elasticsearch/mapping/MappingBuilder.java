package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

import org.elasticsearch.annotation.ESAll;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.IndexAnalyserDefinition;
import org.elasticsearch.annotation.TypeName;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.util.AnnotationScanner;
import org.elasticsearch.util.MapUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private FieldsMappingBuilder fieldsMappingBuilder = new FieldsMappingBuilder();

    private Map<String, String> classesMappings = new HashMap<String, String>();
    private Map<String, String> settingsByClassName = new HashMap<String, String>();
    private Map<String, String> typeByClassName = new HashMap<String, String>();

    private Map<String, List<IFilterBuilderHelper>> filtersByClassName = new HashMap<String, List<IFilterBuilderHelper>>();
    private Map<String, List<IFacetBuilderHelper>> facetByClassName = new HashMap<String, List<IFacetBuilderHelper>>();
    private Map<String, Map<String, SourceFetchContext>> fetchSourceContextByClass = new HashMap<String, Map<String, SourceFetchContext>>();

    /**
     * Helper to return a valid index type from a class. Currently uses clazz.getSimpleName().toLowerCase();
     * Using the helper make sure this is done in a consistent way in different locations of the code.
     *
     * @param clazz The class for which to get an index type.
     * @return The index type.
     */
    public static String indexTypeFromClass(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    /**
     * Build mapping for the given packages.
     *
     * @param packages list of packages in which to look for {@link ESObject} annotated classes.
     * @throws IntrospectionException Java reflection usage related exception.
     * @throws IOException In case of an IO error while creating Json.
     * @throws JsonMappingException In case of an error while creating mapping json.
     * @throws JsonGenerationException In case of an error while creating mapping json.
     */
    public void initialize(String... packages) throws IntrospectionException, JsonGenerationException, JsonMappingException, IOException {
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
    public String getMapping(Class<?> clazz) throws JsonGenerationException, JsonMappingException, IntrospectionException, IOException {
        String classMapping = classesMappings.get(clazz.getName());
        if (classMapping == null) {
            parseClassAnnotations(clazz, "");
        }
        classMapping = classesMappings.get(clazz.getName());
        return classMapping;
    }

    public String getIndexSettings(Class<?> clazz) throws JsonGenerationException, JsonMappingException, IntrospectionException, IOException {
        String settings = settingsByClassName.get(clazz.getName());
        if (settings == null) {
            parseClassAnnotations(clazz, "");
        }
        settings = settingsByClassName.get(clazz.getName());
        return settings;
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
     * Get the list of es fields that should be filtered in a filter search for the given class.
     *
     * @param clazz The class for which to get the facets.
     * @return The list of filters builders for this class.
     */
    public List<IFilterBuilderHelper> getFilters(Class<?> clazz) {
        return this.filtersByClassName.get(clazz.getName());
    }

    /**
     * Get the list of es fields that should be filtered in a filter search for the given class.
     *
     * @param className The name for which to get the facets.
     * @return The list of filters builders for this class.
     */
    public List<IFilterBuilderHelper> getFilters(String className) {
        return this.filtersByClassName.get(className);
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

    /**
     * Get the {@link SourceFetchContext} for a given fetch context.
     *
     * @param className The class for which to get the fetch context.
     * @param fetchContext The fetch context for which to get the field list.
     * @return The requested {@link SourceFetchContext} or null if no context match the given class and fetch context key.
     */
    public SourceFetchContext getFetchSource(String className, String fetchContext) {
        Map<String, SourceFetchContext> fetchSourceByContext = this.fetchSourceContextByClass.get(className);
        if (fetchSourceByContext == null) {
            return null;
        }
        return fetchSourceByContext.get(fetchContext);
    }

    private void initialize(String packageName) throws IntrospectionException, JsonGenerationException, JsonMappingException, IOException {
        Set<Class<?>> classSet = org.elasticsearch.util.AnnotationScanner.scan(packageName, ESObject.class);
        for (Class<?> clazz : classSet) {
            parseClassAnnotations(clazz, "");
        }
    }

    public void parseClassAnnotations(Class<?> clazz, String pathPrefix)
            throws IntrospectionException, JsonGenerationException, JsonMappingException, IOException {
        ESObject esObject = AnnotationScanner.getAnnotation(ESObject.class, clazz);
        ESAll esAll = AnnotationScanner.getAnnotation(ESAll.class, clazz);

        String typeNameStr = null;
        if (!Modifier.isAbstract(clazz.getModifiers())) {
            TypeName typeName = clazz.getAnnotation(TypeName.class);
            if (typeName == null) {
                typeNameStr = MappingBuilder.indexTypeFromClass(clazz);
            } else {
                typeNameStr = typeName.typeName();
            }
        }

        Map<String, Object> typeDefinitionMap = new HashMap<String, Object>();
        Map<String, Object> classDefinitionMap = new HashMap<String, Object>();
        List<IFilterBuilderHelper> filteredFields = new ArrayList<IFilterBuilderHelper>();
        List<IFacetBuilderHelper> facetFields = new ArrayList<IFacetBuilderHelper>();
        Map<String, SourceFetchContext> fetchContexts = new HashMap<String, SourceFetchContext>();

        typeDefinitionMap.put(typeNameStr, classDefinitionMap);

/*********************
        if (esAll != null) {
            classDefinitionMap.put("_all",
                    MapUtil.getMap(new String[] { "enabled", "analyzer", "store" }, new Object[] { true, esAll.analyser(), esAll.store() }));
        } else {
            classDefinitionMap.put("_all", MapUtil.getMap("enabled", esObject.all()));
        }
        classDefinitionMap.put("_source", MapUtil.getMap("enabled", esObject.source()));
        //classDefinitionMap.put("_type", MapUtil.getMap(new String[] { "store", "index" }, new Object[] { esObject.store(), esObject.index() }));
****************************/

        this.fieldsMappingBuilder.parseFieldMappings(clazz, classDefinitionMap, facetFields, filteredFields, fetchContexts, pathPrefix, null, true);

        ObjectMapper mapper = new ObjectMapper();
        if (typeNameStr != null) {
            String jsonMapping = mapper.writeValueAsString(typeDefinitionMap);
            this.classesMappings.put(clazz.getName(), jsonMapping);
        }
        // abstract types are not registered but can be use for global queries over indexes.
        this.typeByClassName.put(clazz.getName(), typeNameStr);
        this.facetByClassName.put(clazz.getName(), facetFields);
        this.filtersByClassName.put(clazz.getName(), filteredFields);
        this.fetchSourceContextByClass.put(clazz.getName(), fetchContexts);

        this.settingsByClassName.put(clazz.getName(), buildSettings(mapper, esObject.analyzerDefinitions()));
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private static class AnalyserFields {
        public String tokenizer;
        public String type;
        public String[] char_filter;
        public String[] filter;
        public String[] stopwords;
    }

    private String buildSettings(ObjectMapper mapper, IndexAnalyserDefinition[] customAnalyserDefinitions) throws JsonProcessingException {
        if (ArrayUtils.isEmpty(customAnalyserDefinitions)) {
            return null;
        }

        Map<Object, Object> analysers = Maps.newHashMap();
        for (IndexAnalyserDefinition analyserDefinition : customAnalyserDefinitions) {
            AnalyserFields analyserFields = new AnalyserFields();
            analyserFields.char_filter = analyserDefinition.char_filter();
            analyserFields.filter = analyserDefinition.filters();
            analyserFields.stopwords = analyserDefinition.stopwords();
            analyserFields.tokenizer = analyserDefinition.tokenizer();
            analyserFields.type = analyserDefinition.type();
            analysers.put(analyserDefinition.name(), analyserFields);
        }

        return "{\"analysis\":{\"analyzer\":" + mapper.writeValueAsString(analysers) + "}}";
    }
}
