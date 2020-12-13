package io.github.nuclearfarts.mcap.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
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

import io.github.nuclearfarts.mcap.BlockItemAction;
import io.github.nuclearfarts.mcap.BlockModelAction;
import io.github.nuclearfarts.mcap.ItemModelAction;
import io.github.nuclearfarts.mcap.LootTableAction;
import io.github.nuclearfarts.mcap.annotation.BlockRegistryCallback;
import io.github.nuclearfarts.mcap.annotation.FieldRef;
import io.github.nuclearfarts.mcap.annotation.ItemRegistryCallback;
import io.github.nuclearfarts.mcap.annotation.PostRegisterCallback;
import io.github.nuclearfarts.mcap.annotation.RegisterBlock;
import io.github.nuclearfarts.mcap.annotation.RegisterItem;
import io.github.nuclearfarts.mcap.annotation.RegistryContainer;

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
	private TypeName modInitializerName;
	
	public void init(ProcessingEnvironment env) {
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
		modInitializerName = TypeName.get(elements.getTypeElement("net.fabricmc.api.ModInitializer").asType());
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for(Element clazz : roundEnv.getElementsAnnotatedWith(RegistryContainer.class)) {
			TypeElement typeElement = (TypeElement) clazz;
			RegistryContainer rc = typeElement.getAnnotation(RegistryContainer.class);
			ExecutableElement blockRegisterCallback = null;
			ExecutableElement itemRegisterCallback = null;
			ExecutableElement postRegisterCallback = null;
			List<VariableElement> blocks = new ArrayList<>();
			List<VariableElement> items = new ArrayList<>();
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
					if(checkForCallbackAnnotation((ExecutableElement) ele, PostRegisterCallback.class, "post-register")) {
						if(postRegisterCallback != null) {
							msg.printMessage(Diagnostic.Kind.ERROR, "Cannot have multiple post-register callbacks", ele, thisApiSucks(ele.getAnnotationMirrors(), ItemRegistryCallback.class));
							msg.printMessage(Diagnostic.Kind.ERROR, "Cannot have multiple post-register callbacks", ele, thisApiSucks(postRegisterCallback.getAnnotationMirrors(), PostRegisterCallback.class));
						} else {
							postRegisterCallback = (ExecutableElement) ele;
						}
					}
				} else if(ele.getKind() == ElementKind.FIELD) {
					VariableElement varEle = (VariableElement) ele;
					if(checkForRegisterAnnotation(varEle, RegisterBlock.class, blockMirror)) {
						blocks.add(varEle);
					}
					if(checkForRegisterAnnotation(varEle, RegisterItem.class, itemMirror)) {
						items.add(varEle);
					}
				}
			}
			genRegistrar(typeElement, blockRegisterCallback, itemRegisterCallback, postRegisterCallback, blocks, items, rc);
			genResources(rc, blocks.stream().map(e -> e.getAnnotation(RegisterBlock.class)).collect(Collectors.toList()), items.stream().map(e -> e.getAnnotation(RegisterItem.class)).collect(Collectors.toList()));
		}
		return false;
	}
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return ANNOTATIONS;
	}
	
	private void genRegistrar(TypeElement ownerClass, ExecutableElement blockCallback, ExecutableElement itemCallback, ExecutableElement postCallback, List<VariableElement> blocks, List<VariableElement> items, RegistryContainer rc) {
		TypeName ownerClassName = TypeName.get(ownerClass.asType());
		CodeBlock.Builder regBuilder = CodeBlock.builder();
		for(VariableElement ele : blocks) {
			RegisterBlock regBlock = ele.getAnnotation(RegisterBlock.class);
			BlockItemAction blockItem = regBlock.blockItem();
			if(blockItem == BlockItemAction.INHERIT_MOD) {
				blockItem = rc.blockItem();
			}
			
			if(blockCallback != null) {
				regBuilder.addStatement("$1T.$2L(new $3T($4S, $5S), $1T.$6L)", ownerClassName, blockCallback.getSimpleName(), identifierName, rc.value(), regBlock.value(), ele.getSimpleName());
			} else {
				regBuilder.addStatement("$1T.register($1T.BLOCK, new $2T($3S, $4S), $5T.$6L)", registryName, identifierName, rc.value(), regBlock.value(), ownerClassName, ele.getSimpleName());
			}
			
			if(blockItem.registersItem) {
				FieldRef itemGroup = regBlock.itemGroup();
				if(types.isSameType(thisApiSucksMore(itemGroup), voidMirror)) {
					itemGroup = rc.itemGroup();
				}
				if(!types.isSameType(thisApiSucksMore(itemGroup), voidMirror)) {
					if(itemCallback != null) {
						regBuilder.addStatement("$1T.$2L(new $3T($4S, $5S), new $9T($1T.$10L, new $6T.Settings().group($7T.$8L)))", ownerClassName, itemCallback.getSimpleName(), identifierName, rc.value(), regBlock.value(), itemName, TypeName.get(thisApiSucksMore(itemGroup)), itemGroup.field(), blockItemName, ele.getSimpleName());
					} else {
						regBuilder.addStatement("$1T.register($1T.ITEM, new $2T($3S, $4S), new $5T($9T.$10L, new $6T.Settings().group($7T.$8L)))", registryName, identifierName, rc.value(), regBlock.value(), blockItemName, itemName, TypeName.get(thisApiSucksMore(itemGroup)), itemGroup.field(), ownerClassName, ele.getSimpleName());
					}
				} else {
					if(itemCallback != null) {
						regBuilder.addStatement("$1T.$2L(new $3T($4S, $5S), $1T.$6L)", ownerClassName, itemCallback.getSimpleName(), identifierName, rc.value(), regBlock.value(), ele.getSimpleName());
					} else {
						regBuilder.addStatement("$1T.register($1T.BLOCK, new $2T($3S, $4S), $5T.$6L)", registryName, identifierName, rc.value(), regBlock.value(), ownerClassName, ele.getSimpleName());
					}
				}
			}
		}
		
		for(VariableElement ele : items) {
			RegisterItem regItem = ele.getAnnotation(RegisterItem.class);
			if(itemCallback != null) {
				regBuilder.addStatement("$1T.$2L(new $3T($4S, $5S), $1T.$6L)", ownerClassName, itemCallback.getSimpleName(), identifierName, rc.value(), regItem.value(), ele.getSimpleName());
			} else {
				regBuilder.addStatement("$1T.register($1T.ITEM, new $2T($3S, $4S), $5T.$6L)", registryName, identifierName, rc.value(), regItem.value(), ownerClassName, ele.getSimpleName());
			}
		}
		
		if(postCallback != null) {
			regBuilder.addStatement("$1T.$2L()", ownerClassName, postCallback.getSimpleName());
		}
		
		Path projectPath;
		try {
			projectPath = Paths.get(filer.getResource(StandardLocation.CLASS_OUTPUT, "", ".vibecheck").toUri()).getParent().getParent().getParent();
			Path datafile = projectPath.resolve(".eclipse_cursed_mcap");
			regBuilder.addStatement("//$L", datafile.toString());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		TypeSpec registrar = TypeSpec.classBuilder(ownerClass.getSimpleName() + "Registrar")
				.addModifiers(Modifier.PUBLIC)
				//.addSuperinterface(modInitializerName)
				.addMethod(MethodSpec.methodBuilder("register").returns(TypeName.VOID).addModifiers(Modifier.PUBLIC, Modifier.STATIC).addCode(regBuilder.build()).build())
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
	
	private void genResources(RegistryContainer rc, List<RegisterBlock> blocks, List<RegisterItem> items) {
		for(RegisterBlock block : blocks) {
			BlockModelAction blockModelAction = block.model();
			if(blockModelAction == BlockModelAction.INHERIT_MOD) {
				blockModelAction = rc.blockModel();
			}
			
			if(blockModelAction.model) {
				resourceFromTemplate("blockmodel.json", "assets." + rc.value() + ".models.block", block.value() + ".json", rc.value(), block.value());
			}
			
			if(blockModelAction.state) {
				resourceFromTemplate("blockstate.json", "assets." + rc.value() + ".blockstates", block.value() + ".json", rc.value(), block.value());
			}
			
			BlockItemAction blockItemAction = block.blockItem();
			if(blockItemAction == BlockItemAction.INHERIT_MOD) {
				blockItemAction = rc.blockItem();
			}
			
			if(blockItemAction.generatesResource) {
				switch(blockItemAction) {
				case OTHER_TEXTURE:
					resourceFromTemplate("itemmodel.json", "assets." + rc.value() + ".models.item", block.value() + ".json", rc.value(), block.value());
					break;
				case BASIC_MODEL:
					resourceFromTemplate("blockitem.json", "assets." + rc.value() + ".models.item", block.value() + ".json", rc.value(), block.value());
					break;
				default:
				}
			}
			
			LootTableAction lootAction = block.loot();
			if(lootAction == LootTableAction.INHERIT_MOD) {
				lootAction = rc.loot();
			}
			
			String str = rc.value() + ":" + block.value();
			String template = lootAction == LootTableAction.DROP_SELF ? "loottable.json" : "silktable.json";
			switch(lootAction) {
			case DROP_OTHER:
				str = block.lootData();
			case DROP_SELF:
			case DROP_SELF_SILK:
				resourceFromTemplate(template, "data." + rc.value() + ".loot_tables.blocks", block.value() + ".json", str);
			default:
			}
		}
		
		for(RegisterItem item : items) {
			ItemModelAction itemModelAction = item.model();
			if(itemModelAction == ItemModelAction.INHERIT_MOD) {
				itemModelAction = rc.itemModel();
			}
			
			if(itemModelAction == ItemModelAction.BASIC_ITEM) {
				resourceFromTemplate("itemmodel.json", "assets." + rc.value() + ".models.item", item.value() + ".json", rc.value(), item.value());
			}
		}
	}
	
	private void resourceFromTemplate(String template, String pkg, String fileName, Object... formatArgs) {
		try(Writer w = filer.createResource(StandardLocation.CLASS_OUTPUT, pkg, fileName).openWriter();) {
			try(InputStream inStream = getClass().getClassLoader().getResourceAsStream("templates/" + template)) {
				String str = new BufferedReader(new InputStreamReader(inStream)).lines().collect(Collectors.joining("\n"));
				w.write(String.format(str, formatArgs));
			}
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