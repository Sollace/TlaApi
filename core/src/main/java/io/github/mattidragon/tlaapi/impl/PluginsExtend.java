package io.github.mattidragon.tlaapi.impl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark classes that plug-ins of the TLA API should extend and implementations should call methods on.
 * Has no real effect and only serves as a marker to clarify intended API usage.
 * @see ImplementationsExtend
 */
@Documented
@Target(ElementType.TYPE)
public @interface PluginsExtend {
}
