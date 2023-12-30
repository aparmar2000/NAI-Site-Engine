package aparmar.naisiteengine.templating.preprocessors;

import aparmar.naisiteengine.templating.TemplateParser;

public interface ITemplatePreprocessor {

	public String processTemplate(String templateName, String templateHtml,
			TemplateParser.TemplateParsingContext parsingContext);
}
