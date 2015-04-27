package org.elasticsearch.mapping;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public class Indexable {

    private Field field;

    private PropertyDescriptor propertyDescriptor;

    public Indexable(Field field, PropertyDescriptor propertyDescriptor) {
        checkValidity(field, propertyDescriptor);
        this.field = field;
        this.propertyDescriptor = propertyDescriptor;
    }

    private void checkValidity(Field field, PropertyDescriptor propertyDescriptor) {
        if (field == null && propertyDescriptor == null) {
            throw new MappingException("Both field and property descriptor cannot be null");
        }
        if (propertyDescriptor != null && (propertyDescriptor.getReadMethod() == null || propertyDescriptor.getWriteMethod() == null)) {
            throw new MappingException("Property descriptor [" + propertyDescriptor + "] must contain valid getter and setter method");
        }
        if (field != null && propertyDescriptor != null && !field.getType().equals(propertyDescriptor.getPropertyType())) {
            throw new MappingException("Property descriptor's type [" + propertyDescriptor + "] must be the same as field's type [" + field.getType() + "]");
        }
    }

    public String getDeclaringClassName() {
        if (field != null) {
            return field.getDeclaringClass().getName();
        }
        if (propertyDescriptor != null) {
            return propertyDescriptor.getReadMethod().getDeclaringClass().getName();
        }
        return null;
    }

    public String getName() {
        return propertyDescriptor.getName();
    }

    public Class<?> getType() {
        if (field != null) {
            return field.getType();
        } else {
            return propertyDescriptor.getPropertyType();
        }
    }

    public Class<?> getComponentType() {
        Class<?> type = getType();
        if (type.isArray()) {
            return type.getComponentType();
        } else if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType pt = null;
            if (field != null && field.getGenericType() instanceof ParameterizedType) {
                pt = (ParameterizedType) field.getGenericType();
            } else if (propertyDescriptor != null && propertyDescriptor.getReadMethod().getGenericReturnType() instanceof ParameterizedType) {
                pt = (ParameterizedType) propertyDescriptor.getReadMethod().getGenericReturnType();
            } else {
                return null;
            }
            Type[] types = pt.getActualTypeArguments();
            if (types.length > 0) {
                Class<?> valueClass = (Class<?>) types[0];
                return valueClass;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (field != null) {
            T annotation = field.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        if (propertyDescriptor != null && propertyDescriptor.getReadMethod() != null) {
            return propertyDescriptor.getReadMethod().getAnnotation(annotationClass);
        }
        return null;
    }
}
