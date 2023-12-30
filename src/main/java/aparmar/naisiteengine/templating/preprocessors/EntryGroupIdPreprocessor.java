package aparmar.naisiteengine.templating.preprocessors;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ENTRY_ID;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_PAGINATION_START;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_TAGS;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.regex.Pattern;

import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.templating.TemplateParser.TemplateParsingContext;
import aparmar.naisiteengine.utils.NaiSiteEngineConstants;
import aparmar.naisiteengine.utils.NaiSiteEngineUtils;

public class EntryGroupIdPreprocessor implements ITemplatePreprocessor {
	public static final String ENTRY_ID_GENERATE = "generate";
	private static final Pattern GENERATE_ID_REGEX = Pattern.compile(
			NaiSiteEngineConstants.LAYER_PARAM_ENTRY_ID+":\\s*?"+ENTRY_ID_GENERATE);

	@Override
	public String processTemplate(String templateName, String templateHtml, TemplateParsingContext parsingContext) {
		String[] currentTags = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_TAGS))
				.map(de->de.toArray(new String[0]))
				.orElse(new String[] {});
		int startIndex = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_PAGINATION_START))
				.map(de->de.getFirst())
				.map(Integer::parseInt)
				.orElse(0);
		int entryId = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_ENTRY_ID))
				.map(Deque::getFirst)
				.map(Integer::parseInt)
				.orElse(EntryManager.INVALID_ENTRY_ID);
		
		LinkedList<Integer> orderedEntryIds = new LinkedList<>();
		Arrays.stream(parsingContext.getTemplateParser().getEntryManager().getGeneratedEntries())
			.filter(e->e.hasAllTags(currentTags))
			.mapToInt(EntryData::getId)
			.filter(id->id>=startIndex)
			.filter(id->id!=entryId)
			.sorted()
			.forEachOrdered(orderedEntryIds::add);
		
		templateHtml = NaiSiteEngineUtils.regexSpliceString(GENERATE_ID_REGEX, templateHtml, (match)->{
			if (orderedEntryIds.isEmpty()) { return match.group(); }
			
			return NaiSiteEngineConstants.LAYER_PARAM_ENTRY_ID+":"+orderedEntryIds.poll();
		});
		
		return templateHtml;
	}

}
