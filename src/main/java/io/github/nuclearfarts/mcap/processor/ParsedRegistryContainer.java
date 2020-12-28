package io.github.nuclearfarts.mcap.processor;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import com.squareup.javapoet.TypeName;

import io.github.nuclearfarts.mcap.TemplateType;
import io.github.nuclearfarts.mcap.annotation.FieldRef;
import io.github.nuclearfarts.mcap.annotation.RegistryContainer;
import io.github.nuclearfarts.mcap.annotation.Template;

public class ParsedRegistryContainer {
	private final Map<String, LoadedTemplate> itemTemplates = new HashMap<>();
	private final Map<String, LoadedTemplate> blockTemplates = new HashMap<>();
	private final Map<String, LoadedTemplate> blockStateTemplates = new HashMap<>();
	private final Map<String, LoadedTemplate> lootTemplates = new HashMap<>();
	private final Map<TemplateType, Map<String, LoadedTemplate>> templates = new EnumMap<>(TemplateType.class);
	private final boolean autoBlockItem;
	//private final LoadedTemplate itemTemplate, blockItemTemplate, blockTemplate, lootTemplate, blockStateTemplate;
	private final Map<ProcessorTemplateType, LoadedTemplate> defaultTemplates = new EnumMap<>(ProcessorTemplateType.class);
	//private final String[] itemArgs, blockItemArgs, blockArgs, lootArgs, blockStateArgs;
	private final Map<ProcessorTemplateType, String[]> defaultArgs = new EnumMap<>(ProcessorTemplateType.class);
	private final String modId;
	private final ParsedFieldRef itemGroup;
	private final Function<FieldRef, ParsedFieldRef> fieldRefParser;
	private final TypeName ownerName;
	
	public ParsedRegistryContainer(RegistryContainer c, TemplateLoader tLoader, TypeName ownerName, Consumer<String> errorConsumer, Function<FieldRef, ParsedFieldRef> fieldRefParser) {
		itemGroup = fieldRefParser.apply(c.itemGroup());
		this.fieldRefParser = fieldRefParser;
		this.ownerName = ownerName;
		modId = c.value();
		autoBlockItem = c.autoBlockItem();
		templates.put(TemplateType.BLOCK, blockTemplates);
		templates.put(TemplateType.ITEM, itemTemplates);
		templates.put(TemplateType.BLOCKSTATE, blockStateTemplates);
		templates.put(TemplateType.LOOT, lootTemplates);
		try {
			loadDefaultTemplates(templates, modId);
		} catch (IOException e1) {
			errorConsumer.accept(String.format("Error loading default templates: %s: %s", e1.getClass(), e1.getLocalizedMessage()));
		}
		for(Template t : c.templates()) {
			try {
				templates.get(t.type()).put(t.name(), tLoader.load(modId, t.file()));
			} catch (IOException e) {
				errorConsumer.accept(String.format("Error loading %s for template %s: %s: %s", t.file(), t.name(), e.getClass(), e.getLocalizedMessage()));
			}
		}
		/*itemArgs = c.itemArgs();
		blockItemArgs = c.blockItemArgs();
		blockArgs = c.blockArgs();
		lootArgs = c.lootArgs();
		blockStateArgs = c.blockStateArgs();*/
		defaultArgs.put(ProcessorTemplateType.BLOCK, c.blockArgs());
		defaultArgs.put(ProcessorTemplateType.BLOCKITEM, c.blockItemArgs());
		defaultArgs.put(ProcessorTemplateType.BLOCKSTATE, c.blockStateArgs());
		defaultArgs.put(ProcessorTemplateType.ITEM, c.itemArgs());
		defaultArgs.put(ProcessorTemplateType.LOOT, c.lootArgs());
		/*itemTemplate = c.item().isEmpty() ? null : getTemplateWithErrors(TemplateType.ITEM, c.item(), errorConsumer);
		blockItemTemplate = c.blockItem().isEmpty() ? null : getTemplateWithErrors(TemplateType.ITEM, c.blockItem(), errorConsumer);
		lootTemplate = c.loot().isEmpty() ? null : getTemplateWithErrors(TemplateType.LOOT, c.loot(), errorConsumer);
		blockTemplate = c.block().isEmpty() ? null : getTemplateWithErrors(TemplateType.BLOCK, c.block(), errorConsumer);
		blockStateTemplate = c.blockState().isEmpty() ? null : getTemplateWithErrors(TemplateType.BLOCKSTATE, c.blockState(), errorConsumer);*/
		defaultTemplates.put(ProcessorTemplateType.ITEM, c.item().isEmpty() ? null : getTemplateWithErrors(ProcessorTemplateType.ITEM, c.item(), errorConsumer));
		defaultTemplates.put(ProcessorTemplateType.BLOCKITEM, c.blockItem().isEmpty() ? null : getTemplateWithErrors(ProcessorTemplateType.ITEM, c.blockItem(), errorConsumer));
		defaultTemplates.put(ProcessorTemplateType.LOOT, c.loot().isEmpty() ? null : getTemplateWithErrors(ProcessorTemplateType.LOOT, c.loot(), errorConsumer));
		defaultTemplates.put(ProcessorTemplateType.BLOCK, c.block().isEmpty() ? null : getTemplateWithErrors(ProcessorTemplateType.BLOCK, c.block(), errorConsumer));
		defaultTemplates.put(ProcessorTemplateType.BLOCKSTATE, c.blockState().isEmpty() ? null : getTemplateWithErrors(ProcessorTemplateType.BLOCKSTATE, c.blockState(), errorConsumer));
	}
	
	private static void loadDefaultTemplates(Map<TemplateType, Map<String, LoadedTemplate>> templates, String modId) throws IOException {
		templates.get(TemplateType.ITEM).put("basic", LoadedTemplate.loadBuiltin("itemmodel", modId));
		templates.get(TemplateType.ITEM).put("block", LoadedTemplate.loadBuiltin("blockitem", modId));
		templates.get(TemplateType.BLOCK).put("basic", LoadedTemplate.loadBuiltin("blockmodel", modId));
		templates.get(TemplateType.BLOCKSTATE).put("basic", LoadedTemplate.loadBuiltin("blockstate", modId));
		templates.get(TemplateType.LOOT).put("basic", LoadedTemplate.loadBuiltin("loottable", modId));
		templates.get(TemplateType.LOOT).put("silk", LoadedTemplate.loadBuiltin("silktable", modId));
	}
	
	public LoadedTemplate getTemplate(ProcessorTemplateType type, String name) {
		if("#$%INHERIT".equals(name)) {
			return defaultTemplates.get(type);
		}
		return templates.get(type.getEquivalent()).get(name);
	}
	
	public LoadedTemplate getTemplateWithErrors(ProcessorTemplateType type, String name, Consumer<String> errorConsumer) {
		LoadedTemplate t;
		if((t = getTemplate(type, name)) == null && !name.isEmpty() && !name.equals("#$%INHERIT")) {
			errorConsumer.accept(String.format("%s template not found: %s", type.getEquivalent().toString(), name));
		}
		return t;
	}
	
	public String[] getArgs(String[] args, ProcessorTemplateType type) {
		if(args.length == 0) {
			return defaultArgs.get(type);
		} else {
			return args;
		}
	}

	public boolean getAutoBlockItem() {
		return autoBlockItem;
	}
	
	public String getModId() {
		return modId;
	}

	public ParsedFieldRef getItemGroup(FieldRef ref) {
		if(!ref.field().equals("#$%INHERIT")) {
			return fieldRefParser.apply(ref);
		} else {
			return itemGroup;
		}
	}

	public TypeName getOwnerName() {
		return ownerName;
	}
}
