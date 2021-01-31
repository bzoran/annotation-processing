package com.nimbussoft.hrmis.annotation.controller;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Set;

@SupportedAnnotationTypes({
		"com.nimbussoft.hrmis.annotation.controller.HrmisRestController",
		"com.nimbussoft.hrmis.annotation.controller.HrmisPrincipalRestController"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class RestControllerProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (roundEnv.processingOver()) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, ">>> Processing finished!");
			return false;
		}
		try {
			for (TypeElement annotation : annotations) {
				for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
					if (annotation.toString().endsWith("HrmisRestController")) {
						writeRestControllerFile(element.toString(), element.getAnnotation(HrmisRestController.class).entityName());
					} else if (annotation.toString().endsWith("HrmisPrincipalRestController")) {
						writePrincipalRestControllerFile(element.toString(), element.getAnnotation(HrmisPrincipalRestController.class).entityName(),
								element.getAnnotation(HrmisPrincipalRestController.class).addInstitutionTo());
					}
				}
			}
		} catch (AnnotationProcessorException e) {
			System.err.println(e.toString());
		}
		return false;
	}

	private void writeRestControllerFile(String className, String entityName) throws AnnotationProcessorException {
		String packageName = calcPackageName(className);
		String rootPackageName = calcRootPackageName(packageName);

		HashMap<String, Object> scopes = new HashMap<>();
		scopes.put("packageName", packageName);
		scopes.put("rootPackageName", rootPackageName);
		scopes.put("entityName", entityName);
		scopes.put("entityVariable", entityName.substring(0, 1).toLowerCase() + entityName.substring(1));
		generateFile(className + "Abstract", scopes,
				"com/nimbussoft/hrmis/annotation/controller/rest-crud-controller.mustache");
	}

	private void writePrincipalRestControllerFile(String className, String entityName, String addInstitutionTo) throws AnnotationProcessorException {
		String packageName = calcPackageName(className);
		String rootPackageName = calcRootPackageName(packageName);

		HashMap<String, Object> scopes = new HashMap<>();
		scopes.put("packageName", packageName);
		scopes.put("rootPackageName", rootPackageName);
		scopes.put("entityName", entityName);
		scopes.put("addInstitutionTo", addInstitutionTo);
		scopes.put("entityVariable", entityName.substring(0, 1).toLowerCase() + entityName.substring(1));
		generateFile(className + "Abstract", scopes,
				"com/nimbussoft/hrmis/annotation/controller/rest-principal-crud-controller.mustache");
	}

	private String calcRootPackageName(String packageName) throws AnnotationProcessorException {
		int lastDot;
		lastDot = packageName.lastIndexOf('.');
		if (lastDot <= 0) {
			throw new AnnotationProcessorException("No parent package name");
		}
		return packageName.substring(0, lastDot);
	}

	private String calcPackageName(String className) throws AnnotationProcessorException {
		int lastDot = className.lastIndexOf('.');
		if (lastDot <= 0) {
			throw new AnnotationProcessorException("No package name");
		}
		return className.substring(0, lastDot);
	}

	private void generateFile(String abstractJavaName, HashMap<String, Object> scopes, String s) throws AnnotationProcessorException {
		scopes.put("now", LocalDateTime.now());
		try {
			JavaFileObject javaFile = processingEnv.getFiler().createSourceFile(abstractJavaName);
			MustacheFactory mf = new DefaultMustacheFactory();
			try (PrintWriter out = new PrintWriter(javaFile.openWriter())) {
				mf.compile(s)
						.execute(out, scopes);
			}
		} catch (IOException e) {
			throw new AnnotationProcessorException(e);
		}
	}

}

