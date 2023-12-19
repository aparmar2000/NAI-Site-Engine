package aparmar.naisiteengine;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.MAIN_THREAD_LOGGER;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

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
import aparmar.nai.utils.HelperConstants;
import aparmar.nai.utils.TextParameterPresets;
import aparmar.naisiteengine.config.SiteConfigManager;
import aparmar.naisiteengine.config.UserConfiguration;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.entry.EntryTypeManager;
import aparmar.naisiteengine.httphandlers.CategoryResolvingHttpHandler;
import aparmar.naisiteengine.httphandlers.DefaultRoutingHttpHandler;
import aparmar.naisiteengine.httphandlers.EntryRatingUpdateHttpHandler;
import aparmar.naisiteengine.httphandlers.LocalResourceHttpHandler;
import aparmar.naisiteengine.templating.CategoryNameProvider;
import aparmar.naisiteengine.templating.CategoryPaginationProvider;
import aparmar.naisiteengine.templating.TagListTemplateProvider;
import aparmar.naisiteengine.templating.CssTemplateProvider;
import aparmar.naisiteengine.templating.EntryGroupTemplateIdHandler;
import aparmar.naisiteengine.templating.EntryTagListProvider;
import aparmar.naisiteengine.templating.EntryTemplateFieldProvider;
import aparmar.naisiteengine.templating.ISpecialTemplateProvider;
import aparmar.naisiteengine.templating.ITemplateHandler;
import aparmar.naisiteengine.templating.StarRatingProvider;
import aparmar.naisiteengine.templating.TemplateParser;
import aparmar.naisiteengine.ui.JThreadMonitorPanel;
import aparmar.naisiteengine.utils.NaiSiteEngineConstants;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.error.SimpleErrorPageHandler;
import lombok.Getter;

public class NAISiteEngine {
	private static final ObjectMapper mapper = NaiSiteEngineConstants.OBJECT_MAPPER;
	
	@Getter
	private static EntryTypeManager entryTypeManager = null;

	private JFrame frame;

	/**
	 * Launch the application.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		NAISiteEngine engine = new NAISiteEngine();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					engine.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		engine.initializeSystem();
	}

	/**
	 * Create the application.
	 * @throws IOException 
	 */
	public NAISiteEngine() throws IOException {
		initializeFrame();
	}
	
	private void initializeSystem() throws IOException {
		UserConfiguration config = loadGeneralConfigFile();
		
		if (config.getGenerationConfig().getApiKey() == null 
				|| config.getGenerationConfig().getApiKey().isEmpty()
				|| !config.getGenerationConfig().getApiKey().matches(HelperConstants.PERSISTENT_KEY_REGEX)) {
			MAIN_THREAD_LOGGER.error("Configuration file does not have a valid NovelAI persistent key!");
			return;
		}
		NAIAPI nai = new NAIAPI(config.getGenerationConfig().getApiKey());
		
		SiteConfigManager siteConfigManager = new SiteConfigManager(new File("./config"), config);

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
		EntryManager entryManager = new EntryManager(new File("entries"), siteConfigManager, config);
		MAIN_THREAD_LOGGER.info(entryManager.getTemplateEntryCount()+" template entries loaded");
		MAIN_THREAD_LOGGER.info("Template entries per category: "+entryManager.getTemplateEntryCountByTag());
		MAIN_THREAD_LOGGER.info(entryManager.getGeneratedEntryCount()+" generated entries loaded");
		MAIN_THREAD_LOGGER.info("Generated entries per category: "+entryManager.getGeneratedEntryCountByTag());
		
		TemplateParser templateParser = initTemplateParser(config, siteConfigManager, entryManager);
		ExampleContext exampleContext = new ExampleContext(
				entryManager,
				templateParser,
				TextGenModel.KAYRA.getTokenizerForModel(),
				8192);
		
		MAIN_THREAD_LOGGER.info("Starting entry generation thread...");
		EntryGenerationManager entryGenerationManager = new EntryGenerationManager(nai, parameterPreset, config, siteConfigManager, entryManager, exampleContext);
		Thread entryGenerationThread = new Thread(entryGenerationManager);
		entryGenerationThread.start();
		
		MAIN_THREAD_LOGGER.info("Starting entry image generation thread...");
		EntryImageGenerationManager entryImageGenerationManager = new EntryImageGenerationManager(nai,baseImageRequest,config,entryManager);
		Thread entryImageGenerationThread = new Thread(entryImageGenerationManager);
		entryImageGenerationThread.start();
		
		int minCacheSize = Math.min(4, config.getGenerationConfig().getTargetCacheSize());
		while (entryManager.getGeneratedEntryCount() < minCacheSize) {
			MAIN_THREAD_LOGGER.info("Entry cache at "+entryManager.getGeneratedEntryCount()+", wating to reach a minimum of "+minCacheSize);
			try { Thread.sleep(5000); } catch (InterruptedException e) { }
		}
		
		Undertow server = initWebServer(config, templateParser, entryManager);
		MAIN_THREAD_LOGGER.info("Starting web server...");
        server.start();
		MAIN_THREAD_LOGGER.info("Server started and available at localhost:8087");
	}

	private static TemplateParser initTemplateParser(UserConfiguration userConfig, SiteConfigManager siteConfig, EntryManager entryManager) {
		ArrayList<ISpecialTemplateProvider> specialTemplateProviders = new ArrayList<>();
		specialTemplateProviders.add(new CssTemplateProvider());
		specialTemplateProviders.add(new CategoryNameProvider());
		specialTemplateProviders.add(new EntryTemplateFieldProvider());
		specialTemplateProviders.add(new StarRatingProvider());
		specialTemplateProviders.add(new CategoryPaginationProvider());
		specialTemplateProviders.add(new TagListTemplateProvider());
		specialTemplateProviders.add(new EntryTagListProvider());
		ArrayList<ITemplateHandler> templateHandlers = new ArrayList<>();
		templateHandlers.add(new EntryGroupTemplateIdHandler("entry_grid", "entry-preview-grid"));
		templateHandlers.add(new EntryGroupTemplateIdHandler("entry-list", "entry-preview-list"));
		return new TemplateParser(
				"/website-template",
				userConfig, siteConfig, entryManager,
				specialTemplateProviders,
				templateHandlers);
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

	private static Undertow initWebServer(UserConfiguration config, TemplateParser templateParser, EntryManager entryManager) {
		MAIN_THREAD_LOGGER.info("Configuring web server...");
		
		ArrayList<LocalResourceHttpHandler.RedirectEntry> redirectEntries = new ArrayList<>();
		redirectEntries.add(new LocalResourceHttpHandler.RedirectEntry("/entries", "entries/generated", false));
		
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

	/**
	 * Initialize the contents of the frame.
	 */
	private void initializeFrame() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		
		JThreadMonitorPanel coreThreadPanel = new JThreadMonitorPanel(MAIN_THREAD_LOGGER);
		tabbedPane.addTab("Core Thread", null, coreThreadPanel, null);
		
		JThreadMonitorPanel textThreadPanel = new JThreadMonitorPanel(NaiSiteEngineConstants.ENTRY_GEN_THREAD_LOGGER);
		tabbedPane.addTab("Text Thread", null, textThreadPanel, null);
		
		JThreadMonitorPanel imageThreadPanel = new JThreadMonitorPanel(NaiSiteEngineConstants.IMAGE_GEN_THREAD_LOGGER);
		tabbedPane.addTab("Image Thread", null, imageThreadPanel, null);
	}

}
