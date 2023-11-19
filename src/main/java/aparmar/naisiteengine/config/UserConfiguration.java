package aparmar.naisiteengine.config;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.MAIN_THREAD_LOGGER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import lombok.Data;

@Data
public class UserConfiguration {
	private final static String GENERATION_SETTINGS_KEY = "generation";
	private final static String WEBSITE_SETTINGS_KEY = "website";
	
	private final Dump yamlWriter = new Dump(DumpSettings.builder().build());
	
	private final GenerationConfiguration generationConfig;
	private final WebsiteConfiguration websiteConfig;
	
	@SuppressWarnings("unchecked")
	public UserConfiguration(String externalPath, String internalTemplatePath) throws IOException {
		MAIN_THREAD_LOGGER.info("Loading config file...");
		
		File externalYamlFile = new File(externalPath);
		if (!externalYamlFile.isFile()) {
			MAIN_THREAD_LOGGER.info("Config file not found, writing default config...");
			byte[] templateData = IOUtils.resourceToByteArray(internalTemplatePath);
			FileUtils.writeByteArrayToFile(externalYamlFile, templateData);
		}
		
		Load yamlReader = new Load(LoadSettings.builder().build());
		GenerationConfiguration loadedGenerationConfig = new GenerationConfiguration();
		WebsiteConfiguration loadedWebsiteConfig = new WebsiteConfiguration();
		try (FileInputStream in = new FileInputStream(externalYamlFile)) {
			Map<String, Object> yamlData = (Map<String, Object>) yamlReader.loadFromInputStream(in);
			
			Object generationSettingsData = yamlData.get(GENERATION_SETTINGS_KEY);
			if (generationSettingsData != null && generationSettingsData instanceof Map) {
				loadedGenerationConfig = new GenerationConfiguration((Map<String, Object>) generationSettingsData);
			}
			
			Object websiteSettingsData = yamlData.get(WEBSITE_SETTINGS_KEY);
			if (websiteSettingsData != null && websiteSettingsData instanceof Map) {
				loadedWebsiteConfig = new WebsiteConfiguration((Map<String, Object>) websiteSettingsData);
			}
		}
		
		generationConfig = loadedGenerationConfig;
		websiteConfig = loadedWebsiteConfig;
		
		MAIN_THREAD_LOGGER.info("Config file loaded.");
	}
}
