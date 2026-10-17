package io.github.mattidragon.tlaapi.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation applied to methods and fields to indicate which review viewer supports a particular feature or parameter.
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface SupportsOnly {
    String[] value();
}
