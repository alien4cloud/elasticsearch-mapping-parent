package org.elasticsearch.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Utility to scan a package for classes with a given annotation.
 * 
 * @author Luc Boutier
 */
public final class AnnotationScanner {
	private static final ESLogger LOGGER = Loggers.getLogger(MappingBuilder.class);

	/** Utility classes should have private constructor. */
	private AnnotationScanner() {}

	/**
	 * Scan a package to find classes that have the given annotation.
	 * 
	 * @param packageRoot The package to scan.
	 * @param anno Annotation that should be on the class that we are interested in.
	 * @return A set of classes that have the annotation.
	 */
	public static Set<Class<?>> scan(String packageRoot, Class<? extends Annotation> anno) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		AnnotationTypeFilter filter = new AnnotationTypeFilter(anno);
		scanner.addIncludeFilter(filter);
		Set<BeanDefinition> beanSet = scanner.findCandidateComponents(packageRoot);

		Set<Class<?>> classSet = new HashSet<Class<?>>();
		for (BeanDefinition beanDef : beanSet) {
			LOGGER.debug("found candidate bean = " + beanDef.getBeanClassName());

			Class<?> clazz;
			try {
				clazz = Class.forName(beanDef.getBeanClassName(), true, Thread.currentThread().getContextClassLoader());
				if (clazz.isAnnotationPresent(anno)) {
					LOGGER.debug("found annotated class, " + clazz.getName());
					classSet.add(clazz);
				}
			} catch (ClassNotFoundException e) {
				LOGGER.error("exception while scanning classpath for annotated classes", e);
			}
		}

		return classSet;
	}
}