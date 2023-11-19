package aparmar.naisiteengine.config;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class WebsiteConfiguration {
	private final static String CATEGORIES_KEY = "categories";
	private final String[] categories;
	
	public WebsiteConfiguration() {
		categories = new String[] {"News", "Local", "Politics", "Entertainment", "Sports"};
	}

	@SuppressWarnings("unchecked")
	public WebsiteConfiguration(Map<String, Object> websiteSettingsData) {
		WebsiteConfiguration defaultSiteConfig = new WebsiteConfiguration();
		
		if (websiteSettingsData.containsKey(CATEGORIES_KEY)) {
			categories = ((List<String>) websiteSettingsData.get(CATEGORIES_KEY)).toArray(new String[0]);
		} else {
			categories = defaultSiteConfig.getCategories();
		}
	}
}
