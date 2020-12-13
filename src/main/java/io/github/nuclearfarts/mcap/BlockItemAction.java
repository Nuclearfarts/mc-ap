package io.github.nuclearfarts.mcap;

public enum BlockItemAction {
	INHERIT_MOD(false, false),
	NO_ACTION(false, false),
	NO_RESOURCE(true, false),
	BASIC_MODEL(true, true),
	OTHER_TEXTURE(true, true);
	
	public final boolean registersItem;
	public final boolean generatesResource;
	
	private BlockItemAction(boolean registers, boolean resource) {
		registersItem = registers;
		generatesResource = resource;
	}
}
