package io.github.nuclearfarts.mcap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.nuclearfarts.mcap.TriState;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface RegisterBlock {
	/**
	 * Block's string ID.
	 */
	String value();
	
	/**
	 * Model generation template.
	 * Builtins: basic (0 args)
	 */
	String model() default "#$%INHERIT";
	String[] modelArgs() default {};
	
	/**
	 * Loot table generation template. Empty string means none.
	 * Builtins: basic (0 args), silk (0 args), other (1 arg)
	 */
	String loot() default "#$%INHERIT";
	String[] lootArgs() default {};
	
	TriState autoBlockItem() default TriState.NONE;
	/**
	 * Block item model generation template. Must be disabled individually from autoBlockItem.
	 * Builtins: basic
	 */
	String blockItem() default "#$%INHERIT";
	String[] blockItemArgs() default {};
	
	String blockState() default "#$%INHERIT";
	String[] blockStateArgs() default {};
	
	/**
	 * Block item group.
	 */
	FieldRef itemGroup() default @FieldRef(clazz = Void.class, field = "#$%INHERIT");
}
