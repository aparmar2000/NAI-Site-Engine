package aparmar.naisiteengine.templating;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_TAGS;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_PAGINATION_START;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.templating.TemplateParser.TemplateParsingContext;

public class CategoryPaginationProvider implements ISpecialTemplateProvider {
	public static final String CATEGORY_PAGINATE_SPECIAL_KEY = "category-paginate";
	
	private static final String PAGINATION_DIRECTION_PROPERTY_KEY = "direction";
	private static final String PAGINATION_DIRECTION_FORWARD = "forward";
	private static final String PAGINATION_DIRECTION_BACKWARD = "backward";
	private static final String PAGINATION_FORWARD_LABEL = ">>";
	private static final String PAGINATION_BACKWARD_LABEL = "<<";
	
	private static final String PAGINATION_START_PROPERTY_KEY = "start";
	private static final String PAGINATION_LABEL_PROPERTY_KEY = "label";
	
	private static final int PAGINATION_DELTA_SIZE = 20;

	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(CATEGORY_PAGINATE_SPECIAL_KEY);
	}

	@Override
	public String provideReplacementString(String templateName, Map<String, String> templateParams,
			TemplateParsingContext parsingContext) {
		String currentCategory = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_TAGS))
				.map(de->de.getFirst())
				.orElse("all");
		int baseStartIndex = Optional.ofNullable(templateParams.get(PAGINATION_START_PROPERTY_KEY))
				.map(Integer::parseInt)
				.orElse(0);
		int startIndex = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_PAGINATION_START))
				.map(de->de.getFirst())
				.map(Integer::parseInt)
				.orElse(baseStartIndex-PAGINATION_DELTA_SIZE);
		
		String paginationLinkText = "";
		String paginationDirection = templateParams.get(PAGINATION_DIRECTION_PROPERTY_KEY);
		if (paginationDirection.equals(PAGINATION_DIRECTION_FORWARD)) {
			startIndex += PAGINATION_DELTA_SIZE;
			paginationLinkText = PAGINATION_FORWARD_LABEL;
		} else if (paginationDirection.equals(PAGINATION_DIRECTION_BACKWARD)) {
			startIndex -= PAGINATION_DELTA_SIZE;
			paginationLinkText = PAGINATION_BACKWARD_LABEL;
		} else {
			return "ERR: invalid pagination direction";
		}
		if (templateParams.containsKey(PAGINATION_LABEL_PROPERTY_KEY)) {
			paginationLinkText = templateParams.get(PAGINATION_LABEL_PROPERTY_KEY);
		}
		
		if (startIndex < 0) {
			return "<a disabled>"+paginationLinkText+"</a>";
		}
		return "<a href=\"category-paginate.html?"+QUERY_PARAM_TAGS+"="+currentCategory+"&"+QUERY_PARAM_PAGINATION_START+"="+startIndex+"\""
				+ "class=\"pagination-link\">"
			+ paginationLinkText
			+ "</a>";
	}

}
