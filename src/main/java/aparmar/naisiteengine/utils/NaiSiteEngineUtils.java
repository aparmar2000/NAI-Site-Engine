package aparmar.naisiteengine.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xnio.channels.StreamSourceChannel;

import io.undertow.server.HttpServerExchange;

public class NaiSiteEngineUtils {

	public static String regexSpliceString(Pattern regex, String inputString, 
			Function<MatchResult, String> calculateReplacement) {
		StringBuilder finalStringBuilder = new StringBuilder();
		Matcher templateMatcher = regex.matcher(inputString);
		int lastEnd = 0;
		while(templateMatcher.find()) {
			finalStringBuilder.append(inputString.substring(lastEnd, templateMatcher.start()));
			lastEnd = templateMatcher.end();
			
			String replacementString = calculateReplacement.apply(templateMatcher.toMatchResult());			
			finalStringBuilder.append(replacementString);
		}
		finalStringBuilder.append(inputString.substring(lastEnd));
		
		return finalStringBuilder.toString();
	}
	
	private static final Pattern PARAMETER_EXTRACTION_REGEX = Pattern.compile("(\\S+):(\\\"(?:\\\\.|[^\\\"])+\\\"|\\S+)");
	private static final String QUOTE_CLEANING_REGEX = "(^\\\"|\\\"$|\\\\(?=\\\"))";
	public static Map<String, String> extractTemplateParameters(String unparsed) {
		Map<String, String> templateParameters = new HashMap<>();
		if (unparsed != null) {
			Matcher matcher = PARAMETER_EXTRACTION_REGEX.matcher(unparsed);
			while (matcher.find()) {
				templateParameters.put(matcher.group(1), matcher.group(2).replaceAll(QUOTE_CLEANING_REGEX, ""));
			}
		}
		
		return templateParameters;
	}
	
	public static Map<String, String> parseFormUrlencoded(HttpServerExchange exchange) throws IOException {
		Map<String, String> result = new HashMap<>();
		
		int requestContentLength = (int) exchange.getRequestContentLength();
		if (requestContentLength <= 0) { return null; }
		ByteBuffer requestDataBuffer = ByteBuffer.allocate(requestContentLength);
		try (StreamSourceChannel requestChannel = exchange.getRequestChannel()) {
			while (requestChannel.read(requestDataBuffer) != -1) {}
		}
		requestDataBuffer.rewind();
		String requestBody = StandardCharsets.UTF_8.decode(requestDataBuffer).toString();
		Arrays.stream(requestBody.split("&"))
			.map(pair->pair.split("=", 2))
			.forEach(pair->result.put(pair[0], pair[1]));
		
		return result;
	}

}
