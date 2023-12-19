package aparmar.naisiteengine.templating;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.entry.EntryData;

public class EntryTemplateFieldProvider extends AbstractEntryTemplateProvider {
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
			TemplateParser.TemplateParsingContext parsingContext, EntryData entryData) {
		String entryFieldName = templateParams.get(ENTRY_FIELD_NAME_PARAM_KEY);
		if (entryFieldName == null) {
			return templateName+" parse error: missing 'name' parameter";
		}
		
		if (entryData.getFieldMap().containsKey(entryFieldName)) {
			return entryData.getFieldMap().get(entryFieldName);
		}
		if (SPECIAL_FIELDS_MAP.containsKey(entryFieldName)) {
			return SPECIAL_FIELDS_MAP.get(entryFieldName).apply(entryData);
		}
		return templateName+" parse error: '"+entryFieldName+"' is not a valid field name";
	}
}
