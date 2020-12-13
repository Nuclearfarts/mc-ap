package io.github.nuclearfarts.mcap;

public enum BlockModelAction {
	INHERIT_MOD(false, false),
	NO_ACTION(false, false),
	BASIC_BLOCK(true, true),
	BLOCKSTATE_ONLY(true, false); // if you want a custom model on a single state block
	
	public final boolean state, model;
	
	private BlockModelAction(boolean state, boolean model) {
		this.state = state;
		this.model = model;
	}
}
