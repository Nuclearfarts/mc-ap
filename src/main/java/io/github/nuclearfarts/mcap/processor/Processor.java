package io.github.nuclearfarts.mcap.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import io.github.nuclearfarts.mcap.annotation.BlockRegistryCallback;
import io.github.nuclearfarts.mcap.annotation.FieldRef;
import io.github.nuclearfarts.mcap.annotation.ItemRegistryCallback;
import io.github.nuclearfarts.mcap.annotation.RegisterBlock;
import io.github.nuclearfarts.mcap.annotation.RegisterItem;
import io.github.nuclearfarts.mcap.annotation.RegistryContainer;

@SupportedOptions("buildDir")
public class Processor extends AbstractProcessor {
	private static final Set<String> ANNOTATIONS = new HashSet<>();
	
	private Messager msg;
	private Filer filer;
	private Elements elements;
	private Types types;
	
	private TypeMirror itemMirror;
	private TypeMirror blockMirror;
	private TypeMirror identifierMirror;
	private TypeMirror voidMirror;
	
	private TypeName itemName;
	private TypeName blockName;
	private TypeName blockItemName;
	private TypeName identifierName;
	private TypeName registryName;
	private TypeName itemGroupName;
	
	private Path projectDir;
	
	public void init(ProcessingEnvironment env) {
		projectDir = Paths.get(env.getOptions().get("buildDir"));
		msg = env.getMessager();
		filer = env.getFiler();
		elements = env.getElementUtils();
		types = env.getTypeUtils();
		itemMirror = elements.getTypeElement("net.minecraft.item.Item").asType();
		blockMirror = elements.getTypeElement("net.minecraft.block.Block").asType();
		identifierMirror = elements.getTypeElement("net.minecraft.util.Identifier").asType();
		voidMirror = elements.getTypeElement("java.lang.Void").asType();
		itemName = TypeName.get(itemMirror);
		blockName = TypeName.get(blockMirror);
		blockItemName = TypeName.get(elements.getTypeElement("net.minecraft.item.BlockItem").asType());
		identifierName = TypeName.get(identifierMirror);
		registryName = TypeName.get(types.erasure(elements.getTypeElement("net.minecraft.util.registry.Registry").asType()));
		itemGroupName = TypeName.get(elements.getTypeElement("net.minecraft.item.ItemGroup").asType());
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for(Element clazz : roundEnv.getElementsAnnotatedWith(RegistryContainer.class)) {
			TypeElement typeElement = (TypeElement) clazz;
			RegistryContainer rc = typeElement.getAnnotation(RegistryContainer.class);
			ExecutableElement blockRegisterCallback = null;
			ExecutableElement itemRegisterCallback = null;
			List<ParsedBlock> blocks = new ArrayList<>();
			List<ParsedItem> items = new ArrayList<>();
			ParsedRegistryContainer parsedContainer = new ParsedRegistryContainer(rc, new TemplateLoader(projectDir.resolve("templates")), TypeName.get(typeElement.asType()), getErrorConsumer(typeElement, RegistryContainer.class), this::parseFieldRef);
			for(Element ele : typeElement.getEnclosedElements()) {
				if(ele.getKind() == ElementKind.METHOD) {
					if(checkForCallbackAnnotation((ExecutableElement) ele, BlockRegistryCallback.class, "block", identifierMirror, blockMirror)) {
						if(blockRegisterCallback != null) {
							msg.printMessage(Diagnostic.Kind.ERROR, "Cannot have multiple block registry callbacks", ele, thisApiSucks(ele.getAnnotationMirrors(), BlockRegistryCallback.class));
							msg.printMessage(Diagnostic.Kind.ERROR, "Cannot have multiple block registry callbacks", ele, thisApiSucks(blockRegisterCallback.getAnnotationMirrors(), BlockRegistryCallback.class));
						} else {
							blockRegisterCallback = (ExecutableElement) ele;
						}
					}
					if(checkForCallbackAnnotation((ExecutableElement) ele, ItemRegistryCallback.class, "item", identifierMirror, itemMirror)) {
						if(itemRegisterCallback != null) {
							msg.printMessage(Diagnostic.Kind.ERROR, "Cannot have multiple item registry callbacks", ele, thisApiSucks(ele.getAnnotationMirrors(), ItemRegistryCallback.class));
							msg.printMessage(Diagnostic.Kind.ERROR, "Cannot have multiple item registry callbacks", ele, thisApiSucks(itemRegisterCallback.getAnnotationMirrors(), ItemRegistryCallback.class));
						} else {
							itemRegisterCallback = (ExecutableElement) ele;
						}
					}
				} else if(ele.getKind() == ElementKind.FIELD) {
					VariableElement varEle = (VariableElement) ele;
					if(checkForRegisterAnnotation(varEle, RegisterBlock.class, blockMirror)) {
						blocks.add(new ParsedBlock(varEle, parsedContainer, getErrorConsumer(varEle, RegisterBlock.class)));
					}
					if(checkForRegisterAnnotation(varEle, RegisterItem.class, itemMirror)) {
						items.add(new ParsedItem(varEle, parsedContainer, getErrorConsumer(varEle, RegisterItem.class)));
					}
				}
			}
			
			genRegistrar(typeElement, blockRegisterCallback, itemRegisterCallback, blocks, items, parsedContainer);
			genResources(blocks, items);
		}
		return false;
	}
	
	private Consumer<String> getErrorConsumer(Element element, Class<?> annotation) {
		return s -> msg.printMessage(Diagnostic.Kind.ERROR, s, element, thisApiSucks(element.getAnnotationMirrors(), annotation));
	}
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return ANNOTATIONS;
	}
	
	private void genRegistrar(TypeElement ownerClass, ExecutableElement blockCallback, ExecutableElement itemCallback, List<ParsedBlock> blocks, List<ParsedItem> items, ParsedRegistryContainer rc) {
		TypeName ownerClassName = TypeName.get(ownerClass.asType());
		CodeBlock.Builder regBuilder = CodeBlock.builder();
		for(ParsedBlock block : blocks) {
			block.appendTo(regBuilder);
		}
		
		for(ParsedItem item : items) {
			item.appendTo(regBuilder);
		}
		
		CodeBlock.Builder itemRegBuilder = CodeBlock.builder();
		if(itemCallback != null) {
			itemRegBuilder.addStatement("$1T.$2L(new $3T($4S, id), item)", ownerClassName, itemCallback.getSimpleName(), identifierName, rc.getModId());
		} else {
			itemRegBuilder.addStatement("$1T.register($1T.ITEM, new $2T($3S, id), item)", registryName, identifierName, rc.getModId());
		}
		
		CodeBlock.Builder blockRegBuilder = CodeBlock.builder();
		if(blockCallback != null) {
			blockRegBuilder.addStatement("$1T.$2L(new $3T($4S, id), block)", ownerClassName, blockCallback.getSimpleName(), identifierName, rc.getModId());
		} else {
			blockRegBuilder.addStatement("$1T.register($1T.BLOCK, new $2T($3S, id), block)", registryName, identifierName, rc.getModId());
		}
		
		CodeBlock.Builder createBI1 = CodeBlock.builder();
		createBI1.addStatement("return new $1T(block, new $2T.Settings())", blockItemName, itemName);
		
		CodeBlock.Builder createBI2 = CodeBlock.builder();
		createBI2.addStatement("return new $1T(block, new $2T.Settings().group(group))", blockItemName, itemName);
		
		TypeSpec registrar = TypeSpec.classBuilder(ownerClass.getSimpleName() + "Registrar")
				.addModifiers(Modifier.PUBLIC)
				.addMethod(MethodSpec.methodBuilder("register").returns(TypeName.VOID).addModifiers(Modifier.PUBLIC, Modifier.STATIC).addCode(regBuilder.build()).build())
				.addMethod(MethodSpec.methodBuilder("registerItem").returns(TypeName.VOID).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
						.addParameter(itemName, "item")
						.addParameter(String.class, "id")
						.addCode(itemRegBuilder.build()).build())
				.addMethod(MethodSpec.methodBuilder("registerBlock").returns(TypeName.VOID).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
						.addParameter(blockName, "block")
						.addParameter(String.class, "id")
						.addCode(blockRegBuilder.build()).build())
				.addMethod(MethodSpec.methodBuilder("createBlockItem").returns(itemName).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
						.addParameter(blockName, "block")
						.addCode(createBI1.build()).build())
				.addMethod(MethodSpec.methodBuilder("createBlockItem").returns(itemName).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
						.addParameter(blockName, "block")
						.addParameter(itemGroupName, "group")
						.addCode(createBI2.build()).build())
				.build();
		
		JavaFile javaFile = JavaFile.builder(elements.getPackageOf(ownerClass).getQualifiedName().toString(), registrar)
				.indent("\t")
				.build();
		try {
			JavaFileObject jfo = filer.createSourceFile(elements.getPackageOf(ownerClass).getQualifiedName().toString() + "." + ownerClass.getSimpleName() + "Registrar", ownerClass);
			try(InputStream in = javaFile.toJavaFileObject().openInputStream()) {
				try(OutputStream out = jfo.openOutputStream()) {
					byte[] buffer = new byte[1024];
					int len = in.read(buffer);
					while (len != -1) {
			    		out.write(buffer, 0, len);
			    		len = in.read(buffer);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			msg.printMessage(Diagnostic.Kind.ERROR, e.toString(), ownerClass);
		}
	}
	
	private void genResources(List<ParsedBlock> blocks, List<ParsedItem> items) {
		for(ParsedBlock b : blocks) {
			b.genResources(this::resourceFromTemplate);
		}
		
		for(ParsedItem i : items) {
			i.genResources(this::resourceFromTemplate);
		}
	}
	
	private void resourceFromTemplate(String pkg, String fileName, String contents) {
		try(Writer w = filer.createResource(StandardLocation.CLASS_OUTPUT, pkg, fileName).openWriter()) {
			w.write(contents);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean checkForRegisterAnnotation(VariableElement ele, Class<? extends Annotation> annotation, TypeMirror requiredType) {
		if(ele.getAnnotation(annotation) != null) {
			if(types.isAssignable(ele.asType(), requiredType)) {
				if(ele.getModifiers().contains(Modifier.STATIC)) {
					if(!ele.getModifiers().contains(Modifier.PRIVATE)) {
						return true;
					} else {
						msg.printMessage(Diagnostic.Kind.ERROR, "Register annotation requires at least package access", ele, thisApiSucks(ele.getAnnotationMirrors(), annotation));
					}
				} else {
					msg.printMessage(Diagnostic.Kind.ERROR, "Register annotation requires field to be static", ele, thisApiSucks(ele.getAnnotationMirrors(), annotation));
				}
			} else {
				msg.printMessage(Diagnostic.Kind.ERROR, String.format("%s annotation requires a field of type %s", annotation.getSimpleName(), requiredType.toString()), ele, thisApiSucks(ele.getAnnotationMirrors(), annotation));
			}
		}
		return false;
	}
	
	private boolean checkForCallbackAnnotation(ExecutableElement ele, Class<? extends Annotation> annotation, String name, TypeMirror... paramsRequired) {
		if(ele.getAnnotation(annotation) != null) {
			if(checkParams(ele, paramsRequired)) {
				if(ele.getModifiers().contains(Modifier.STATIC)) {
					if(!ele.getModifiers().contains(Modifier.PRIVATE)) {
						return true;
					} else {
						msg.printMessage(Diagnostic.Kind.ERROR, "Callback annotation requires at least package access", ele, thisApiSucks(ele.getAnnotationMirrors(), annotation));
					}
				} else {
					msg.printMessage(Diagnostic.Kind.ERROR, "Callback annotation requires method to be static", ele, thisApiSucks(ele.getAnnotationMirrors(), annotation));
				}
			} else {
				msg.printMessage(Diagnostic.Kind.ERROR, String.format("%s registry callback must have parameters %s", name, Arrays.toString(paramsRequired)), ele);
				return false;
			}
		}
		return false;
	}
	
	private boolean checkParams(ExecutableElement element, TypeMirror... desired) {
		List<? extends VariableElement> params = element.getParameters();
		if(params.size() != desired.length) {
			return false;
		}
		for(int i = 0; i < desired.length; i++) {
			if(!types.isSameType(params.get(i).asType(), desired[i])) {
				return false;
			}
		}
		return true;
	}
	
	private AnnotationMirror thisApiSucks(List<? extends AnnotationMirror> why, Class<?> clazz) {
		TypeMirror bad = elements.getTypeElement(clazz.getCanonicalName()).asType();
		for(AnnotationMirror m : why) {
			if(types.isSameType(bad, m.getAnnotationType())) {
				return m;
			}
		}
		return null;
	}
	
	private ParsedFieldRef parseFieldRef(FieldRef fieldRef) {
		TypeMirror t = thisApiSucksMore(fieldRef);
		if(types.isSameType(t, voidMirror) && !fieldRef.field().equals("#$%INHERIT")) {
			return null;
		} else {
			return new ParsedFieldRef(t, fieldRef.field());
		}
	}
	
	private TypeMirror thisApiSucksMore(FieldRef fieldRef) {
		TypeMirror mirror;
		try {
			mirror = elements.getTypeElement(fieldRef.clazz().getCanonicalName()).asType();
		} catch(MirroredTypeException e) {
			mirror = e.getTypeMirror();
		}
		return mirror;
	}
	
	static {
		ANNOTATIONS.add(RegistryContainer.class.getCanonicalName());
	}
}