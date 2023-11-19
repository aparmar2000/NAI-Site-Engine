package aparmar.naisiteengine.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import aparmar.nai.data.request.TextGenModel;
import lombok.Data;

@Data
public class GenerationConfiguration {
	private final static String API_KEY_KEY = "novelai-api-key";
	private final String apiKey;
	private final static String CACHE_SIZE_KEY = "target-cache-size";
	private final int targetCacheSize;
	private final static String MODEL_KEY = "model";
	private final TextGenModel model;
	private final static String IMAGE_GENERATION_ENABLE_KEY = "image-generation";
	private final boolean imageGenerationEnabled;
	private final static String CONTEXT_SETTINGS_KEY = "text-context";
	private final static String TAGS_KEY = "tags";
	private final String[] tags;
	private final static String GENRES_KEY = "genres";
	private final String[] genres;
	private final static String STYLES_KEY = "styles";
	private final String[] styles;
	private final static String CATEGORY_DESCRIPTOR_KEY = "category-descriptor";
	private final String categoryDescriptor;
	
	public GenerationConfiguration() {
		apiKey = "";
		targetCacheSize = 16;
		model = TextGenModel.KAYRA;
		imageGenerationEnabled = true;
		tags = new String[] {"satire", "article", "news"};
		genres = new String[] {"Article"};
		styles = new String[] {"formal", "advanced", "professional"};
		categoryDescriptor = "News Category";
	}

	@SuppressWarnings("unchecked")
	public GenerationConfiguration(Map<String, Object> generationSettingsData) {
		GenerationConfiguration defaultGenConfig = new GenerationConfiguration();

		if (generationSettingsData.containsKey(API_KEY_KEY)) {
			apiKey = (String) generationSettingsData.get(API_KEY_KEY);
		} else {
			apiKey = defaultGenConfig.getApiKey();
		}
		
		if (generationSettingsData.containsKey(CACHE_SIZE_KEY)) {
			targetCacheSize = (Integer) generationSettingsData.get(CACHE_SIZE_KEY);
		} else {
			targetCacheSize = defaultGenConfig.getTargetCacheSize();
		}
		
		if (generationSettingsData.containsKey(MODEL_KEY)) {
			String selectedModelString = (String) generationSettingsData.get(MODEL_KEY);
			TextGenModel selectedModel = defaultGenConfig.getModel();
			for (TextGenModel modelOption : TextGenModel.values()) {
				if (modelOption.toString().equalsIgnoreCase(selectedModelString)) {
					selectedModel = modelOption;
					break;
				}
			}
			
			model = selectedModel;
		} else {
			model = defaultGenConfig.getModel();
		}
		
		if (generationSettingsData.containsKey(IMAGE_GENERATION_ENABLE_KEY)) {
			imageGenerationEnabled = (Boolean) generationSettingsData.get(IMAGE_GENERATION_ENABLE_KEY);
		} else {
			imageGenerationEnabled = defaultGenConfig.isImageGenerationEnabled();
		}
		
		Map<String, Object> textContextSettingsData = (Map<String, Object>) generationSettingsData.get(CONTEXT_SETTINGS_KEY);
		if (textContextSettingsData!= null && textContextSettingsData.containsKey(TAGS_KEY)) {
			tags = ((List<String>) textContextSettingsData.get(TAGS_KEY)).toArray(new String[0]);
		} else {
			tags = defaultGenConfig.getTags();
		}
		
		if (textContextSettingsData!= null && textContextSettingsData.containsKey(GENRES_KEY)) {
			genres = ((List<String>) textContextSettingsData.get(GENRES_KEY)).toArray(new String[0]);
		} else {
			genres = defaultGenConfig.getGenres();
		}
		
		if (textContextSettingsData!= null && textContextSettingsData.containsKey(STYLES_KEY)) {
			styles = ((List<String>) textContextSettingsData.get(STYLES_KEY)).toArray(new String[0]);
		} else {
			styles = defaultGenConfig.getStyles();
		}
		
		if (textContextSettingsData!= null && textContextSettingsData.containsKey(CATEGORY_DESCRIPTOR_KEY)) {
			categoryDescriptor = (String) textContextSettingsData.get(CATEGORY_DESCRIPTOR_KEY);
		} else {
			categoryDescriptor = defaultGenConfig.getCategoryDescriptor();
		}
	}
	
	public String getMemoryText() {
		StringBuilder memoryStringBuilder = new StringBuilder();
		
		if (tags.length>0 || genres.length>0) {
			memoryStringBuilder.append("[ ");
			
			if (tags.length>0) {
				memoryStringBuilder.append("Tags: ");
				memoryStringBuilder.append(Arrays.stream(tags).collect(Collectors.joining(", ")));
				if (genres.length>0) { memoryStringBuilder.append("; "); }
			}
			if (genres.length>0) {
				memoryStringBuilder.append("Genre: ");
				memoryStringBuilder.append(Arrays.stream(genres).collect(Collectors.joining(", ")));
			}
			
			memoryStringBuilder.append(" ]");
			if (styles.length>0) { memoryStringBuilder.append("\n"); }
		}
		if (styles.length>0) {
			memoryStringBuilder.append("[ Style: ");
			memoryStringBuilder.append(Arrays.stream(styles).collect(Collectors.joining(", ")));
			memoryStringBuilder.append(" ]");
		}
		
		return memoryStringBuilder.toString();
	}
}
