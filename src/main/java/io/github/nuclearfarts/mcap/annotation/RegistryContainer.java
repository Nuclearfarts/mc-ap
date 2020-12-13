package io.github.nuclearfarts.mcap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.nuclearfarts.mcap.BlockItemAction;
import io.github.nuclearfarts.mcap.BlockModelAction;
import io.github.nuclearfarts.mcap.ItemModelAction;
import io.github.nuclearfarts.mcap.LootTableAction;

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
	BlockModelAction blockModel() default BlockModelAction.NO_ACTION;
	
	/**
	 * Item model action to be inherited.
	 */
	ItemModelAction itemModel() default ItemModelAction.NO_ACTION;
	
	/**
	 * Loot table action to be inherited.
	 */
	LootTableAction loot() default LootTableAction.NO_ACTION;
	
	/**
	 * Block item action to be inherited.
	 */
	BlockItemAction blockItem() default BlockItemAction.NO_ACTION;
	
	/**
	 * Block item group.
	 */
	FieldRef itemGroup() default @FieldRef(clazz = Void.class, field = "");
}
