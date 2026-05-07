package com.thanglong.landtax.infrastructure.config.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation dùng để đánh dấu các method cần ghi log thao tác nhạy cảm.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    String action() default "UNKNOWN_ACTION";
}
