package aparmar.naisiteengine.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import aparmar.nai.data.request.TextGenModel;
import lombok.Data;

@Data
public class GenerationConfiguration {
	@JsonProperty("novelai-api-key")
	private final String apiKey;
	@JsonProperty("target-cache-size")
	private final int targetCacheSize;
	@JsonProperty("model")
	private final TextGenModel model;
	@JsonProperty("image-generation")
	private final boolean imageGenerationEnabled;
	
	public GenerationConfiguration() {
		apiKey = "";
		targetCacheSize = 16;
		model = TextGenModel.KAYRA;
		imageGenerationEnabled = true;
	}
	
//	public String getMemoryText() {
//		StringBuilder memoryStringBuilder = new StringBuilder();
//		
//		if (tags.length>0 || genres.length>0) {
//			memoryStringBuilder.append("[ ");
//			
//			if (tags.length>0) {
//				memoryStringBuilder.append("Tags: ");
//				memoryStringBuilder.append(Arrays.stream(tags).collect(Collectors.joining(", ")));
//				if (genres.length>0) { memoryStringBuilder.append("; "); }
//			}
//			if (genres.length>0) {
//				memoryStringBuilder.append("Genre: ");
//				memoryStringBuilder.append(Arrays.stream(genres).collect(Collectors.joining(", ")));
//			}
//			
//			memoryStringBuilder.append(" ]");
//			if (styles.length>0) { memoryStringBuilder.append("\n"); }
//		}
//		if (styles.length>0) {
//			memoryStringBuilder.append("[ Style: ");
//			memoryStringBuilder.append(Arrays.stream(styles).collect(Collectors.joining(", ")));
//			memoryStringBuilder.append(" ]");
//		}
//		
//		return memoryStringBuilder.toString();
//	}
}
