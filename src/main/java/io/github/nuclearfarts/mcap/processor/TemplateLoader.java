package io.github.nuclearfarts.mcap.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class TemplateLoader {
	private final Path rootDir;
	
	public TemplateLoader(Path dir) {
		rootDir = dir;
	}
	
	public LoadedTemplate load(String modId, String file) throws IOException {
		Path p = rootDir.resolve(file);
		return new LoadedTemplate(Files.newBufferedReader(p).lines().collect(Collectors.joining("\n")), modId);
	}
}
