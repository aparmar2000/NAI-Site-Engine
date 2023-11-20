package aparmar.naisiteengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import aparmar.naisiteengine.config.UserConfiguration;
import lombok.Getter;

public class EntryManager {
	public static final String TEMPLATE_ENTRY_FOLDER_NAME = "templates";
	public static final String SPECIAL_ALL_CATEGORY_STRING = "all";
	
	@Getter
	private File rootEntryDirectory, templateEntryDirectory, generatedEntryDirectory;
	private final UserConfiguration config;
	
	private EntryData[] templateEntries = new EntryData[0];
	private final HashMap<Integer, EntryData> generatedEntries = new HashMap<>();
	private final HashMap<String, List<EntryData>> generatedEntriesByCategory = new HashMap<>();
	private final AtomicInteger idCounter = new AtomicInteger(1);
	
	public EntryManager(File rootEntryDirectory, UserConfiguration config) throws IOException {
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
		templateEntries = Arrays.stream(templateEntryDirectory.listFiles())
			.filter(File::isFile)
			.filter(f->f.getName().endsWith(".yaml") || f.getName().endsWith(".yml"))
			.map(f->{
				try {
					return EntryData.loadFromFile(f);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return null;
			})
			.filter(Objects::nonNull)
			.toArray(EntryData[]::new);

		ArrayList<EntryData> loadedEntries = new ArrayList<>();
		for (File file : generatedEntryDirectory.listFiles()) {
			if (!file.isFile()) { continue; }
			if (!file.getName().endsWith(".yaml")) { continue; }
			
			try {
				EntryData loadedEntry = EntryData.loadFromFile(file);
				loadedEntries.add(loadedEntry);
				idCounter.getAndUpdate(i->Math.max(i, loadedEntry.getId()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		generatedEntries.clear();
		generatedEntriesByCategory.clear();
		for (EntryData loadedEntry : loadedEntries) {
			addGeneratedEntry(loadedEntry);
		}
	}
	
	public synchronized void saveGeneratedEntriesToFileSystem() {
		HashSet<String> expectedFilenames = new HashSet<>();
		for (EntryData generatedEntry : generatedEntries.values()) {
			expectedFilenames.add(generatedEntry.getImgFilename());
			if (generatedEntry.getHalfStarRating() == 0) { continue; }
			
			String outputFilename = generatedEntry.getEntryFilename()+".yaml";
			File outputFile = new File(generatedEntryDirectory+"/"+outputFilename);
			
			expectedFilenames.add(outputFilename);
			
			try {
				generatedEntry.saveToFile(outputFile);
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
			entryToSave.saveToFile(outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addGeneratedEntry(EntryData entryData) {
		while (entryData.getId()<=0 || generatedEntries.containsKey(entryData.getId())) {
			entryData.setId(idCounter.getAndIncrement());
		}
		generatedEntries.put(entryData.getId(), entryData);
		
		generatedEntriesByCategory.putIfAbsent(SPECIAL_ALL_CATEGORY_STRING, new ArrayList<EntryData>());
		generatedEntriesByCategory.get(SPECIAL_ALL_CATEGORY_STRING).add(entryData);
		if (!entryData.getCategory().isEmpty()) {
			generatedEntriesByCategory.putIfAbsent(entryData.getCategory(), new ArrayList<EntryData>());
			generatedEntriesByCategory.get(entryData.getCategory()).add(entryData);
		}
	}
	
	public EntryData[] getTemplateEntries() {
		return templateEntries.clone();
	}
	
	public EntryData getGeneratedEntryById(int id) {
		return generatedEntries.get(Integer.valueOf(id));
	}
	public Optional<EntryData> tryGetGeneratedEntryWithoutImage() {
		return generatedEntries.values().stream()
				.filter(entry->entry.getImgFilename().isEmpty())
				.findAny();
	}
	public EntryData[] getGeneratedEntriesOfCategoryOrderedByNewest(String category) {
		return generatedEntriesByCategory.get(category).stream()
			.sorted(Comparator.comparing(EntryData::getCreationDateTime).reversed())
			.toArray(EntryData[]::new);
	}
	public int getRandomGeneratedEntryIdByCategory(Random rng, String category) {
		int[] idArray = generatedEntriesByCategory.get(category)
				.stream()
				.mapToInt(EntryData::getId)
				.toArray();
		return idArray[rng.nextInt(idArray.length)];
	}
	public int getLatestGeneratedEntryIdByCategory(String category) {
		return generatedEntriesByCategory.get(category).stream()
				.mapToInt(EntryData::getId)
				.max().orElse(0);
	}
	
	public int getTemplateEntryCount() { return templateEntries.length; }
	public Map<String, Integer> getTemplateEntryCountByCategory() {
		HashMap<String, Integer> countMap = new HashMap<>();
		Arrays.stream(config.getWebsiteConfig().getCategories())
			.forEach(c->countMap.put(c, 0));
		
		for (EntryData templateEntry : templateEntries) {
			countMap.merge(templateEntry.getCategory(), 1, (a,b)->a+b);
		}
		
		return countMap;
	}
	public int getGeneratedEntryCount() { return generatedEntries.size(); }
	public Map<String, Integer> getGeneratedEntryCountByCategory() {
		HashMap<String, Integer> countMap = new HashMap<>();
		Arrays.stream(config.getWebsiteConfig().getCategories())
			.forEach(c->countMap.put(c, 0));
		
		for (EntryData generatedEntry : generatedEntries.values()) {
			countMap.merge(generatedEntry.getCategory(), 1, (a,b)->a+b);
		}
		
		return countMap;
	}
	public int getUnreadGeneratedEntryCount() {
		return (int) generatedEntries.values().stream()
				.filter(entry->entry.getHalfStarRating()<0)
				.count();
	}
	public Map<String, Integer> getUnreadGeneratedEntryCountByCategory() {
		HashMap<String, Integer> countMap = new HashMap<>();
		Arrays.stream(config.getWebsiteConfig().getCategories())
			.forEach(c->countMap.put(c, 0));
		
		for (EntryData generatedEntry : generatedEntries.values()) {
			if (generatedEntry.getHalfStarRating()>=0) { continue; }
			countMap.merge(generatedEntry.getCategory(), 1, (a,b)->a+b);
		}
		
		return countMap;
	}

	public EntryData[] getTemplateEntriesSpecifiedCategoryFirst(String category, Random rng) {
		EntryData[] shuffledEntries = templateEntries.clone();
		Collections.shuffle(Arrays.asList(shuffledEntries));
		
		EntryData[] sortedEntries = new EntryData[templateEntries.length];
		int head = 0;
		int tail = sortedEntries.length-1;
		for (EntryData templateEntry : shuffledEntries) {
			if (templateEntry.getCategory().equals(category)) {
				sortedEntries[head] = templateEntry;
				head++;
			} else {
				sortedEntries[tail] = templateEntry;
				tail--;
			}
		}
		
		return sortedEntries;
	}
	public EntryData[] getGeneratedEntriesSortedByCategoryAndScore(String category) {
		EntryData[] sortedEntries = generatedEntries.values()
				.stream()
				.filter(entry->entry.getHalfStarRating()>0)
				.toArray(EntryData[]::new);
		
		Arrays.sort(sortedEntries, (a,b)->{
			int aScore = a.getHalfStarRating() + (a.getCategory().equals(category)?2:0);
			int bScore = b.getHalfStarRating() + (b.getCategory().equals(category)?2:0);
			
			return aScore - bScore;
		});
		
		return sortedEntries;
	}
}
