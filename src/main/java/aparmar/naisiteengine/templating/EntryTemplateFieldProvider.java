package aparmar.naisiteengine.templating;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ENTRY_ID;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.utils.NaiSiteEngineConstants;

public class EntryTemplateFieldProvider implements ISpecialTemplateProvider {
	public static final String ENTRY_FIELD_SPECIAL_KEY = "entry-field";
	
	private static final String ENTRY_FIELD_NAME_PARAM_KEY = "name";
	
	private static final Map<String, Function<EntryData,String>> SPECIAL_FIELDS_MAP = ImmutableMap.of(
			"id", e->Integer.toString(e.getId()),
			"rating", e->Integer.toString(e.getRating())
			);
	
	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(ENTRY_FIELD_SPECIAL_KEY);
	}
	
	@Override
	public String provideReplacementString(String templateName, Map<String, String> templateParams,
			TemplateParser.TemplateParsingContext parsingContext) {
		EntryManager entryManager = parsingContext.getTemplateParser().getEntryManager();

		String entryFieldName = templateParams.get(ENTRY_FIELD_NAME_PARAM_KEY);
		if (entryFieldName == null) {
			return templateName+" parse error: missing 'name' parameter";
		}
		
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
		if (entryData.getFieldMap().containsKey(entryFieldName)) {
			return entryData.getFieldMap().get(entryFieldName);
		}
		if (SPECIAL_FIELDS_MAP.containsKey(entryFieldName)) {
			return SPECIAL_FIELDS_MAP.get(entryFieldName).apply(entryData);
		}
		return templateName+" parse error: '"+entryFieldName+"' is not a valid field name";
	}
}
