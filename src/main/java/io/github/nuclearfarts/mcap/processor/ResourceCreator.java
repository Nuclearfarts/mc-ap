package io.github.nuclearfarts.mcap.processor;

@FunctionalInterface
public interface ResourceCreator {
	void createResource(String pkg, String fileName, String contents);
}
