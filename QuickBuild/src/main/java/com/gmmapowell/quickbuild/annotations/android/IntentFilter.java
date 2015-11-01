package com.gmmapowell.quickbuild.annotations.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IntentFilter {
	public String action() default "";
	public String[] category() default "";
	public String data() default "";
}
