package com.fy.framework.annotation;

import java.lang.annotation.*;


@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FYRequestMapping {  String value() default "";
}
