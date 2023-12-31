package aparmar.naisiteengine.templating.providers;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_PAGINATION_START;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_TAGS;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.templating.TemplateParser.TemplateParsingContext;
import aparmar.naisiteengine.utils.LinkBuilder;

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
		String[] currentTags = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_TAGS))
				.map(de->de.toArray(new String[0]))
				.orElse(new String[] {});
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
		
		LinkBuilder lb = new LinkBuilder("category-paginate.html");
		lb.addMultiValueParam(QUERY_PARAM_TAGS, currentTags);
		lb.addParam(QUERY_PARAM_PAGINATION_START, startIndex);
		
		return "<a href=\""+lb.build()+"\" class=\"pagination-link\">"
			+ paginationLinkText
			+ "</a>";
	}

}
