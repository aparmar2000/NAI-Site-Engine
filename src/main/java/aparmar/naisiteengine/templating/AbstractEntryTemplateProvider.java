package aparmar.naisiteengine.templating;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ENTRY_ID;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;

import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.templating.TemplateParser.TemplateParsingContext;
import aparmar.naisiteengine.utils.NaiSiteEngineConstants;

public abstract class AbstractEntryTemplateProvider implements ISpecialTemplateProvider {

	@Override
	public String provideReplacementString(String templateName, Map<String, String> templateParams,
			TemplateParsingContext parsingContext) {
		EntryManager entryManager = parsingContext.getTemplateParser().getEntryManager();
		
		String entryIdParam = parsingContext.getLayerParameters()
				.get(NaiSiteEngineConstants.LAYER_PARAM_ENTRY_ID);
		int entryIdQueryParam = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_ENTRY_ID))
				.map(Deque::getFirst)
				.map(Integer::parseInt)
				.orElse(0);
		int entryId = 1;
		if (entryIdParam!=null && entryIdParam.matches("^\\d+$")) {
			entryId = Integer.parseUnsignedInt(entryIdParam);
		} else if (entryIdQueryParam!=0) {
			entryId = entryIdQueryParam;
		} else {
			return templateName+" parse error: invalid entry-id";
		}

		EntryData entryData = entryManager.getEntryById(entryId);
		if (entryData == null) {
			return templateName+" parse error: unknown entry-id '"+entryId+"'";
		}
		return provideReplacementString(templateName, templateParams, parsingContext, entryData);
	}
	
	protected abstract String provideReplacementString(String templateName, Map<String, String> templateParams,
			TemplateParsingContext parsingContext, EntryData entryData);

}
