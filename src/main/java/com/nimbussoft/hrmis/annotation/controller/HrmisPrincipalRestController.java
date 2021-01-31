package com.nimbussoft.hrmis.annotation.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface HrmisPrincipalRestController {
	String entityName() default "Invalid entity name";

	String addInstitutionTo() default "Invalid property name";
}
