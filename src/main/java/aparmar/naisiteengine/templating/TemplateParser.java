package aparmar.naisiteengine.templating;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.IOUtils;

import aparmar.naisiteengine.config.SiteConfigManager;
import aparmar.naisiteengine.config.UserConfiguration;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.templating.handlers.ITemplateHandler;
import aparmar.naisiteengine.templating.preprocessors.ITemplatePreprocessor;
import aparmar.naisiteengine.templating.providers.ISpecialTemplateProvider;
import aparmar.naisiteengine.utils.NaiSiteEngineUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

public class TemplateParser {
	@Getter
	private final String internalRootDirectory;
	@Getter
	private final UserConfiguration userConfig;
	@Getter
	private final SiteConfigManager siteConfig;
	@Getter
	private final EntryManager entryManager;
	private final List<ITemplatePreprocessor> templatePreprocessors;
	private final List<ISpecialTemplateProvider> specialTemplateProviders;
	private final List<ITemplateHandler> templateHandlers;
	
	public static final String SEED_PARAM_KEY = "seed";
	
	
	public TemplateParser(String internalRootDirectory, 
			UserConfiguration config, SiteConfigManager siteConfig, EntryManager entryManager,
			List<ITemplatePreprocessor> templatePreprocessors,
			List<ISpecialTemplateProvider> specialTemplateProviders,
			List<ITemplateHandler> templateHandlers) {
		this.internalRootDirectory = internalRootDirectory;
		this.userConfig = config;
		this.siteConfig = siteConfig;
		this.entryManager = entryManager;
		
		this.templatePreprocessors = templatePreprocessors;
		this.specialTemplateProviders = specialTemplateProviders;
		this.templateHandlers = templateHandlers;
	}
	
	@Data
	@AllArgsConstructor
	public static class TemplateParsingContext {
		Set<String> cssEmbeds;
		private String currentRelativePath;
		private Map<String, Deque<String>> queryParameters; 
		private Map<String, String> layerParameters;
		private TemplateParser templateParser;
		
		public TemplateParsingContext copy() {
			return new TemplateParsingContext(
					cssEmbeds,
					currentRelativePath, 
					queryParameters, 
					new HashMap<>(layerParameters), 
					templateParser);
		}
	}

	public String parseHTML(String htmlString, String currentRelativePath, Map<String, Deque<String>> queryParameters) {
		HashMap<String,String> layerParameters = new HashMap<>();
		Set<String> cssEmbeds = new HashSet<>();
		TemplateParsingContext parsingContext = new TemplateParsingContext(
				cssEmbeds,
				currentRelativePath, 
				queryParameters, 
				layerParameters, 
				this);
		
		specialTemplateProviders.forEach(p->p.initializeData(parsingContext));
		
		htmlString = parseHTMLRecursive(htmlString, parsingContext);
		htmlString = populateSpecialTemplates(htmlString, parsingContext);
		
		return htmlString;
	}
	public ByteBuffer parseHTML(ByteBuffer resourceData, String currentRelativePath, Map<String, Deque<String>> queryParameters) {
		String htmlString = StandardCharsets.UTF_8.decode(resourceData).toString();
		htmlString = parseHTML(htmlString, currentRelativePath, queryParameters);
		
		return StandardCharsets.UTF_8.encode(htmlString);
	}

	private String parseHTMLRecursive(String htmlString, 
			TemplateParsingContext parsingContext) {
		return NaiSiteEngineUtils.regexSpliceString(ITemplateHandler.TEMPLATE_REGEX, htmlString, (match)->{
			String templateName = match.group(1);
			String templateHtml = loadTemplateData(templateName);
			
			Map<String, String> modifiedLayerParameters = new HashMap<>(parsingContext.getLayerParameters());
			modifiedLayerParameters.putAll(NaiSiteEngineUtils.extractTemplateParameters(match.group(2)));
			long layerSeed = ThreadLocalRandom.current().nextLong();
			modifiedLayerParameters.put(SEED_PARAM_KEY, Long.toString(layerSeed));
			
			TemplateParsingContext newParsingContext = parsingContext.copy();
			newParsingContext.setLayerParameters(modifiedLayerParameters);
			
			for (ITemplatePreprocessor templatePreprocessor : templatePreprocessors) {
				templateHtml = templatePreprocessor
						.processTemplate(templateName, templateHtml, newParsingContext);
			}
			
			for (ITemplateHandler templateHandler : templateHandlers) {
				if (!templateHandler.getTemplateNames().contains(templateName)) { continue; }
				templateHtml = templateHandler
						.processTemplate(templateName, templateHtml, newParsingContext);
			}

			specialTemplateProviders
				.forEach(p->p.onTemplateLoad(templateName, newParsingContext));
			templateHtml = populateSpecialTemplates(templateHtml, newParsingContext);
			String parsedTemplateHTML = parseHTMLRecursive(templateHtml, newParsingContext);
			
			return parsedTemplateHTML;
		});
	}

	private String populateSpecialTemplates(String htmlString, 
			TemplateParsingContext parsingContext) {		
		htmlString = NaiSiteEngineUtils.regexSpliceString(ISpecialTemplateProvider.SPECIAL_TEMPLATE_REGEX, htmlString, (match)->{
			String templateName = match.group(1);
			Map<String, String> templateParameters = NaiSiteEngineUtils.extractTemplateParameters(match.group(2));
			
			Optional<String> replacement = specialTemplateProviders.stream()
				.filter(provider->provider.getTemplateNames().contains(templateName))
				.map(provider->provider.provideReplacementString(templateName, templateParameters, parsingContext))
				.filter(Objects::nonNull)
				.findFirst();
			
			return replacement.orElse(match.group());
		});
		return htmlString;
	}
	
	private String loadTemplateData(String name) {
		String internalTemplateHtmlLoc = internalRootDirectory+"/templates/"+name+".html";
		String templateHTML = "<span>Template lookup failed!</span>";
		
		try {
			templateHTML = IOUtils.resourceToString(internalTemplateHtmlLoc, StandardCharsets.UTF_8);
		} catch (IOException e) {
			if (e.getMessage().contains("Resource not found: ")) {
				System.out.println("No HTML file found for template '"+name+"'");
			} else {
				e.printStackTrace();
			}
		}
		
		return templateHTML;
	}

}
