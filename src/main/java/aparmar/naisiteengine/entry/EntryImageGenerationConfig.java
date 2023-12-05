package aparmar.naisiteengine.entry;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

import lombok.Data;

@Data
public class EntryImageGenerationConfig {
	@JsonProperty(value="generation-prompt", required=true)
	private ImageGenerationPromptItem[] imageGenerationPromptItems;
	
	public String resolvePromptString(EntryData entryData) {
		return Arrays.stream(imageGenerationPromptItems)
				.map(i->i.resolvePromptItemString(entryData))
				.filter(t->!t.isEmpty())
				.collect(Collectors.joining(", "));
	}

	@Data
	public static class ImageGenerationPromptItem {
		@JsonProperty(value="text-static", required=false)
		private String defaultText = "";
		@JsonProperty(value="text-field", required=false)
		private String referenceTextField;
		@JsonProperty(value="text-filter-regex", required=false)
		private String referenceTextFieldRegexFilter;
		@JsonProperty(value="strip-commas", required=false)
		private boolean stripCommas = false;
		@JsonProperty(value="strengthening-levels", required=false)
		private int strengtheningLevels = 0;
		
		public String resolvePromptItemString(EntryData entryData) {
			String result = "";
			
			if (referenceTextField != null 
					&& entryData.getFieldMap().containsKey(referenceTextField)
					&& entryData.getFieldMap().get(referenceTextField).trim().length()>0) {
				result = entryData.getFieldMap().get(referenceTextField).trim();
				
				if (referenceTextFieldRegexFilter != null) {
					Matcher fieldMatcher = Pattern.compile(referenceTextFieldRegexFilter).matcher(result);
					if (fieldMatcher.find()) {
						if (fieldMatcher.groupCount()>0) {
							result = fieldMatcher.group(1);
						} else {
							result = fieldMatcher.group();
						}
					} else {
						result = "";
					}
				}
				
				if (stripCommas) { result = result.replaceAll(",", ""); }
				result = result.trim();
			}
			
			if (result.isEmpty()) { result = defaultText; }
			
			String prefix = "";
			String suffix = "";
			if (strengtheningLevels>0) {
				prefix = Strings.repeat("{", strengtheningLevels);
				suffix = Strings.repeat("}", strengtheningLevels);
			} else if (strengtheningLevels<0) {
				prefix = Strings.repeat("[", -strengtheningLevels);
				suffix = Strings.repeat("]", -strengtheningLevels);
			}
			
			return prefix+result+suffix;
		}
	}
}
