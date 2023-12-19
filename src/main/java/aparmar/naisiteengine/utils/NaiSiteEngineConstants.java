package aparmar.naisiteengine.utils;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class NaiSiteEngineConstants {
	public static final Logger MAIN_THREAD_LOGGER = Logger.getLogger("mainThread");
	public static final Logger ENTRY_GEN_THREAD_LOGGER = Logger.getLogger("entryGenThread");
	public static final Logger IMAGE_GEN_THREAD_LOGGER = Logger.getLogger("imageGenThread");
	
	public static final ObjectMapper OBJECT_MAPPER = YAMLMapper.builder(new YAMLFactory())
			.configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true)
			.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
			.build()
			.registerModule(new JavaTimeModule());
	
	public static final String QUERY_PARAM_TAGS = "tags";
	public static final String QUERY_PARAM_PAGINATION_START = "startIdx";
	public static final String QUERY_PARAM_ENTRY_ID = "entryId";
	public static final String LAYER_PARAM_ENTRY_ID = "entry-id";
	
	public static final String CONFIG_ENTRY_TYPE_PREFIX = "entry-";
	public static final String CONFIG_TAG_GROUP_PREFIX = "tag-group-";
}
