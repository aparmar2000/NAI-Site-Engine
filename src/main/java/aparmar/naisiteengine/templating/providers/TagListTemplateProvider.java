package aparmar.naisiteengine.templating.providers;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_TAGS;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import aparmar.naisiteengine.config.SiteConfigManager;
import aparmar.naisiteengine.entry.TagGroupData;
import aparmar.naisiteengine.templating.TemplateParser;

public class TagListTemplateProvider implements ISpecialTemplateProvider {
	private static final String TAGS_SPECIAL_KEY = "list-tags";
	
	private static final String GROUP_PROPERTY_KEY = "group";
	private static final String PAGE_PROPERTY_KEY = "target-page";
	
	private static final String MODE_PROPERTY_KEY = "mode";
	private static final String MODE_SELECT = "select";
	private static final String MODE_ADD = "add";

	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(TAGS_SPECIAL_KEY);
	}
	
	private String assembleCategoryHTML(SiteConfigManager siteConfigManager, String currentRelativePath, 
			Map<String, Deque<String>> queryParameters, Map<String, String> templateParams) {
		Deque<String> currentTags = Optional.ofNullable(queryParameters.get(QUERY_PARAM_TAGS))
				.orElse(new LinkedList<>());
		Set<String> currentTagSet = ImmutableSet.copyOf(currentTags);
		if (!templateParams.containsKey(GROUP_PROPERTY_KEY)) { return "ERR: missing tag group id"; }
		String tagGroupName = Optional.ofNullable(templateParams.get(GROUP_PROPERTY_KEY))
				.get();
		String targetPage = Optional.ofNullable(templateParams.get(PAGE_PROPERTY_KEY))
				.orElse(currentRelativePath);
		String mode = Optional.ofNullable(templateParams.get(MODE_PROPERTY_KEY))
				.orElse(MODE_SELECT);
		switch (mode) {
		case MODE_SELECT:
		case MODE_ADD:
			break;
		default:
			return "ERR: unknown mode '"+mode+"'";
		}
		
		TagGroupData tagGroup = siteConfigManager.getTagGroupManager().getTagGroupByName(tagGroupName);
		if (tagGroup == null) {
			return "ERR: unknown tag group '"+tagGroupName+"'";
		}
		
		return Arrays.stream(tagGroup.getTagEntries())
				.map(t->"<li"+(currentTagSet.contains(t.getName())?" class=\"active\"":"")+"><a href=\""+assembleTarget(targetPage, t.getName(), currentTagSet, tagGroup, mode)+"\">"+t.getName()+"</a></li>")
				.collect(Collectors.joining("\n"));
	}
	
	private String assembleTarget(String targetPage, String targetTag, Set<String> currentTagSet, TagGroupData tagGroup, String mode) {
		SetView<String> activeTags;
		if (mode == MODE_SELECT) {
			activeTags = Sets.difference(currentTagSet, ImmutableSet.copyOf(tagGroup.getTagNames()));
			activeTags = Sets.union(activeTags, ImmutableSet.of(targetTag));
		} else {
			activeTags = Sets.union(currentTagSet, ImmutableSet.of(targetTag));
		}
		
		return targetPage+"?"+activeTags.stream()
			.map(t->QUERY_PARAM_TAGS+"="+t)
			.collect(Collectors.joining("&"));
	}

	@Override
	public String provideReplacementString(String templateName, Map<String,String> templateParams,
			TemplateParser.TemplateParsingContext parsingContext) {
		return assembleCategoryHTML(
				parsingContext.getTemplateParser().getSiteConfig(), 
				parsingContext.getCurrentRelativePath(), 
				parsingContext.getQueryParameters(),
				templateParams);
	}

}
