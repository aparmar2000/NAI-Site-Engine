package aparmar.naisiteengine.templating.handlers;

import java.util.Set;
import java.util.regex.Pattern;

import aparmar.naisiteengine.templating.TemplateParser;

public interface ITemplateHandler {
	public String TEMPLATE_PREFIX = "template-";
	public Pattern TEMPLATE_REGEX = Pattern.compile("\\{\\{"+TEMPLATE_PREFIX+"(\\S+?)(?: (.+?))?\\s*\\}\\}");

	public Set<String> getTemplateNames();

	public String processTemplate(String templateName, String templateHtml,
			TemplateParser.TemplateParsingContext parsingContext);
}
