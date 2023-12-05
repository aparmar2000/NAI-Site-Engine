package aparmar.naisiteengine.config;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryType;
import aparmar.naisiteengine.entry.EntryTypeManager;
import aparmar.naisiteengine.entry.TagGroupManager;
import lombok.Getter;

public class SiteConfigManager {
	@Getter
	private final EntryTypeManager entryTypeManager;
	@Getter
	private final TagGroupManager tagGroupManager;
	
	public SiteConfigManager(File rootConfigDirectory, UserConfiguration config) throws IOException {
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
