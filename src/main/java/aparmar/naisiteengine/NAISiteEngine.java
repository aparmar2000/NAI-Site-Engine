package aparmar.naisiteengine;

import static aparmar.nai.utils.HelperConstants.DINKUS;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.MAIN_THREAD_LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import aparmar.nai.NAIAPI;
import aparmar.nai.data.request.TextGenModel;
import aparmar.nai.data.request.TextGenerationParameters;
import aparmar.nai.data.request.imagen.ImageGenerationRequest;
import aparmar.nai.data.request.imagen.ImageGenerationRequest.ImageGenModel;
import aparmar.nai.data.request.imagen.ImageGenerationRequest.QualityTagsLocation;
import aparmar.nai.data.request.imagen.ImageParameters;
import aparmar.nai.data.request.imagen.ImageParameters.ImageGenSampler;
import aparmar.nai.utils.TextParameterPresets;
import aparmar.naisiteengine.config.UserConfiguration;
import aparmar.naisiteengine.httphandlers.ArticleRatingUpdateHttpHandler;
import aparmar.naisiteengine.httphandlers.CategoryResolvingHttpHandler;
import aparmar.naisiteengine.httphandlers.DefaultRoutingHttpHandler;
import aparmar.naisiteengine.httphandlers.LocalResourceHttpHandler;
import aparmar.naisiteengine.templating.ArticleGroupTemplateIdHandler;
import aparmar.naisiteengine.templating.ArticleTemplateProvider;
import aparmar.naisiteengine.templating.CategoryNameProvider;
import aparmar.naisiteengine.templating.CategoryPaginationProvider;
import aparmar.naisiteengine.templating.CategoryTemplateProvider;
import aparmar.naisiteengine.templating.CssTemplateProvider;
import aparmar.naisiteengine.templating.ISpecialTemplateProvider;
import aparmar.naisiteengine.templating.ITemplateHandler;
import aparmar.naisiteengine.templating.StarRatingTemplateHandler;
import aparmar.naisiteengine.templating.TemplateParser;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.error.SimpleErrorPageHandler;

public class NAISiteEngine {
	public static void main(String[] args) throws IOException {
		UserConfiguration config = new UserConfiguration("config.yaml", "/config-template.yaml");
		
		if (config.getGenerationConfig().getApiKey() == null 
				|| config.getGenerationConfig().getApiKey().isEmpty()
				|| config.getGenerationConfig().getApiKey().matches(null)) {
			MAIN_THREAD_LOGGER.error("Configuration file does not have a valid NovelAI persistent key!");
			return;
		}
		NAIAPI nai = new NAIAPI(config.getGenerationConfig().getApiKey());

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
		MAIN_THREAD_LOGGER.info("Template entries per category: "+entryManager.getTemplateEntryCountByCategory());
		MAIN_THREAD_LOGGER.info(entryManager.getGeneratedEntryCount()+" generated articles loaded");
		MAIN_THREAD_LOGGER.info("Generated entries per category: "+entryManager.getGeneratedEntryCountByCategory());
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
				config.getWebsiteConfig().getCategories().length*2, 
				config.getGenerationConfig().getTargetCacheSize())) {
			try { Thread.sleep(1000); } catch (InterruptedException e) { }
		}
		
		MAIN_THREAD_LOGGER.info("Configuring web server...");
		ArrayList<ISpecialTemplateProvider> specialTemplateProviders = new ArrayList<>();
		specialTemplateProviders.add(new CssTemplateProvider());
		specialTemplateProviders.add(new CategoryNameProvider());
		specialTemplateProviders.add(new CategoryTemplateProvider());
		specialTemplateProviders.add(new ArticleTemplateProvider());
		specialTemplateProviders.add(new CategoryPaginationProvider());
		ArrayList<ITemplateHandler> templateHandlers = new ArrayList<>();
		templateHandlers.add(new ArticleGroupTemplateIdHandler("article_grid", "article-preview-grid"));
		templateHandlers.add(new ArticleGroupTemplateIdHandler("article-list", "article-preview-list"));
		templateHandlers.add(new StarRatingTemplateHandler());
		TemplateParser templateParser = new TemplateParser(
				"/website-template", config, entryManager,
				specialTemplateProviders,
				templateHandlers);
		
		ArrayList<LocalResourceHttpHandler.RedirectEntry> redirectEntries = new ArrayList<>();
		redirectEntries.add(new LocalResourceHttpHandler.RedirectEntry("/articles", "articles/generated", false));
		
		SimpleErrorPageHandler errorPageHandler = new SimpleErrorPageHandler();
		LocalResourceHttpHandler localResourceHandler = new LocalResourceHttpHandler(
				redirectEntries,
				new LocalResourceHttpHandler.RedirectEntry("", "/website-template", true), 
				templateParser, 
				errorPageHandler);
		ArticleRatingUpdateHttpHandler articleRatingUpdateHttpHandler = new ArticleRatingUpdateHttpHandler(entryManager, localResourceHandler);
		CategoryResolvingHttpHandler categoryResolvingHttpHandler = new CategoryResolvingHttpHandler(articleRatingUpdateHttpHandler);
		DefaultRoutingHttpHandler defaultRoutingHandler = new DefaultRoutingHttpHandler(categoryResolvingHttpHandler);
		HttpHandler rootHandler = new CanonicalPathHandler(defaultRoutingHandler);
		
		MAIN_THREAD_LOGGER.info("Starting web server...");
		Undertow server = Undertow.builder()
                .addHttpListener(8087, "localhost")
                .setHandler(rootHandler)
                .build();
        server.start();
	}

	private static ExampleContext initExampleContext(
			TextGenerationParameters parameterPreset,
			UserConfiguration config,
			EntryManager articleManager) {
		ExampleContext exampleContext = new ExampleContext(
				articleManager,
				TextGenModel.KAYRA.getTokenizerForModel(),
				8192);
		exampleContext.setMemoryText(config.getGenerationConfig().getMemoryText()+"\n"+DINKUS);
		
//		System.out.println(exampleContext.buildContext("all", 500).getTextChunk());
		return exampleContext;
	}

}
