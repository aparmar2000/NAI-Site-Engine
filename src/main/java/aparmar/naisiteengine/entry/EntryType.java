package aparmar.naisiteengine.entry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import aparmar.naisiteengine.entry.EntryFieldConfig.EntryFieldType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EntryType {
	@JsonProperty(required=true)
	private String name;
	@JsonProperty(value="filename-field", required=true)
	private String filenameField;
	@JsonProperty(value="text-generation", required=true)
	private EntryTextGenerationConfig[] textGenerationConfigs;
	@JsonProperty(value="image-generation", required=false)
	private EntryImageGenerationConfig[] imageGenerationConfigs;
	@JsonProperty(value="fields", required=true)
	private EntryFieldConfig[] entryFieldConfigs;
	@JsonProperty(value="tag-groups", required=false)
	private String[] tagGroupNames;

	public Map<String, String> populateFieldMapWithDefaults() {
		return Arrays.stream(entryFieldConfigs)
				.collect(Collectors.toMap(
						EntryFieldConfig::getName, 
						f->f.getFieldValueConfig().getDefaultValue(), 
						(a,b)->a, 
						HashMap<String,String>::new));
	}
	public boolean populateFieldMapFromTextGen(EntryData entryData, int textGenerationConfigIndex, String inString) {
		Matcher contentMatcher = textGenerationConfigs[textGenerationConfigIndex].getContentMatcher(inString);
		if (!contentMatcher.find()) { return false; }
		
		Arrays.stream(entryFieldConfigs)
			.filter(f->f.getType() == EntryFieldType.TEXT)
			.filter(f->f.getFieldValueConfig().getGenerationConfigIndex()==textGenerationConfigIndex)
			.filter(f->!f.getTextValueFromRegexMatch(contentMatcher).isEmpty())
			.forEach(f->entryData.setField(f, f.getTextValueFromRegexMatch(contentMatcher)));
		
		return true;
	}
}
