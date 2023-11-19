package aparmar.naisiteengine.templating;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ARTICLE_ID;

import java.util.Deque;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.EntryManager;
import aparmar.naisiteengine.EntryData;
import aparmar.naisiteengine.templating.TemplateParser.TemplateParsingContext;
import aparmar.naisiteengine.utils.NaiSiteEngineConstants;

public class StarRatingTemplateHandler implements ITemplateHandler {
	public static final String STAR_RATING_TEMPLATE_KEY = "star-rating";
	
	private static final String ENABLED_PROPERTY_KEY = "enabled";

	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(STAR_RATING_TEMPLATE_KEY);
	}

	@Override
	public String processTemplate(String templateName, String templateHtml, TemplateParsingContext parsingContext) {
		EntryManager articleManager = parsingContext.getTemplateParser().getArticleManager();
		
		boolean isEnabled = Optional.ofNullable(parsingContext.getLayerParameters().get(ENABLED_PROPERTY_KEY))
				.map(v->v.equals("true"))
				.orElse(false);

		String articleIdParam = parsingContext.getLayerParameters()
				.get(NaiSiteEngineConstants.LAYER_PARAM_ARTICLE_ID);
		int articleIdQueryParam = Optional.ofNullable(parsingContext.getQueryParameters().get(QUERY_PARAM_ARTICLE_ID))
				.map(Deque::getFirst)
				.map(Integer::parseInt)
				.orElse(-1);
		int articleId = 0;
		if (articleIdParam!=null && articleIdParam.matches("^\\d+$")) {
			articleId = Integer.parseUnsignedInt(articleIdParam);
		} else if (articleIdQueryParam>=0) {
			articleId = articleIdQueryParam;
		}
		
		EntryData currentArticle = articleManager.getGeneratedEntryById(articleId);
		if (currentArticle == null) { return "ERR: unknown article id"; }
		int currentArticleRating = currentArticle.getHalfStarRating();

		if (currentArticleRating>=0) {
			templateHtml = templateHtml.replaceFirst("(<input)(.*?value=\\\""+currentArticleRating+"\\\")", "$1 checked$2");
		}
		if (isEnabled) {
			templateHtml = templateHtml.replaceFirst(
					"(<form action=\\\"update-rating)(\\\")", 
					"$1?"+QUERY_PARAM_ARTICLE_ID+"="+articleId+"$2");
		} else {
			templateHtml = templateHtml.replaceFirst("<iframe.*</iframe>", "");
			
			templateHtml = templateHtml.replaceFirst("<form.*?>", "<form readonly>");
			templateHtml = templateHtml.replaceFirst("<fieldset", "<fieldset readonly");
			templateHtml = templateHtml.replaceAll("<input", "<input readonly disabled");
			
			templateHtml = templateHtml.replaceAll("(\\w+=\\\"rating\\d+)(\\\")", "$1_readonly$2");
			templateHtml = templateHtml.replaceAll("(name=\\\"rating)(\\\")", "$1_readonly$2");
		}
		
		return templateHtml;
	}

}
