package io.github.nuclearfarts.mcap.processor;

import java.util.function.Consumer;

import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;

import com.squareup.javapoet.CodeBlock;

import io.github.nuclearfarts.mcap.annotation.RegisterItem;

public class ParsedItem {
	private final LoadedTemplate template;
	private final String[] args;
	private final String name;
	private final Name fieldName;
	private final ParsedRegistryContainer container;
	
	public ParsedItem(VariableElement ele, ParsedRegistryContainer container, Consumer<String> errorConsumer) {
		this.container = container;
		fieldName = ele.getSimpleName();
		RegisterItem item = ele.getAnnotation(RegisterItem.class);
		name = item.value();
		args = container.getArgs(item.modelArgs(), ProcessorTemplateType.ITEM);
		template = container.getTemplateWithErrors(ProcessorTemplateType.ITEM, item.model(), errorConsumer);
	}
	
	public void appendTo(CodeBlock.Builder builder) {
		builder.addStatement("registerItem($1T.$2L, $3S)", container.getOwnerName(), fieldName, name);
	}
	
	public void genResources(ResourceCreator rc) {
		if(template != null) {
			rc.createResource(ProcessorTemplateType.ITEM.getPackage(container.getModId()), name + ".json", template.with(name, args));
		}
	}
}
