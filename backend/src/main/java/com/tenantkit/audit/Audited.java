package com.tenantkit.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method as audited. When the method returns normally, the
 * aspect writes one row to the tenant's audit_logs table. Failures are
 * never logged as actions (nothing happened), which keeps the trail honest.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** Machine-readable action name, e.g. "USER_CREATED". */
    String action();

    /** Which entity the action touches, e.g. "app_user". */
    String entity() default "";
}
