package aparmar.naisiteengine.templating;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_TAGS;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.templating.TemplateParser.TemplateParsingContext;

public class CategoryNameProvider implements ISpecialTemplateProvider {
	private static final String CATEGORY_NAME_SPECIAL_KEY = "category-name";

	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(CATEGORY_NAME_SPECIAL_KEY);
	}

	@Override
	public String provideReplacementString(String templateName, Map<String, String> templateParams,
			TemplateParsingContext parsingContext) {
		String currentCategory = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_TAGS))
				.map(de->de.getFirst())
				.map(c->c.substring(0, 1).toUpperCase()+c.substring(1))
				.orElse("Unknown");
		return currentCategory;
	}

}
