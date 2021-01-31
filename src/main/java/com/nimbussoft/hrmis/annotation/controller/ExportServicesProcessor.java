package com.nimbussoft.hrmis.annotation.controller;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
		"com.nimbussoft.hrmis.annotation.controller.HrmisExportService",
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ExportServicesProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (roundEnv.processingOver()) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, ">>> Processing finished!");
			return false;
		}
		try {
			final List<String> allowedTypes = Lists.newArrayList("excel", "pdf", "word");
			for (TypeElement annotation : annotations) {
				for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
					HrmisExportService singleAnnotation = element.getAnnotation(HrmisExportService.class);
					List<String> fields = element.getEnclosedElements().stream().map(Element::getSimpleName)
							.map(Name::toString).map(String::toLowerCase).collect(Collectors.toList());
					for (String field : fields) {
						if (allowedTypes.contains(field)) {
							writeExportFile(element.toString(), singleAnnotation.entityName(), field);
						}
					}
				}
			}
		} catch (AnnotationProcessorException e) {
			System.err.println(e.toString());
		}
		return false;
	}

	private void writeExportFile(String className, String entityName, String fileType) throws AnnotationProcessorException {
		String packageName = calcPackageName(className);
		String rootPackageName = calcRootPackageName(packageName);

		HashMap<String, Object> scopes = new HashMap<>();
		scopes.put("packageName", packageName);
		scopes.put("rootPackageName", rootPackageName);
		scopes.put("entityName", entityName);
		scopes.put("entityVariable", entityName.substring(0, 1).toLowerCase() + entityName.substring(1));
		generateFile(transformClassName(className, fileType), scopes,
				String.format("com/nimbussoft/hrmis/annotation/controller/%s-export-service.mustache", fileType));
	}

	private String transformClassName(String className, String fileType) {
		int idxOfSplit = className.indexOf("Service");
		return className.substring(0, idxOfSplit) + capitalize(fileType) + className.substring(idxOfSplit);
	}

	public String capitalize(String val) {
		return val.substring(0, 1).toUpperCase() + val.substring(1);
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
		className = className.substring(0, lastDot);
		lastDot = className.lastIndexOf('.');
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
