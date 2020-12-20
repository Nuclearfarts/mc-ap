package io.github.nuclearfarts.mcap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RegistryContainer {
	/**
	 * Namespace, usually your modid.
	 */
	String value();
	
	/**
	 * Block model action to be inherited.
	 */
	String block() default "";
	String[] blockArgs() default {};
	
	/**
	 * Blockstate action to be inherited.
	 */
	String blockState() default "";
	String[] blockStateArgs() default {};
	
	/**
	 * Item model action to be inherited. Empty string means none.
	 * Builtins: 
	 */
	String item() default "";
	String[] itemArgs() default {};
	
	/**
	 * Loot table template to be inherited. Empty string means none.
	 */
	String loot() default "";
	String[] lootArgs() default {};
	/**
	 * Whether or not to auto-generate a block item.
	 */
	boolean autoBlockItem() default false;
	/**
	 * Block item model template. Empty string means none. Only used when autoBlockItem is true.
	 */
	String blockItem() default "";
	String[] blockItemArgs() default {};
	
	/**
	 * Block item group.
	 */
	FieldRef itemGroup() default @FieldRef(clazz = Void.class, field = "");
	
	/**
	 * Custom templates.
	 */
	Template[] templates() default {};
}
