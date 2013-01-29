package org.elasticsearch.mapping.parser;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;

import org.elasticsearch.annotation.NumberField;
import org.elasticsearch.mapping.MappingException;

/**
 * Parse a {@link NumberField} annotation.
 * 
 * @author luc boutier
 */
public class NumberFieldAnnotationParser implements IPropertyAnnotationParser<NumberField> {
	public void parseAnnotation(NumberField annotation, Map<String, Object> fieldDefinition, String pathPrefix,
			Field field, PropertyDescriptor propertyDescriptor) {
		if (fieldDefinition.get("type") != null) {
			throw new MappingException(
					"A field cannot have more than one Elastic Search type. Parsing StringField on <"
							+ field.getDeclaringClass().getName() + "." + field.getName()
							+ "> type is already set to <" + fieldDefinition.get("type") + ">");
		}

		String type = getESNumberType(field);
		if (type == null) {
			String error = "Field <"
					+ field.getName()
					+ "> has a Number annotation but type is <"
					+ field.getType().getName()
					+ "> and should be one of Float, float, Double, double, Integer, int, Long, long, Short, short, Byte or byte and therefore is not supported.";
			throw new MappingException(error);
		}

		fieldDefinition.put("type", type);
		fieldDefinition.put("store", annotation.store());
		fieldDefinition.put("index", annotation.index());
		fieldDefinition.put("precision_step", annotation.precisionStep());
		fieldDefinition.put("boost", annotation.boost());
		fieldDefinition.put("include_in_all", annotation.includeInAll());
		fieldDefinition.put("ignore_malformed", annotation.ignoreMalformed());
	}

	private String getESNumberType(Field field) {
		if (Float.TYPE.equals(field.getType()) || Float.class.equals(field.getType())) {
			return "float";
		} else if (Double.TYPE.equals(field.getType()) || Double.class.equals(field.getType())) {
			return "double";
		} else if (Integer.TYPE.equals(field.getType()) || Integer.class.equals(field.getType())) {
			return "integer";
		} else if (Long.TYPE.equals(field.getType()) || Long.class.equals(field.getType())) {
			return "long";
		} else if (Short.TYPE.equals(field.getType()) || Short.class.equals(field.getType())) {
			return "short";
		} else if (Byte.TYPE.equals(field.getType()) || Byte.class.equals(field.getType())) {
			return "byte";
		}
		return null;
	}
}