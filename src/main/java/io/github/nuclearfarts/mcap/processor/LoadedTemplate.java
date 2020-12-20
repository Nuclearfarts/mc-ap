package io.github.nuclearfarts.mcap.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class LoadedTemplate {
	private final String text;
	
	public LoadedTemplate(String templateText, String modId) {
		text = templateText.replace("${modid}", modId);
	}
	
	public static LoadedTemplate loadBuiltin(String loc, String modId) throws IOException {
		try(BufferedReader r = new BufferedReader(new InputStreamReader(LoadedTemplate.class.getClassLoader().getResourceAsStream("templates/" + loc + ".json")))) {
			String text = r.lines().collect(Collectors.joining("\n"));
			return new LoadedTemplate(text, modId);
		}
	}
	
	public String with(String ownId, String[] args) {
		return String.format(text.replace("${id}", ownId), (Object[]) args);
	}
}
