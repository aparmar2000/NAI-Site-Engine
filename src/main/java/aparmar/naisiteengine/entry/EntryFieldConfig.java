package aparmar.naisiteengine.entry;

import java.util.regex.Matcher;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class EntryFieldConfig {
	public enum EntryFieldType {
		@JsonProperty("text")
		TEXT,
		@JsonProperty("image")
		IMAGE;
	}
	
	@JsonProperty(required=true)
	private String name;
	@JsonProperty(required=true)
	private EntryFieldType type;
	@JsonProperty(value="source", required=true)
	FieldValueConfig fieldValueConfig;

	@Data
	public static class FieldValueConfig {
		@JsonProperty(value="default-value", required=false)
		private String defaultValue = "";
		@JsonProperty(value="generation-index", required=false)
		private int generationConfigIndex = -1;
		@JsonProperty(value="generation-group", required=false)
		private int generationRegexGroup = 0;
		
		public String getTextValueFromRegexMatch(Matcher match) {
			String value = match.group(generationRegexGroup);
			value = value==null?"":value;
			return value;
		}
	}
	
	public String getTextValueFromRegexMatch(Matcher match) { return fieldValueConfig.getTextValueFromRegexMatch(match); }
}
