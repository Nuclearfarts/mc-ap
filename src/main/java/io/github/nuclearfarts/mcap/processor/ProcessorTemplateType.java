package io.github.nuclearfarts.mcap.processor;

import io.github.nuclearfarts.mcap.TemplateType;

public enum ProcessorTemplateType {
	ITEM(TemplateType.ITEM, "assets.%s.models.item"),
	BLOCK(TemplateType.BLOCK, "assets.%s.models.block"),
	BLOCKSTATE(TemplateType.BLOCKSTATE, "assets.%s.blockstates"),
	BLOCKITEM(TemplateType.ITEM, "assets.%s.models.item"),
	LOOT(TemplateType.LOOT, "data.%s.loot_tables.blocks");
	private final TemplateType eq;
	private final String pkg;
	
	private ProcessorTemplateType(TemplateType equiv, String pkg) {
		eq = equiv;
		this.pkg = pkg;
	}
	
	public TemplateType getEquivalent() {
		return eq;
	}
	
	public String getPackage(String modId) {
		return String.format(pkg, modId);
	}
}
