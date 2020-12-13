package io.github.nuclearfarts.mcap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.nuclearfarts.mcap.BlockItemAction;
import io.github.nuclearfarts.mcap.BlockModelAction;
import io.github.nuclearfarts.mcap.LootTableAction;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface RegisterBlock {
	/**
	 * Block's string ID.
	 */
	String value();
	
	/**
	 * Model generation action.
	 */
	BlockModelAction model() default BlockModelAction.INHERIT_MOD;
	
	/**
	 * Loot table generation action
	 */
	LootTableAction loot() default LootTableAction.INHERIT_MOD;
	
	/**
	 * For use with {@link LootTableAction.DROP_OTHER} to specify the ID to drop.
	 */
	String lootData() default "";
	
	/**
	 * Block item generation action.
	 */
	BlockItemAction blockItem() default BlockItemAction.INHERIT_MOD;
	
	/**
	 * Block item group.
	 */
	FieldRef itemGroup() default @FieldRef(clazz = Void.class, field = "");
}
