package aparmar.naisiteengine.entry;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.MAIN_THREAD_LOGGER;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import aparmar.naisiteengine.config.SiteConfigManager;
import aparmar.naisiteengine.config.UserConfiguration;
import aparmar.naisiteengine.entry.EntryFieldConfig.EntryFieldType;
import aparmar.naisiteengine.entry.TagGroupData.TagEntry;
import aparmar.naisiteengine.utils.NaiSiteEngineConstants;
import lombok.Getter;

public class EntryManager {
	public static final String TEMPLATE_ENTRY_FOLDER_NAME = "templates";
	public static final String SPECIAL_ALL_TAG_STRING = "all";
	
	private static final ObjectMapper mapper = NaiSiteEngineConstants.OBJECT_MAPPER;
	
	@Getter
	private File rootEntryDirectory, templateEntryDirectory, generatedEntryDirectory;
	private final SiteConfigManager siteConfigManager;
	private final UserConfiguration config;
	
	private final HashMap<Integer, EntryData> templateEntries = new HashMap<>();
	private final HashMap<String, List<EntryData>> templateEntriesByType = new HashMap<>();
	
	private final HashMap<Integer, EntryData> generatedEntries = new HashMap<>();
	private final HashMap<String, List<EntryData>> generatedEntriesByTag = new HashMap<>();
	private final HashMap<String, List<EntryData>> generatedEntriesByType = new HashMap<>();
	
	private final AtomicInteger templateIdCounter = new AtomicInteger(-1);
	private final AtomicInteger generatedIdCounter = new AtomicInteger(1);
	
	public EntryManager(File rootEntryDirectory, SiteConfigManager siteConfigManager, UserConfiguration config) throws IOException {
		this.siteConfigManager = siteConfigManager;
		this.config = config;
		
		if (rootEntryDirectory.exists() && !rootEntryDirectory.isDirectory()) {
			throw new IllegalArgumentException(rootEntryDirectory+" exists and is not a directory!");
		}
		this.rootEntryDirectory = rootEntryDirectory;
		Files.createDirectories(rootEntryDirectory.toPath(), new FileAttribute<?>[0]);
		
		templateEntryDirectory = rootEntryDirectory.toPath().resolve(TEMPLATE_ENTRY_FOLDER_NAME).toFile();
		if (templateEntryDirectory.exists() && !templateEntryDirectory.isDirectory()) {
			throw new IllegalArgumentException(templateEntryDirectory+" exists and is not a directory!");
		}
		if (!templateEntryDirectory.exists()) {
			templateEntryDirectory.mkdir();
			MAIN_THREAD_LOGGER.info("Template entry folder not found, creating and writing example...");
			byte[] templateData = IOUtils.resourceToByteArray("/example-entry.yaml");
			FileUtils.writeByteArrayToFile(templateEntryDirectory.toPath().resolve("example-entry.yaml").toFile(), templateData);
		}
		
		generatedEntryDirectory = rootEntryDirectory.toPath().resolve("generated").toFile();
		if (generatedEntryDirectory.exists() && !generatedEntryDirectory.isDirectory()) {
			throw new IllegalArgumentException(generatedEntryDirectory+" exists and is not a directory!");
		}
		if (!generatedEntryDirectory.exists()) {
			generatedEntryDirectory.mkdir();
		}
		
		reloadEntriesFromFileSystem();
	}
	
	public void reloadEntriesFromFileSystem() {
		templateEntries.clear();
		templateEntriesByType.clear();
		for (File file : templateEntryDirectory.listFiles()) {
			if (!file.isFile()) { continue; }
			if (!file.getName().endsWith(".yaml")) { continue; }
			
			loadEntryData(file, false);
		}

		generatedEntries.clear();
		generatedEntriesByTag.clear();
		for (File file : generatedEntryDirectory.listFiles()) {
			if (!file.isFile()) { continue; }
			if (!file.getName().endsWith(".yaml")) { continue; }
			
			loadEntryData(file, true);
		}
	}
	
	private EntryData loadEntryData(File inFile, boolean isGenerated) {
		EntryData loadedEntry = null;
		try {
			loadedEntry = mapper.readValue(inFile, EntryData.class);
			loadedEntry.setSiteConfigManager(siteConfigManager);

			final int loadedId = loadedEntry.getId();
			if (isGenerated) {
				generatedIdCounter.getAndUpdate(i->Math.max(i, loadedId));
				addGeneratedEntry(loadedEntry);
				
				generatedEntriesByType
					.getOrDefault(loadedEntry.getEntryTypeName(), new LinkedList<>())
					.add(loadedEntry);
			} else {
				loadedEntry.setId(templateIdCounter.getAndDecrement());
				templateEntries.put(loadedEntry.getId(), loadedEntry);
				
				templateEntriesByType
					.getOrDefault(loadedEntry.getEntryTypeName(), new LinkedList<>())
					.add(loadedEntry);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return loadedEntry;
	}
	
	public synchronized void saveGeneratedEntriesToFileSystem() {
		HashSet<String> expectedFilenames = new HashSet<>();
		for (EntryData generatedEntry : generatedEntries.values()) {
			Arrays.stream(generatedEntry.getEntryType().getEntryFieldConfigs())
				.filter(f->f.getType() == EntryFieldType.IMAGE)
				.map(generatedEntry::getField)
				.filter(Objects::nonNull)
				.forEach(expectedFilenames::add);
			if (generatedEntry.getRating() == 0) { continue; }
			
			String outputFilename = generatedEntry.getEntryFilename()+".yaml";
			File outputFile = new File(generatedEntryDirectory+"/"+outputFilename);
			
			expectedFilenames.add(outputFilename);
			
			try {
				mapper.writeValue(outputFile, generatedEntry);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for (File testFile : generatedEntryDirectory.listFiles()) {
			if (!testFile.canWrite()) { continue; }
			if (expectedFilenames.contains(testFile.getName())) { continue; }
			if (!(testFile.getName().endsWith(".png") || testFile.getName().endsWith(".yaml"))) { continue; }
			
			testFile.delete();
		}
	}

	public synchronized void saveExistingGeneratedEntry(int id) {
		EntryData entryToSave = generatedEntries.get(id);
		if (entryToSave==null) { return; }

		File outputFile = new File(generatedEntryDirectory+"/"+entryToSave.getEntryFilename()+".yaml");
		try {
			mapper.writeValue(outputFile, entryToSave);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addGeneratedEntry(EntryData entryData) {
		while (entryData.getId()<=0 || generatedEntries.containsKey(entryData.getId())) {
			entryData.setId(generatedIdCounter.getAndIncrement());
		}
		generatedEntries.put(entryData.getId(), entryData);
		
		generatedEntriesByTag.putIfAbsent(SPECIAL_ALL_TAG_STRING, new ArrayList<EntryData>());
		generatedEntriesByTag.get(SPECIAL_ALL_TAG_STRING).add(entryData);
		for (String tag : entryData.getTags()) {
			generatedEntriesByTag.putIfAbsent(tag, new ArrayList<EntryData>());
			generatedEntriesByTag.get(tag).add(entryData);
		}
	}
	
	public EntryData[] getTemplateEntries() {
		return templateEntries.values().stream()
				.toArray(EntryData[]::new);
	}

	public EntryData[] getGeneratedEntries() {
		return generatedEntries.values().stream()
				.toArray(EntryData[]::new);
	}
	public EntryData[] getScoredGeneratedEntries() {
		return generatedEntries.values().stream()
				.filter(entry->entry.getRating()>0)
				.toArray(EntryData[]::new);
	}

	public EntryData getEntryById(int id) {
		return generatedEntries.getOrDefault(Integer.valueOf(id), templateEntries.get(Integer.valueOf(id)));
	}
	public EntryData getTemplateEntryById(int id) {
		return templateEntries.get(Integer.valueOf(id));
	}
	public EntryData getGeneratedEntryById(int id) {
		return generatedEntries.get(Integer.valueOf(id));
	}
	public int getRandomGeneratedEntryIdByCategory(Random rng, String category) {
		int[] idArray = generatedEntriesByTag.get(category)
				.stream()
				.mapToInt(EntryData::getId)
				.toArray();
		return idArray[rng.nextInt(idArray.length)];
	}
	
	public int getTemplateEntryCount() { return templateEntries.size(); }
	public Map<String, Integer> getTemplateEntryCountByTag() {
		HashMap<String, Integer> countMap = new HashMap<>();
		Arrays.stream(siteConfigManager.getTagGroupManager().getTagGroups())
			.map(TagGroupData::getTagEntries)
			.flatMap(Arrays::stream)
			.map(TagEntry::getName)
			.forEach(t->countMap.put(t, 0));
		
		for (EntryData templateEntry : templateEntries.values()) {
			for (String tag : templateEntry.getTags()) {
				countMap.merge(tag, 1, (a,b)->a+b);
			}
		}
		
		return countMap;
	}
	public int getGeneratedEntryCount() { return generatedEntries.size(); }
	public Map<String, Integer> getGeneratedEntryCountByTag() {
		HashMap<String, Integer> countMap = new HashMap<>();
		Arrays.stream(siteConfigManager.getTagGroupManager().getTagGroups())
			.map(TagGroupData::getTagEntries)
			.flatMap(Arrays::stream)
			.map(TagEntry::getName)
			.forEach(t->countMap.put(t, 0));
		
		for (EntryData generatedEntry : generatedEntries.values()) {
			for (String tag : generatedEntry.getTags()) {
				countMap.merge(tag, 1, (a,b)->a+b);
			}
		}
		
		return countMap;
	}
	public int getUnreadGeneratedEntryCount() {
		return (int) generatedEntries.values().stream()
				.filter(entry->entry.getRating()<0)
				.count();
	}
	public Map<String, Integer> getUnreadGeneratedEntryCountByType() {
		HashMap<String, Integer> countMap = new HashMap<>();
		Arrays.stream(siteConfigManager.getEntryTypeManager().getEntryTypeNames())
			.forEach(t->countMap.put(t, 0));
		
		for (EntryData generatedEntry : generatedEntries.values()) {
			if (generatedEntry.getRating()>=0) { continue; }
			countMap.merge(generatedEntry.getEntryTypeName(), 1, (a,b)->a+b);
		}
		
		return countMap;
	}
}
