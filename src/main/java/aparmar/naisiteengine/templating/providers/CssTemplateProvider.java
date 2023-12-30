package aparmar.naisiteengine.templating.providers;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.templating.TemplateParser;

public class CssTemplateProvider implements ISpecialTemplateProvider {
	private static final String CSS_SPECIAL_KEY = "css";
	
	private static final Pattern EXTRACT_NAME_REGEX = Pattern.compile("([^/]+)\\.\\w+");
	
	private final HashSet<String> noCssWarned = new HashSet<>();

	@Override
	public Set<String> getTemplateNames() {
		return ImmutableSet.of(CSS_SPECIAL_KEY);
	}
	
	@Override
	public void initializeData(TemplateParser.TemplateParsingContext parsingContext) {
		Matcher nameMatcher = EXTRACT_NAME_REGEX.matcher(parsingContext.getCurrentRelativePath());

		tryAddCssAtLoc(parsingContext, "", "root");
		if (nameMatcher.find()) {
			tryAddCssAtLoc(parsingContext, "", nameMatcher.group(1));
		}
	}
	
	@Override
	public void onTemplateLoad(String templateName,
			TemplateParser.TemplateParsingContext parsingContext) {
		tryAddCssAtLoc(parsingContext, "templates", templateName);
	}
	
	private void tryAddCssAtLoc(TemplateParser.TemplateParsingContext parsingContext, String directoryOffset, String name) {
		String internalTemplateCssLoc = parsingContext.getTemplateParser().getInternalRootDirectory();
		if (!directoryOffset.isEmpty()) { internalTemplateCssLoc += "/"+directoryOffset; }
		internalTemplateCssLoc += "/"+name+".css";
		try {
			IOUtils.resourceToURL(internalTemplateCssLoc);
			String templateCSS = "<link href=\""+directoryOffset+"/"+name+".css\" rel=\"stylesheet\">";
			parsingContext.getCssEmbeds().add(templateCSS);
		} catch (IOException e) {
			if (e.getMessage().contains("Resource not found: ")) {
				if (!noCssWarned.contains(name)) {
					System.out.println("No CSS file found for template '"+name+"'");
					noCssWarned.add(name);
				}
			} else {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String provideReplacementString(String templateName, Map<String,String> templateParams,
			TemplateParser.TemplateParsingContext parsingContext) {
		return parsingContext.getCssEmbeds().stream().collect(Collectors.joining("\n"));
	}
}
