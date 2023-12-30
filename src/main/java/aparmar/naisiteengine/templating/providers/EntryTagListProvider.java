package aparmar.naisiteengine.templating.providers;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_TAGS;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.templating.TemplateParser;

public class EntryTagListProvider extends AbstractEntryTemplateProvider {
	public static final String ENTRY_TAGS_SPECIAL_KEY = "entry-tags";
	
	private static final String PAGE_PROPERTY_KEY = "target-page";
	
	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(ENTRY_TAGS_SPECIAL_KEY);
	}
	
	@Override
	public String provideReplacementString(String templateName, Map<String, String> templateParams,
			TemplateParser.TemplateParsingContext parsingContext, EntryData entryData) {
		String targetPage = templateParams.get(PAGE_PROPERTY_KEY);
		if (targetPage == null) {
			return templateName+" parse error: missing 'target-page' parameter";
		}
		
		return Arrays.stream(entryData.getTags())
				.map(t->"<li><a href=\""+(targetPage+"?"+QUERY_PARAM_TAGS+"="+t)+"\">"+t+"</a></li>")
				.collect(Collectors.joining("\n"));
	}
}
