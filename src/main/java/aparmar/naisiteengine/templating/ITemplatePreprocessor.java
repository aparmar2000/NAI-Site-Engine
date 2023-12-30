package aparmar.naisiteengine.templating;

public interface ITemplatePreprocessor {

	public String processTemplate(String templateName, String templateHtml,
			TemplateParser.TemplateParsingContext parsingContext);
}
