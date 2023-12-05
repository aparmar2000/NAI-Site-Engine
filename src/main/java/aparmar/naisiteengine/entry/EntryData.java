package aparmar.naisiteengine.entry;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ENTRY_ID;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import aparmar.naisiteengine.config.SiteConfigManager;
import aparmar.naisiteengine.entry.EntryFieldConfig.EntryFieldType;
import aparmar.naisiteengine.templating.TemplateParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntryData {
	@JsonProperty(value="entry-type", required=true)
	private String entryTypeName;
	@Setter
	private transient SiteConfigManager siteConfigManager;
	private transient EntryType entryType;
	@JsonProperty(required=false)
	private int id;
	@JsonProperty(value="creation-timestamp", required=false)
	private LocalDateTime creationDateTime;
	@JsonProperty(value="creation-timestamp", required=false)
	private int rating;

	@JsonProperty(value="fields", required=true)
	private Map<String, String> fieldMap;
	@JsonProperty(value="tags", required=true)
	private String[] tags;
	
	public String mergedContextString(TemplateParser templateParser, int textGenerationConfigIndex) {
		return mergedContextString(templateParser, getEntryType().getTextGenerationConfigs()[textGenerationConfigIndex]);
	}
	public String mergedContextString(TemplateParser templateParser, EntryTextGenerationConfig textGenerationConfig) {
		String entryContextText = siteConfigManager.getTagGroupManager().tagStringsToTextGenerationContextForm(tags);
		entryContextText += "\n";
		entryContextText += templateParser.parseHTML(
				textGenerationConfig.getGenerationContext().getEntryTemplateString(), 
				".", 
				new HashMap<>(Collections.singletonMap(QUERY_PARAM_ENTRY_ID, new LinkedList<>(Collections.singletonList(Integer.toString(id))))));
		return entryContextText;
	}
	
	public String imagePrompt(int imageGenerationConfigIndex) {
		return getEntryType()
				.getImageGenerationConfigs()[imageGenerationConfigIndex]
				.resolvePromptString(this);
	}
	
	public boolean hasImagesToGenerate() {
		EntryType entryType = getEntryType();
		if (entryType == null) { return false; }
		
		Arrays.stream(entryType.getEntryFieldConfigs())
			.filter(f->f.getType() == EntryFieldType.IMAGE)
			.filter(f->!fieldMap.containsKey(f.getName()))
			.map(field->field.getFieldValueConfig());
		
		return true;
	}

	private static final int FILENAME_LENGTH_LIMIT = 25;
	public String getEntryFilename() {
		String[] splitTitle = fieldMap.get(getEntryType().getFilenameField())
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
		
		return filename + (id % 100);
	}
	
	public EntryType getEntryType() {
		if (entryType == null) {
			synchronized (entryType) {
				if (entryType == null) {
					if (siteConfigManager == null) { return null;}
					entryType = siteConfigManager.getEntryTypeManager().getEntryTypeByName(entryTypeName);
				}
			}
		}
		
		return entryType;
	}
	
	public String getField(String fieldName) { return fieldMap.get(fieldName); }	
	public String getField(EntryFieldConfig fieldConfig) { return getField(fieldConfig.getName()); }
	
	public void setField(String fieldName, String newVal) { fieldMap.put(fieldName, newVal); }	
	public void setField(EntryFieldConfig fieldConfig, String newVal) { setField(fieldConfig.getName(), newVal); }
}
