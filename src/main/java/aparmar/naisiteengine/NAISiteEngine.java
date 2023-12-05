package aparmar.naisiteengine;

import static aparmar.nai.utils.HelperConstants.DINKUS;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.MAIN_THREAD_LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import aparmar.nai.NAIAPI;
import aparmar.nai.data.request.TextGenModel;
import aparmar.nai.data.request.TextGenerationParameters;
import aparmar.nai.data.request.imagen.ImageGenerationRequest;
import aparmar.nai.data.request.imagen.ImageGenerationRequest.ImageGenModel;
import aparmar.nai.data.request.imagen.ImageGenerationRequest.QualityTagsLocation;
import aparmar.nai.data.request.imagen.ImageParameters;
import aparmar.nai.data.request.imagen.ImageParameters.ImageGenSampler;
import aparmar.nai.utils.TextParameterPresets;
import aparmar.naisiteengine.config.SiteConfigManager;
import aparmar.naisiteengine.config.UserConfiguration;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.entry.EntryTypeManager;
import aparmar.naisiteengine.httphandlers.EntryRatingUpdateHttpHandler;
import aparmar.naisiteengine.httphandlers.CategoryResolvingHttpHandler;
import aparmar.naisiteengine.httphandlers.DefaultRoutingHttpHandler;
import aparmar.naisiteengine.httphandlers.LocalResourceHttpHandler;
import aparmar.naisiteengine.templating.EntryGroupTemplateIdHandler;
import aparmar.naisiteengine.templating.EntryTemplateProvider;
import aparmar.naisiteengine.templating.CategoryNameProvider;
import aparmar.naisiteengine.templating.CategoryPaginationProvider;
import aparmar.naisiteengine.templating.CategoryTemplateProvider;
import aparmar.naisiteengine.templating.CssTemplateProvider;
import aparmar.naisiteengine.templating.ISpecialTemplateProvider;
import aparmar.naisiteengine.templating.ITemplateHandler;
import aparmar.naisiteengine.templating.StarRatingProvider;
import aparmar.naisiteengine.templating.TemplateParser;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.error.SimpleErrorPageHandler;
import lombok.Getter;

public class NAISiteEngine {
	private static final ObjectMapper mapper = new ObjectMapper();
	
	@Getter
	private static EntryTypeManager entryTypeManager = null;
	
	public static void main(String[] args) throws IOException {
		UserConfiguration config = loadGeneralConfigFile();
		
		if (config.getGenerationConfig().getApiKey() == null 
				|| config.getGenerationConfig().getApiKey().isEmpty()
				|| config.getGenerationConfig().getApiKey().matches(null)) {
			MAIN_THREAD_LOGGER.error("Configuration file does not have a valid NovelAI persistent key!");
			return;
		}
		NAIAPI nai = new NAIAPI(config.getGenerationConfig().getApiKey());
		
		SiteConfigManager siteConfigManager = new SiteConfigManager(new File("/config"), config);

		TextGenerationParameters parameterPreset = 
				TextParameterPresets.getPresetByNameAndModel(TextGenModel.KAYRA, "Fresh Coffee");
		parameterPreset.setMaxLength(2048);
		
		ImageGenerationRequest baseImageRequest = ImageGenerationRequest.builder()
				.model(ImageGenModel.ANIME_V3)
				.parameters(ImageParameters.builder()
						.width(768)
						.height(512)
						.steps(28)
						.scale(5)
						.sampler(ImageGenSampler.K_EULER_ANCESTRAL)
						.qualityToggle(true)
						.qualityInsertLocation(QualityTagsLocation.APPEND)
						.undesiredContent(ImageGenerationRequest.ANIME_V3_LIGHT_UC)
						.build())
				.build();

		MAIN_THREAD_LOGGER.info("Initializing entryManager...");
		EntryManager entryManager = new EntryManager(new File("entries"), config);
		MAIN_THREAD_LOGGER.info(entryManager.getTemplateEntryCount()+" template entries loaded");
		MAIN_THREAD_LOGGER.info("Template entries per category: "+entryManager.getTemplateEntryCountByTag());
		MAIN_THREAD_LOGGER.info(entryManager.getGeneratedEntryCount()+" generated entrys loaded");
		MAIN_THREAD_LOGGER.info("Generated entries per category: "+entryManager.getGeneratedEntryCountByTag());
		ExampleContext exampleContext = initExampleContext(parameterPreset, config, entryManager);
		
		MAIN_THREAD_LOGGER.info("Starting entry generation thread...");
		EntryGenerationManager entryGenerationManager = new EntryGenerationManager(nai, parameterPreset, config, entryManager, exampleContext);
		Thread entryGenerationThread = new Thread(entryGenerationManager);
		entryGenerationThread.start();
		
		MAIN_THREAD_LOGGER.info("Starting entry image generation thread...");
		EntryImageGenerationManager entryImageGenerationManager = new EntryImageGenerationManager(nai,baseImageRequest,config,entryManager);
		Thread entryImageGenerationThread = new Thread(entryImageGenerationManager);
		entryImageGenerationThread.start();
		
		while (entryManager.getGeneratedEntryCount() < Math.min(
				4, 
				config.getGenerationConfig().getTargetCacheSize())) {
			try { Thread.sleep(1000); } catch (InterruptedException e) { }
		}
		
		Undertow server = initWebServer(config, entryManager);
		MAIN_THREAD_LOGGER.info("Starting web server...");
        server.start();
	}

	private static UserConfiguration loadGeneralConfigFile() throws IOException, StreamReadException, DatabindException {
		MAIN_THREAD_LOGGER.info("Loading config file...");
		File externalYamlFile = new File("config.yaml");
		if (!externalYamlFile.isFile()) {
			MAIN_THREAD_LOGGER.info("Config file not found, writing default config...");
			byte[] templateData = IOUtils.resourceToByteArray("/config-template.yaml");
			FileUtils.writeByteArrayToFile(externalYamlFile, templateData);
		}
		UserConfiguration config = mapper.readValue(externalYamlFile, UserConfiguration.class);
		MAIN_THREAD_LOGGER.info("Config file loaded.");
		return config;
	}

	private static Undertow initWebServer(UserConfiguration config, EntryManager entryManager) {
		MAIN_THREAD_LOGGER.info("Configuring web server...");
		ArrayList<ISpecialTemplateProvider> specialTemplateProviders = new ArrayList<>();
		specialTemplateProviders.add(new CssTemplateProvider());
		specialTemplateProviders.add(new CategoryNameProvider());
		specialTemplateProviders.add(new CategoryTemplateProvider());
		specialTemplateProviders.add(new EntryTemplateProvider());
		specialTemplateProviders.add(new StarRatingProvider());
		specialTemplateProviders.add(new CategoryPaginationProvider());
		ArrayList<ITemplateHandler> templateHandlers = new ArrayList<>();
		templateHandlers.add(new EntryGroupTemplateIdHandler("entry_grid", "entry-preview-grid"));
		templateHandlers.add(new EntryGroupTemplateIdHandler("entry-list", "entry-preview-list"));
		TemplateParser templateParser = new TemplateParser(
				"/website-template", config, entryManager,
				specialTemplateProviders,
				templateHandlers);
		
		ArrayList<LocalResourceHttpHandler.RedirectEntry> redirectEntries = new ArrayList<>();
		redirectEntries.add(new LocalResourceHttpHandler.RedirectEntry("/entrys", "entrys/generated", false));
		
		SimpleErrorPageHandler errorPageHandler = new SimpleErrorPageHandler();
		LocalResourceHttpHandler localResourceHandler = new LocalResourceHttpHandler(
				redirectEntries,
				new LocalResourceHttpHandler.RedirectEntry("", "/website-template", true), 
				templateParser, 
				errorPageHandler);
		EntryRatingUpdateHttpHandler entryRatingUpdateHttpHandler = new EntryRatingUpdateHttpHandler(entryManager, localResourceHandler);
		CategoryResolvingHttpHandler categoryResolvingHttpHandler = new CategoryResolvingHttpHandler(entryRatingUpdateHttpHandler);
		DefaultRoutingHttpHandler defaultRoutingHandler = new DefaultRoutingHttpHandler(categoryResolvingHttpHandler);
		HttpHandler rootHandler = new CanonicalPathHandler(defaultRoutingHandler);

		MAIN_THREAD_LOGGER.info("Building web server...");
		Undertow server = Undertow.builder()
                .addHttpListener(8087, "localhost")
                .setHandler(rootHandler)
                .build();
		return server;
	}

	private static ExampleContext initExampleContext(
			TextGenerationParameters parameterPreset,
			UserConfiguration config,
			EntryManager entryManager) {
		ExampleContext exampleContext = new ExampleContext(
				entryManager,
				TextGenModel.KAYRA.getTokenizerForModel(),
				8192);
		exampleContext.setMemoryText(config.getGenerationConfig().getMemoryText()+"\n"+DINKUS);
		
//		System.out.println(exampleContext.buildContext("all", 500).getTextChunk());
		return exampleContext;
	}

}
