package aparmar.naisiteengine.templating.providers;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ENTRY_ID;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.templating.TemplateParser.TemplateParsingContext;
import aparmar.naisiteengine.utils.NaiSiteEngineConstants;

public class StarRatingProvider implements ISpecialTemplateProvider {
	public static final String RATING_TEMPLATE_KEY = "entry-rating";

	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(RATING_TEMPLATE_KEY);
	}

	@Override
	public String provideReplacementString(String templateName, Map<String, String> templateParams, TemplateParsingContext parsingContext) {
		EntryManager entryManager = parsingContext.getTemplateParser().getEntryManager();

		String entryIdParam = parsingContext.getLayerParameters()
				.get(NaiSiteEngineConstants.LAYER_PARAM_ENTRY_ID);
		int entryIdQueryParam = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_ENTRY_ID))
				.map(Deque::getFirst)
				.map(Integer::parseInt)
				.orElse(-1);
		int entryId = 0;
		if (entryIdParam!=null && entryIdParam.matches("^\\d+$")) {
			entryId = Integer.parseUnsignedInt(entryIdParam);
		} else if (entryIdQueryParam>=0) {
			entryId = entryIdQueryParam;
		}
		
		EntryData currentArticle = entryManager.getGeneratedEntryById(entryId);
		if (currentArticle == null) { return "ERR: unknown entry id"; }
		return Integer.toString(currentArticle.getRating());
	}

}
