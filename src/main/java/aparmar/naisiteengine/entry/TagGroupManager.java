package aparmar.naisiteengine.entry;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.CONFIG_TAG_GROUP_PREFIX;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import aparmar.naisiteengine.entry.TagGroupData.TagEntry;

public class TagGroupManager {
	private static final ObjectMapper mapper = new ObjectMapper();
	
	private final HashMap<String, TagGroupData> tagGroups = new HashMap<>();
	private final HashMap<String, TagEntry> tagMap = new HashMap<>();
	
	private final File configDirectory;
	
	public TagGroupManager(File configDirectory) throws IOException {
		this.configDirectory = configDirectory;
		
		reloadTagGroupsFromFileSystem();
	}

	private void reloadTagGroupsFromFileSystem() throws IOException {
		Arrays.stream(configDirectory.listFiles())
			.filter(File::isFile)
			.filter(File::canRead)
			.filter(f->f.getName().endsWith(".yaml") || f.getName().endsWith(".yml"))
			.filter(f->f.getName().startsWith(CONFIG_TAG_GROUP_PREFIX))
			.map(f -> {
				try {
					return mapper.readValue(f, TagGroupData.class);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			})
			.filter(Objects::nonNull)
			.forEach(tagGroup->tagGroups.put(tagGroup.getName(), tagGroup));
		
		tagMap.clear();
		for (TagGroupData tagGroup : tagGroups.values()) {
			for (TagEntry tag : tagGroup.getTagEntries()) {
				tagMap.put(tag.getName(), tag);
			}
		}
	}

	public TagGroupData[] getTagGroups() { return tagGroups.values().toArray(new TagGroupData[0]); }
	public TagGroupData getTagGroupByName(String name) { return tagGroups.get(name); }
	
	public TagEntry getTagByName(String name) { return tagMap.get(name); }
	
	public String tagStringsToTextGenerationContextForm(String[] entries) {
		return TagEntry.toTextGenerationContextForm(Arrays.stream(entries)
				.map(this::getTagByName)
				.toArray(TagEntry[]::new));
	}
}
