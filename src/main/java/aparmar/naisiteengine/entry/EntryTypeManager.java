package aparmar.naisiteengine.entry;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.CONFIG_ENTRY_TYPE_PREFIX;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EntryTypeManager {
	private static final ObjectMapper mapper = new ObjectMapper();
	
	private final HashMap<String, EntryType> entryTypes = new HashMap<>();
	
	private final File configDirectory;
	
	public EntryTypeManager(File configDirectory) throws IOException {
		this.configDirectory = configDirectory;
		
		reloadEntryTypesFromFileSystem();
	}

	private void reloadEntryTypesFromFileSystem() {
		Arrays.stream(configDirectory.listFiles())
			.filter(File::isFile)
			.filter(File::canRead)
			.filter(f->f.getName().endsWith(".yaml") || f.getName().endsWith(".yml"))
			.filter(f->f.getName().startsWith(CONFIG_ENTRY_TYPE_PREFIX))
			.map(f -> {
				try {
					return mapper.readValue(f, EntryType.class);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			})
			.filter(Objects::nonNull)
			.forEach(entryType->entryTypes.put(entryType.getName(), entryType));
	}
	
	public String[] getEntryTypeNames() {
		return entryTypes.keySet().toArray(new String[0]);
	}
	
	public EntryType getEntryTypeByName(String name) { return entryTypes.get(name); }
	public int getEntryTypeCount() { return entryTypes.size(); }

}
