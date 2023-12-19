package aparmar.naisiteengine.config;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.MAIN_THREAD_LOGGER;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryType;
import aparmar.naisiteengine.entry.EntryTypeManager;
import aparmar.naisiteengine.entry.TagGroupManager;
import lombok.Getter;

public class SiteConfigManager {
	private static final String[] defaultConfigFiles = new String[] {
			"entry-article.yaml",
			"tag-group-category.yaml"
	};
	
	@Getter
	private final EntryTypeManager entryTypeManager;
	@Getter
	private final TagGroupManager tagGroupManager;
	
	public SiteConfigManager(File rootConfigDirectory, UserConfiguration config) throws IOException {
		if (!rootConfigDirectory.isDirectory()) {
			MAIN_THREAD_LOGGER.info("Website config folder not found, writing default configs...");
			Path rootConfigPath = rootConfigDirectory.toPath();
			Files.createDirectories(rootConfigPath);
			
			for (String defaultConfigFile : defaultConfigFiles) {
				byte[] templateData = IOUtils.resourceToByteArray("/config-template/"+defaultConfigFile);
				FileUtils.writeByteArrayToFile(rootConfigPath.resolve(defaultConfigFile).toFile(), templateData);
			}
		}
		
		entryTypeManager = new EntryTypeManager(rootConfigDirectory);
		tagGroupManager = new TagGroupManager(rootConfigDirectory);
	}
	
	/**
	 * Supplier of {@link EntryData} instances; prefer over using constructor.
	 * @param entryType
	 * @return
	 */
	public EntryData createEntry(EntryType entryType) {
		EntryData parsedEntry = new EntryData();
		parsedEntry.setId(-1);
		parsedEntry.setCreationDateTime(LocalDateTime.now());
		parsedEntry.setRating(-10);
		parsedEntry.setSiteConfigManager(this);
		parsedEntry.setEntryType(entryType);
		parsedEntry.setFieldMap(entryType.populateFieldMapWithDefaults());
		
		return parsedEntry;
	}
}
