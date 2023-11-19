package aparmar.naisiteengine.templating;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_CATEGORY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.config.UserConfiguration;
import lombok.AllArgsConstructor;
import lombok.Data;

public class CategoryTemplateProvider implements ISpecialTemplateProvider {
	private static final String CATEGORIES_SPECIAL_KEY = "categories";

	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(CATEGORIES_SPECIAL_KEY);
	}
	
	@Data
	@AllArgsConstructor
	private static class CategoryData {
		private final String label, anchorData;
		private final boolean active;
		
		public CategoryData(String name, String currentCategory) {
			label = name;
			anchorData = "href=\"category.html?"+QUERY_PARAM_CATEGORY+"="+name+"\"";
			active = currentCategory.equals(name);
		}
	}
	
	private String assembleCategoryHTML(UserConfiguration config,
			String currentRelativePath, Map<String, Deque<String>> queryParameters) {
		String currentCategory = Optional.ofNullable(queryParameters.get(QUERY_PARAM_CATEGORY)).map(de->de.getFirst()).orElse("");
		
		List<CategoryData> categories = new ArrayList<>();
		categories.add(new CategoryData("Home", "href=\"index.html\"", currentRelativePath.equals("/index.html")));
		Arrays.stream(config.getWebsiteConfig().getCategories())
			.map(c->new CategoryData(c, currentCategory))
			.forEachOrdered(categories::add);
		
		return categories.stream()
			.map(c->"<li"+(c.isActive()?" class=\"active\"":"")+"><a "+c.getAnchorData()+">"+c.getLabel()+"</a></li>")
			.collect(Collectors.joining("\n"));
	}

	@Override
	public String provideReplacementString(String templateName, Map<String,String> templateParams,
			TemplateParser.TemplateParsingContext parsingContext) {
		return assembleCategoryHTML(
				parsingContext.getTemplateParser().getConfig(), 
				parsingContext.getCurrentRelativePath(), 
				parsingContext.getQueryParameters());
	}

}
