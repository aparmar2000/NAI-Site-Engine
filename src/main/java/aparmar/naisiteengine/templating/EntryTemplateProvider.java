package aparmar.naisiteengine.templating;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_CATEGORY;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ENTRY_ID;

import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.EntryManager;
import aparmar.naisiteengine.EntryData;
import aparmar.naisiteengine.utils.NaiSiteEngineConstants;

public class EntryTemplateProvider implements ISpecialTemplateProvider {
	public static final String ENTRY_LINK_SPECIAL_KEY = "article-link";
	public static final String ENTRY_IMAGE_SPECIAL_KEY = "article-image";
	public static final String ENTRY_CATEGORY_SPECIAL_KEY = "article-category";
	public static final String ENTRY_TITLE_SPECIAL_KEY = "article-title";
	public static final String ENTRY_SNIPPET_SPECIAL_KEY = "article-snippet";
	public static final String ENTRY_BODY_SPECIAL_KEY = "article-body";
	
	private static final String ENTRY_ID_LATEST = "latest";
	private static final String ENTRY_ID_RANDOM = "random";
	
	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(
				ENTRY_LINK_SPECIAL_KEY,
				ENTRY_IMAGE_SPECIAL_KEY,
				ENTRY_CATEGORY_SPECIAL_KEY,
				ENTRY_TITLE_SPECIAL_KEY,
				ENTRY_SNIPPET_SPECIAL_KEY,
				ENTRY_BODY_SPECIAL_KEY);
	}
	
	@Override
	public String provideReplacementString(String templateName, Map<String, String> templateParams,
			TemplateParser.TemplateParsingContext parsingContext) {
		EntryManager entryManager = parsingContext.getTemplateParser().getEntryManager();
		String currentCategory = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_CATEGORY))
				.map(de->de.getFirst())
				.orElse("all");

		String articleIdParam = parsingContext.getLayerParameters()
				.get(NaiSiteEngineConstants.LAYER_PARAM_ENTRY_ID);
		int entryIdQueryParam = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_ENTRY_ID))
				.map(Deque::getFirst)
				.map(Integer::parseInt)
				.orElse(-1);
		int articleId = 1;
		if (articleIdParam!=null) {
			if (articleIdParam.matches("^\\d+$")) {
				articleId = Integer.parseUnsignedInt(articleIdParam);
			} else if (articleIdParam.equals(ENTRY_ID_LATEST)) {
				articleId = entryManager.getLatestGeneratedEntryIdByCategory(currentCategory);
			} else if (articleIdParam.equals(ENTRY_ID_RANDOM)) {
				Random rng = new Random(
						Long.parseLong(parsingContext.getLayerParameters()
								.getOrDefault(TemplateParser.SEED_PARAM_KEY, "0")));
				articleId = entryManager.getRandomGeneratedEntryIdByCategory(rng, currentCategory);
			}
		} else if (entryIdQueryParam>=0) {
			articleId = entryIdQueryParam;
		} else {
			return templateName+" parse error: invalid article-id";
		}
		
		EntryData entryData = entryManager.getGeneratedEntryById(articleId);
		switch (templateName) {
		case ENTRY_LINK_SPECIAL_KEY:
			return "article.html?"+QUERY_PARAM_CATEGORY+"="+entryData.getCategory()+"&"+QUERY_PARAM_ENTRY_ID+"="+articleId;
		case ENTRY_IMAGE_SPECIAL_KEY:
			String imgSrc = "https://placehold.co/768x512/png";
			if (!entryData.getImgFilename().isEmpty()) {
				imgSrc = "articles/"+entryData.getImgFilename();
			}
			return "<img src=\""+imgSrc+"\" class=\"article-image\">";
		case ENTRY_TITLE_SPECIAL_KEY:
			return entryData.getTitle();
		case ENTRY_CATEGORY_SPECIAL_KEY:
			return "<a href=\"category.html?"+QUERY_PARAM_CATEGORY+"="+entryData.getCategory()+"\" class=\"article-category\">"+entryData.getCategory()+"</a>";
		case ENTRY_SNIPPET_SPECIAL_KEY:
			return Arrays.stream(entryData.getEntryBody().split(" "))
					.limit(30)
					.collect(Collectors.joining(" "))+"...";
		case ENTRY_BODY_SPECIAL_KEY:
			return entryData.getEntryBody();
		}
		
		return null;
	}
}
