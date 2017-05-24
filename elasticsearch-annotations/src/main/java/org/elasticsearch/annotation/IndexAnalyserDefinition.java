package org.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface IndexAnalyserDefinition {
    String name();

    String tokenizer() default "";

    String[] filters() default {};

    String type() default "";

    String[] stopwords() default {};

    String[] char_filter() default {};
}
