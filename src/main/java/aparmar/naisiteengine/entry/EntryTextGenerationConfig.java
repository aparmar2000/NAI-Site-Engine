package aparmar.naisiteengine.entry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class EntryTextGenerationConfig {
	@JsonProperty(value="generation-context", required=true)
	private TextGenerationContextConfig generationContext;
	@JsonProperty(value="content-regex", required=true)
	private String contentRegex;
	@JsonProperty(value="target-min-length", required=false)
	private int targetMinLength = -1;
	@JsonProperty(value="target-max-length", required=false)
	private int targetMaxLength = -1;
	@JsonProperty(value="ending-bias", required=false)
	private String endingBias;

	@Data
	public static class TextGenerationContextConfig {
		@JsonProperty
		private String[] tags;
		@JsonProperty
		private String[] genres;
		@JsonProperty
		private String[] styles;
		@JsonProperty(value="max-examples", required=false)
		private int maxExamples = -1;
		@JsonProperty(value="entry-string", required=true)
		private String entryTemplateString;
	}
	
	public Matcher getContentMatcher(String text) {
		return Pattern.compile(contentRegex).matcher(text);
	}
}
