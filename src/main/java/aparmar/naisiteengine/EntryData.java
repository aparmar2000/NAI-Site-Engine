package aparmar.naisiteengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntryData {
	private static final String ARTICLE_PARSE_REGEX = "\\[ News Category: (.+?) ]\\n(.+?)\\n(.+)\\n\\*\\*\\*";
	private static final Pattern ARTICLE_PARSE_PATTERN = Pattern.compile(ARTICLE_PARSE_REGEX, Pattern.MULTILINE | Pattern.DOTALL);

	private static final DateTimeFormatter CREATION_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	
	private static final String ID_INTEGER_KEY = "id";
	private static final String CREATION_DATE_KEY = "creation-date";
	private static final String IMAGE_FILENAME_STRING_KEY = "img-filename";
	private static final String INSTRUCT_STRING_KEY = "instruct-prompt";
	private static final String TITLE_STRING_KEY = "title";
	private static final String CATEGORY_STRING_KEY = "category";
	private static final String BODY_STRING_KEY = "body";
	private static final String RATING_INTEGER_KEY = "rating";
	
	private int id;
	private LocalDateTime creationDateTime;
	private String imgFilename;
	private String instructString;
	private String title;
	private String category;
	private String entryBody;
	private int halfStarRating;
	
	public String mergedText() {
		return "[ News Category: "+category+" ]\n"+title+"\n"+entryBody+"\n***";
	}
	public String mergedTextInstruct() {
		return "{ "+instructString+" }\n"+mergedText();
	}
	
	private static final int MAX_SAMPLE_LEN = 500;
	public String imagePrompt() {
		String sample = "";
		for (String sampleSentence : entryBody.replaceFirst("^.+—", "").split("\\.")) {
			String mergedSample = sample+sampleSentence+".";
			if (mergedSample.length()<=MAX_SAMPLE_LEN) {
				sample = mergedSample;
			}
		}
		return sample + ", stock photo, realistic, news, entry, ";
	}

	private static final int FILENAME_LENGTH_LIMIT = 25;
	public String getEntryFilename() {
		String[] splitTitle = title
				.replaceAll("[^A-Za-z ]", "")
				.replaceAll(" {2,}", " ")
				.split(" ");
		String filename = "";
		for (String titleChunk : splitTitle) {
			if (filename.length()+titleChunk.length()<=FILENAME_LENGTH_LIMIT) {
				filename = filename + titleChunk;
			} else {
				break;
			}
		}
		
		return filename;
	}
	
	public static EntryData loadFromString(String inString) {
		Matcher matcher = ARTICLE_PARSE_PATTERN.matcher(inString);
		if (!matcher.find()) { return null; }
		
		int id = -1;
		LocalDateTime creationDateTime = LocalDateTime.now();
		String imgFilename = "";
		String instructString = "{ Write a satirical news entry }";
		String title = matcher.group(2);
		String category = matcher.group(1);
		String entryBody = matcher.group(3);
		int halfStarRating = -10;
		
		return new EntryData(
				id, 
				creationDateTime, 
				imgFilename,
				instructString, 
				title, 
				category, 
				entryBody, 
				halfStarRating);
	}
	
	public static EntryData loadFromFile(File inFile) throws FileNotFoundException, IOException {
		int id = -1;
		LocalDateTime creationDateTime = LocalDateTime.of(2023, Month.JULY, 28, 10, 53);
		String imgFilename = "";
		String instructString = "{ Write a satirical news entry }";
		String title = null;
		String category = "";
		String entryBody = null;
		int halfStarRating = 5;
		
		Load yamlReader = new Load(LoadSettings.builder().build());
		try (FileInputStream in = new FileInputStream(inFile)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> yamlData = (Map<String, Object>) yamlReader.loadFromInputStream(in);

			if (yamlData.containsKey(ID_INTEGER_KEY)) {
				id = (Integer) yamlData.get(ID_INTEGER_KEY);
			}
			if (yamlData.containsKey(CREATION_DATE_KEY)) {
				creationDateTime = LocalDateTime.from(CREATION_DATE_FORMATTER.parse((String) yamlData.get(CREATION_DATE_KEY)));
			}
			if (yamlData.containsKey(IMAGE_FILENAME_STRING_KEY)) {
				imgFilename = (String) yamlData.get(IMAGE_FILENAME_STRING_KEY);
			}
			if (yamlData.containsKey(INSTRUCT_STRING_KEY)) {
				instructString = (String) yamlData.get(INSTRUCT_STRING_KEY);
			}
			if (yamlData.containsKey(TITLE_STRING_KEY)) {
				title = (String) yamlData.get(TITLE_STRING_KEY);
			}
			if (yamlData.containsKey(CATEGORY_STRING_KEY)) {
				category = (String) yamlData.get(CATEGORY_STRING_KEY);
			}
			if (yamlData.containsKey(BODY_STRING_KEY)) {
				entryBody = (String) yamlData.get(BODY_STRING_KEY);
			}
			if (yamlData.containsKey(RATING_INTEGER_KEY)) {
				halfStarRating = (Integer) yamlData.get(RATING_INTEGER_KEY);
			}
		}
		if (title==null) { throw new IOException(inFile+" has no recognized title."); }
		if (entryBody==null) { throw new IOException(inFile+" has no recognized body."); }
		
		return new EntryData(
				id,
				creationDateTime,
				imgFilename,
				instructString,
				title,
				category,
				entryBody,
				halfStarRating);
	}
	
	public void saveToFile(File outFile) throws FileNotFoundException, IOException {
		Dump yamlWriter = new Dump(DumpSettings.builder().build());
		
		HashMap<String, Object> dataMap = new HashMap<>();
		dataMap.put(ID_INTEGER_KEY, getId());
		dataMap.put(CREATION_DATE_KEY, CREATION_DATE_FORMATTER.format(getCreationDateTime()));
		dataMap.put(IMAGE_FILENAME_STRING_KEY, imgFilename);
		dataMap.put(INSTRUCT_STRING_KEY, getInstructString());
		dataMap.put(TITLE_STRING_KEY, getTitle());
		dataMap.put(CATEGORY_STRING_KEY, getCategory());
		dataMap.put(BODY_STRING_KEY, getEntryBody());
		dataMap.put(RATING_INTEGER_KEY, getHalfStarRating());

		String dataString = yamlWriter.dumpToString(dataMap);
		try(FileWriter writer = new FileWriter(outFile)) {
			writer.write(dataString);
		}
	}
}
