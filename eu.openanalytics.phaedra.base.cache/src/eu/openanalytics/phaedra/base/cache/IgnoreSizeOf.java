package eu.openanalytics.phaedra.base.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to ignore a field, type or entire package while doing a SizeOf measurement
 * @see net.sf.ehcache.pool.sizeof.SizeOf
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE, ElementType.PACKAGE })
public @interface IgnoreSizeOf {

    boolean inherited() default false;
}
