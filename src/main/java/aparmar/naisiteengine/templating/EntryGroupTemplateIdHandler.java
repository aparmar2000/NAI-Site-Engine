package aparmar.naisiteengine.templating;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ENTRY_ID;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_TAGS;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_PAGINATION_START;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.templating.TemplateParser.TemplateParsingContext;
import aparmar.naisiteengine.utils.NaiSiteEngineConstants;
import aparmar.naisiteengine.utils.NaiSiteEngineUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntryGroupTemplateIdHandler implements ITemplateHandler {
	public static final String ENTRY_ID_GENERATE = "generate";
	
	private final String groupTemplateKey, groupItemTemplateKey;

	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(groupTemplateKey);
	}

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
				.orElse(-1);
		
		LinkedList<Integer> orderedEntryIds = new LinkedList<>();
		Arrays.stream(parsingContext.getTemplateParser().getEntryManager().getGeneratedEntries())
			.filter(e->e.hasAllTags(currentTags))
			.mapToInt(EntryData::getId)
			.sorted()
			.forEachOrdered(orderedEntryIds::add);
		for(int i=0;i<startIndex&&!orderedEntryIds.isEmpty();i++) { orderedEntryIds.poll(); }
		
		templateHtml = NaiSiteEngineUtils.regexSpliceString(ITemplateHandler.TEMPLATE_REGEX, templateHtml, (match)->{
			if (!orderedEntryIds.isEmpty() && orderedEntryIds.peek()==entryId) {orderedEntryIds.poll(); }
			if (orderedEntryIds.isEmpty()) { return match.group(); }
			
			String matchTemplateName = match.group(1);
			Map<String, String> matchTemplateParams = NaiSiteEngineUtils.extractTemplateParameters(match.group(2));
			if (!matchTemplateName.equals(groupItemTemplateKey)) { return match.group(); }
			if (!matchTemplateParams.get(NaiSiteEngineConstants.LAYER_PARAM_ENTRY_ID).equals(ENTRY_ID_GENERATE)) { return match.group(); }
			
			return match.group().replaceFirst(
					NaiSiteEngineConstants.LAYER_PARAM_ENTRY_ID+":"+ENTRY_ID_GENERATE, 
					NaiSiteEngineConstants.LAYER_PARAM_ENTRY_ID+":"+orderedEntryIds.poll());
		});
		
		return templateHtml;
	}

}
