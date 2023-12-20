package aparmar.naisiteengine.templating;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.entry.EntryData;

public class EntryTemplateFieldProvider extends AbstractEntryTemplateProvider {
	public static final String ENTRY_FIELD_SPECIAL_KEY = "entry-field";
	
	private static final String ENTRY_FIELD_NAME_PARAM_KEY = "name";
	private static final String FIELD_LENGTH_LIMIT_PARAM_KEY = "max-len";
	
	private static final String FIELD_TRUNC_METHOC_PARAM_KEY = "trunc-method";
	private static final String TRUNC_METHOD_CHARACTER = "CHAR";
	private static final String TRUNC_METHOD_WORD = "WORD";
	
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
		
		int maxFieldLength = Optional.ofNullable(templateParams.get(FIELD_LENGTH_LIMIT_PARAM_KEY))
				.map(Integer::parseInt)
				.orElse(Integer.MAX_VALUE);
		String truncStrategy = Optional.ofNullable(templateParams.get(FIELD_TRUNC_METHOC_PARAM_KEY))
				.map(String::toUpperCase)
				.orElse(TRUNC_METHOD_WORD);
		if (!truncStrategy.equals(TRUNC_METHOD_CHARACTER) && !truncStrategy.equals(TRUNC_METHOD_WORD)) {
			return templateName+" parse error: unknown trunc-method '"+truncStrategy+"'";
		}
		
		if (entryData.getFieldMap().containsKey(entryFieldName)) {
			return truncateFieldValue(entryData.getFieldMap().get(entryFieldName), maxFieldLength, truncStrategy);
		}
		if (SPECIAL_FIELDS_MAP.containsKey(entryFieldName)) {
			return truncateFieldValue(SPECIAL_FIELDS_MAP.get(entryFieldName).apply(entryData), maxFieldLength, truncStrategy);
		}
		return templateName+" parse error: '"+entryFieldName+"' is not a valid field name";
	}
	
	private String truncateFieldValue(String value, int maxLength, String truncStrategy) {
		if (value.length()<=maxLength) {
			return value;
		}
		
		switch (truncStrategy) {
		case TRUNC_METHOD_CHARACTER:
			return value.substring(0, maxLength-3)+"...";
		default:
		case TRUNC_METHOD_WORD:
			String result = "";
			for (String section : value.split(" ")) {
				String nextResult = result+" "+section;
				if (nextResult.length()>maxLength-3) {
					return result+"...";
				}
				result = nextResult;
			}
			break;
		}
		
		return "err: String truncation error";
	}
}
