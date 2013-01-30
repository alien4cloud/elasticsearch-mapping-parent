package org.elasticsearch.mapping;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
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
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author luc boutier
 */
public class MappingBuilder {
	private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);
	private Map<String, String> classesMappings = new HashMap<String, String>();

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

	public String getMapping(Class<?> clazz) throws JsonGenerationException, JsonMappingException,
			IntrospectionException, IOException {
		String classMapping = classesMappings.get(clazz.getName());
		if (classMapping == null) {
			parseClassMapping(clazz, "");
		}
		classMapping = classesMappings.get(clazz.getName());
		return classMapping;
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

		if (Modifier.isAbstract(clazz.getModifiers())) {
			LOGGER.info("Class <" + clazz.getName() + "> is abstract and won't be processed.");
			return;
		}

		ESObject esObject = clazz.getAnnotation(ESObject.class);
		TypeName typeName = clazz.getAnnotation(TypeName.class);
		String typeNameStr;
		if (typeName == null) {
			typeNameStr = clazz.getSimpleName().toLowerCase();
		} else {
			typeNameStr = typeName.typeName();
		}

		Map<String, Object> typeDefinitionMap = new HashMap<String, Object>();
		Map<String, Object> classDefinitionMap = new HashMap<String, Object>();

		typeDefinitionMap.put(typeNameStr, classDefinitionMap);

		classDefinitionMap.put("_all", getMap("enabled", esObject.all()));
		classDefinitionMap.put("_source", getMap("enabled", esObject.source()));
		classDefinitionMap.put("_type",
				getMap(new String[] { "store", "index" }, new Object[] { esObject.store(), esObject.index() }));

		parseFieldMappings(clazz, classDefinitionMap, pathPrefix);

		ObjectMapper mapper = new ObjectMapper();
		String jsonMapping = mapper.writeValueAsString(typeDefinitionMap);
		classesMappings.put(clazz.getName(), jsonMapping);
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

	private void parseFieldMappings(Class<?> clazz, Map<String, Object> classDefinitionMap, String pathPrefix)
			throws IntrospectionException {
		if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
			parseFieldMappings(clazz.getSuperclass(), classDefinitionMap, pathPrefix);
		}

		Field[] fields = clazz.getDeclaredFields();
		Map<String, PropertyDescriptor> pdMap = getFieldPropertyDescriptorMap(clazz);

		Map<String, Object> propertiesDefinitionMap = new HashMap<String, Object>();
		classDefinitionMap.put("properties", propertiesDefinitionMap);

		for (Field field : fields) {
			if (Modifier.isTransient(field.getModifiers())) {
				LOGGER.debug("Field <" + field.getName() + "> of class <" + clazz.getName()
						+ "> has no proper setter/getter and won't be persisted.");
				continue; // transient field are not mapped.
			}

			PropertyDescriptor propertyDescriptor = pdMap.get(field.getName());

			if (propertyDescriptor == null || propertyDescriptor.getReadMethod() == null
					|| propertyDescriptor.getWriteMethod() == null) {
				LOGGER.debug("Field <" + field.getName() + "> of class <" + clazz.getName()
						+ "> has no proper setter/getter and won't be persisted.");
				continue;
			}

			parseFieldMappings(clazz, classDefinitionMap, propertiesDefinitionMap, pathPrefix, field,
					propertyDescriptor);
		}
	}

	private void parseFieldMappings(Class<?> clazz, Map<String, Object> classDefinitionMap,
			Map<String, Object> propertiesDefinitionMap, String pathPrefix, Field field,
			PropertyDescriptor propertyDescriptor) {
		processIdAnnotation(classDefinitionMap, pathPrefix, field, propertyDescriptor);
		processRoutingAnnotation(classDefinitionMap, pathPrefix, field, propertyDescriptor);
		processBoostAnnotation(classDefinitionMap, pathPrefix, field, propertyDescriptor);

		// process the fields
		if (ClassUtils.isPrimitiveOrWrapper(field.getType()) || field.getType() == String.class) {
			processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, field, propertyDescriptor);
		} else {
			// mapping of a complex field
			if (field.getType().isArray()) {
				// process the array type.
				Class<?> arrayType = field.getType().getComponentType();
				if (ClassUtils.isPrimitiveOrWrapper(arrayType) || arrayType == String.class) {
					processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, field, propertyDescriptor);
				} else {
					if (arrayType.isEnum()) {
						// if this is an enum and there is a String
						StringField annotation = getAnnotation(field, propertyDescriptor, StringField.class);
						if (annotation != null) {
							processStringOrPrimitive(clazz, propertiesDefinitionMap, pathPrefix, field,
									propertyDescriptor);
						}
					} else {
						processComplexType(clazz, propertiesDefinitionMap, pathPrefix, field, propertyDescriptor);
					}
				}
			} else {
				// process the type
				processComplexType(clazz, propertiesDefinitionMap, pathPrefix, field, propertyDescriptor);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void processIdAnnotation(Map<String, Object> classDefinitionMap, String pathPrefix, Field field,
			PropertyDescriptor propertyDescriptor) {
		Id id = getAnnotation(field, propertyDescriptor, Id.class);
		if (id != null) {
			if (classDefinitionMap.containsKey("_id")) {
				LOGGER.warn("An Id annotation is defined on field <" + pathPrefix + field.getName() + "> of <"
						+ field.getDeclaringClass().getName() + "> but a routing has already be defined for <"
						+ ((Map<String, Object>) classDefinitionMap.get("_id")).get("path") + ">");
			} else {
				classDefinitionMap.put(
						"_id",
						getMap(new String[] { "path", "index", "store" }, new Object[] { pathPrefix + field.getName(),
								id.index(), id.store() }));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void processRoutingAnnotation(Map<String, Object> classDefinitionMap, String pathPrefix, Field field,
			PropertyDescriptor propertyDescriptor) {
		Routing routing = getAnnotation(field, propertyDescriptor, Routing.class);
		if (routing != null) {
			if (classDefinitionMap.containsKey("_routing")) {
				LOGGER.warn("A Routing annotation is defined on field <" + pathPrefix + field.getName() + "> of <"
						+ field.getDeclaringClass().getName() + "> but a routing has already be defined for <"
						+ ((Map<String, Object>) classDefinitionMap.get("_routing")).get("path") + ">");
			} else {
				Map<String, Object> routingDef = new HashMap<String, Object>();
				routingDef.put("path", pathPrefix + field.getName());
				routingDef.put("required", routing.required());
				classDefinitionMap.put("_routing", routingDef);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void processBoostAnnotation(Map<String, Object> classDefinitionMap, String pathPrefix, Field field,
			PropertyDescriptor propertyDescriptor) {
		Boost boost = getAnnotation(field, propertyDescriptor, Boost.class);
		if (boost != null) {
			if (classDefinitionMap.containsKey("_boost")) {
				LOGGER.warn("A Routing annotation is defined on field <" + pathPrefix + field.getName() + "> of <"
						+ field.getDeclaringClass().getName() + "> but a routing has already be defined for <"
						+ ((Map<String, Object>) classDefinitionMap.get("_boost")).get("name") + ">");
			} else {
				Map<String, Object> boostDef = new HashMap<String, Object>();
				boostDef.put("name", pathPrefix + field.getName());
				boostDef.put("null_value", boost.nullValue());
				classDefinitionMap.put("_boost", boostDef);
			}
		}
	}

	private void processStringOrPrimitive(Class<?> clazz, Map<String, Object> propertiesDefinitionMap,
			String pathPrefix, Field field, PropertyDescriptor propertyDescriptor) {
		processFieldAnnotation(IndexName.class, new IndexNameAnnotationParser(), propertiesDefinitionMap, pathPrefix,
				field, propertyDescriptor);
		processFieldAnnotation(NullValue.class, new NullValueAnnotationParser(), propertiesDefinitionMap, pathPrefix,
				field, propertyDescriptor);

		// String field annotations.
		processFieldAnnotation(StringField.class, new StringFieldAnnotationParser(), propertiesDefinitionMap,
				pathPrefix, field, propertyDescriptor);
		processFieldAnnotation(Analyser.class, new AnalyserAnnotationParser(), propertiesDefinitionMap, pathPrefix,
				field, propertyDescriptor);
		processFieldAnnotation(IndexAnalyser.class, new IndexAnalyserAnnotationParser(), propertiesDefinitionMap,
				pathPrefix, field, propertyDescriptor);
		processFieldAnnotation(SearchAnalyser.class, new SearchAnalyserAnnotationParser(), propertiesDefinitionMap,
				pathPrefix, field, propertyDescriptor);

		// Numeric field annotation
		processFieldAnnotation(NumberField.class, new NumberFieldAnnotationParser(), propertiesDefinitionMap,
				pathPrefix, field, propertyDescriptor);

		// Date field annotation
		processFieldAnnotation(DateField.class, new DateFieldAnnotationParser(), propertiesDefinitionMap, pathPrefix,
				field, propertyDescriptor);
		processFieldAnnotation(DateFormat.class, new DateFormatAnnotationParser(), propertiesDefinitionMap, pathPrefix,
				field, propertyDescriptor);

		// Boolean field annotation
		processFieldAnnotation(BooleanField.class, new BooleanFieldAnnotationParser(), propertiesDefinitionMap,
				pathPrefix, field, propertyDescriptor);
		// TODO binary type mapping
	}

	private void processComplexType(Class<?> clazz, Map<String, Object> propertiesDefinitionMap, String pathPrefix,
			Field field, PropertyDescriptor propertyDescriptor) {
		// TODO check annotations

	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> void processFieldAnnotation(Class<T> annotationClass,
			IPropertyAnnotationParser<T> propertyAnnotationParser, Map<String, Object> propertiesDefinitionMap,
			String pathPrefix, Field field, PropertyDescriptor propertyDescriptor) {
		T annotation = getAnnotation(field, propertyDescriptor, annotationClass);
		if (annotation != null) {
			Map<String, Object> fieldDefinition = (Map<String, Object>) propertiesDefinitionMap.get(field.getName());
			if (fieldDefinition == null) {
				fieldDefinition = new HashMap<String, Object>();
				propertiesDefinitionMap.put(field.getName(), fieldDefinition);
			}
			propertyAnnotationParser
					.parseAnnotation(annotation, fieldDefinition, pathPrefix, field, propertyDescriptor);
		}
	}

	private <T extends Annotation> T getAnnotation(Field field, PropertyDescriptor propertyDescriptor,
			Class<T> annotationClass) {
		T annotation = field.getAnnotation(annotationClass);
		if (annotation == null) {
			annotation = propertyDescriptor.getReadMethod().getAnnotation(annotationClass);
		}
		return annotation;
	}

	/**
	 * Get a map of property descriptor for the given class.
	 * 
	 * @param clazz The class for which to get the map of property descriptors.
	 * @return A map of {@link PropertyDescriptor} by name ({@link PropertyDescriptor#getName()}).
	 * @throws IntrospectionException In case we cannot use reflection to get the {@link PropertyDescriptor}.
	 */
	private Map<String, PropertyDescriptor> getFieldPropertyDescriptorMap(Class<?> clazz) throws IntrospectionException {
		Map<String, PropertyDescriptor> pdMap = new HashMap<String, PropertyDescriptor>();

		PropertyDescriptor[] pdArr = Introspector.getBeanInfo(clazz, clazz.getSuperclass()).getPropertyDescriptors();

		if (pdArr == null || pdArr.length == 0) {
			return pdMap;
		}

		for (PropertyDescriptor pd : pdArr) {
			pdMap.put(pd.getName(), pd);
		}

		return pdMap;
	}
}