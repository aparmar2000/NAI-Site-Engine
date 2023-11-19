package aparmar.naisiteengine.templating;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public interface ISpecialTemplateProvider {
	public static final String SPECIAL_PREFIX = "special-";
	public static final Pattern SPECIAL_TEMPLATE_REGEX = Pattern.compile("\\{\\{"+SPECIAL_PREFIX+"(\\S+?)(?: (.+?))?\\s*\\}\\}");	
	
	public Set<String> getTemplateNames();
	
	public default void initializeData(
			TemplateParser.TemplateParsingContext parsingContext) {}
	public default void onTemplateLoad(String templateName,
			TemplateParser.TemplateParsingContext parsingContext) {}
	public String provideReplacementString(String templateName, Map<String,String> templateParams,
			TemplateParser.TemplateParsingContext parsingContext);
}
