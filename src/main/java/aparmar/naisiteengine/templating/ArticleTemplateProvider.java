package aparmar.naisiteengine.templating;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_CATEGORY;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ARTICLE_ID;

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

public class ArticleTemplateProvider implements ISpecialTemplateProvider {
	public static final String ARTICLE_LINK_SPECIAL_KEY = "article-link";
	public static final String ARTICLE_IMAGE_SPECIAL_KEY = "article-image";
	public static final String ARTICLE_CATEGORY_SPECIAL_KEY = "article-category";
	public static final String ARTICLE_TITLE_SPECIAL_KEY = "article-title";
	public static final String ARTICLE_SNIPPET_SPECIAL_KEY = "article-snippet";
	public static final String ARTICLE_BODY_SPECIAL_KEY = "article-body";
	
	private static final String ARTICLE_ID_LATEST = "latest";
	private static final String ARTICLE_ID_RANDOM = "random";
	
	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(
				ARTICLE_LINK_SPECIAL_KEY,
				ARTICLE_IMAGE_SPECIAL_KEY,
				ARTICLE_CATEGORY_SPECIAL_KEY,
				ARTICLE_TITLE_SPECIAL_KEY,
				ARTICLE_SNIPPET_SPECIAL_KEY,
				ARTICLE_BODY_SPECIAL_KEY);
	}
	
	@Override
	public String provideReplacementString(String templateName, Map<String, String> templateParams,
			TemplateParser.TemplateParsingContext parsingContext) {
		EntryManager articleManager = parsingContext.getTemplateParser().getArticleManager();
		String currentCategory = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_CATEGORY))
				.map(de->de.getFirst())
				.orElse("all");

		String articleIdParam = parsingContext.getLayerParameters()
				.get(NaiSiteEngineConstants.LAYER_PARAM_ARTICLE_ID);
		int articleIdQueryParam = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_ARTICLE_ID))
				.map(Deque::getFirst)
				.map(Integer::parseInt)
				.orElse(-1);
		int articleId = 1;
		if (articleIdParam!=null) {
			if (articleIdParam.matches("^\\d+$")) {
				articleId = Integer.parseUnsignedInt(articleIdParam);
			} else if (articleIdParam.equals(ARTICLE_ID_LATEST)) {
				articleId = articleManager.getLatestGeneratedEntryIdByCategory(currentCategory);
			} else if (articleIdParam.equals(ARTICLE_ID_RANDOM)) {
				Random rng = new Random(
						Long.parseLong(parsingContext.getLayerParameters()
								.getOrDefault(TemplateParser.SEED_PARAM_KEY, "0")));
				articleId = articleManager.getRandomGeneratedEntryIdByCategory(rng, currentCategory);
			}
		} else if (articleIdQueryParam>=0) {
			articleId = articleIdQueryParam;
		} else {
			return templateName+" parse error: invalid article-id";
		}
		
		EntryData articleData = articleManager.getGeneratedEntryById(articleId);
		switch (templateName) {
		case ARTICLE_LINK_SPECIAL_KEY:
			return "article.html?"+QUERY_PARAM_CATEGORY+"="+articleData.getCategory()+"&"+QUERY_PARAM_ARTICLE_ID+"="+articleId;
		case ARTICLE_IMAGE_SPECIAL_KEY:
			String imgSrc = "https://placehold.co/768x512/png";
			if (!articleData.getImgFilename().isEmpty()) {
				imgSrc = "articles/"+articleData.getImgFilename();
			}
			return "<img src=\""+imgSrc+"\" class=\"article-image\">";
		case ARTICLE_TITLE_SPECIAL_KEY:
			return articleData.getTitle();
		case ARTICLE_CATEGORY_SPECIAL_KEY:
			return "<a href=\"category.html?"+QUERY_PARAM_CATEGORY+"="+articleData.getCategory()+"\" class=\"article-category\">"+articleData.getCategory()+"</a>";
		case ARTICLE_SNIPPET_SPECIAL_KEY:
			return Arrays.stream(articleData.getEntryBody().split(" "))
					.limit(30)
					.collect(Collectors.joining(" "))+"...";
		case ARTICLE_BODY_SPECIAL_KEY:
			return articleData.getEntryBody();
		}
		
		return null;
	}
}
