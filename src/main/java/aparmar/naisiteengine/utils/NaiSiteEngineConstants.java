package aparmar.naisiteengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaiSiteEngineConstants {
	public static final Logger MAIN_THREAD_LOGGER = LoggerFactory.getLogger("mainThread");
	public static final Logger ENTRY_GEN_THREAD_LOGGER = LoggerFactory.getLogger("entryGenThread");
	public static final Logger IMAGE_GEN_THREAD_LOGGER = LoggerFactory.getLogger("imageGenThread");
	
	public static final String QUERY_PARAM_CATEGORY = "cat";
	public static final String QUERY_PARAM_PAGINATION_START = "startIdx";
	public static final String QUERY_PARAM_ENTRY_ID = "articleId";
	public static final String LAYER_PARAM_ENTRY_ID = "article-id";
}
