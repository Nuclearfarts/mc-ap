package io.github.nuclearfarts.mcap.processor;

import java.util.function.Consumer;

import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import com.squareup.javapoet.CodeBlock;
import io.github.nuclearfarts.mcap.annotation.RegisterBlock;

public class ParsedBlock {
	private final boolean autoBlockItem;
	private final LoadedTemplate blockItemTemplate, blockStateTemplate, blockModelTemplate, lootTemplate;
	private final String[] blockItemArgs, blockStateArgs, blockModelArgs, lootArgs;
	private final Name name;
	private final String id;
	private final ParsedFieldRef itemGroup;
	private final ParsedRegistryContainer container;
	
	public ParsedBlock(VariableElement ele, ParsedRegistryContainer container, Consumer<String> errorConsumer) {
		this.container = container;
		name = ele.getSimpleName();
		RegisterBlock block = ele.getAnnotation(RegisterBlock.class);
		itemGroup = container.getItemGroup(block.itemGroup());
		id = block.value();
		switch(block.autoBlockItem()) {
		case TRUE: autoBlockItem = true; break;
		case FALSE: autoBlockItem = false; break;
		case NONE: autoBlockItem = container.getAutoBlockItem(); break;
		default: throw new RuntimeException();
		}
		blockItemArgs = container.getArgs(block.blockItemArgs(), ProcessorTemplateType.BLOCKITEM);
		blockStateArgs = container.getArgs(block.blockStateArgs(), ProcessorTemplateType.BLOCKSTATE);
		blockModelArgs = container.getArgs(block.modelArgs(), ProcessorTemplateType.BLOCK);
		lootArgs = container.getArgs(block.lootArgs(), ProcessorTemplateType.LOOT);
		blockItemTemplate = container.getTemplateWithErrors(ProcessorTemplateType.BLOCKITEM, block.blockItem(), errorConsumer);
		blockStateTemplate = container.getTemplateWithErrors(ProcessorTemplateType.BLOCKSTATE, block.blockState(), errorConsumer);
		blockModelTemplate = container.getTemplateWithErrors(ProcessorTemplateType.BLOCK, block.model(), errorConsumer);
		lootTemplate = container.getTemplateWithErrors(ProcessorTemplateType.LOOT, block.loot(), errorConsumer);
	}
	
	public void appendTo(CodeBlock.Builder builder) {
		builder.addStatement("registerBlock($1T.$2L, $3S)", container.getOwnerName(), name, id);
		if(autoBlockItem) {
			if(itemGroup != null) {
				builder.addStatement("registerItem(createBlockItem($1T.$2L, $3T.$4L), $5S)", container.getOwnerName(), name, itemGroup.getTargetType(), itemGroup.getTargetName(), id);
			} else {
				builder.addStatement("registerItem(createBlockItem($1T.$2L), $3S)", container.getOwnerName(), name, id);
			}
		}
	}
	
	public void genResources(ResourceCreator rc) {
		if(blockItemTemplate != null) {
			rc.createResource(ProcessorTemplateType.BLOCKITEM.getPackage(container.getModId()), id + ".json", blockItemTemplate.with(id, blockItemArgs));
		}
		if(blockStateTemplate != null) {
			rc.createResource(ProcessorTemplateType.BLOCKSTATE.getPackage(container.getModId()), id + ".json", blockStateTemplate.with(id, blockStateArgs));
		}
		if(blockModelTemplate != null) {
			rc.createResource(ProcessorTemplateType.BLOCK.getPackage(container.getModId()), id + ".json", blockModelTemplate.with(id, blockModelArgs));
		}
		if(lootTemplate != null) {
			rc.createResource(ProcessorTemplateType.BLOCKSTATE.getPackage(container.getModId()), id + ".json", lootTemplate.with(id, lootArgs));
		}
	}
}
