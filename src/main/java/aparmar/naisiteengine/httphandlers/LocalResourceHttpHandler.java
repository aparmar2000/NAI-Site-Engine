package aparmar.naisiteengine.httphandlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import aparmar.naisiteengine.templating.TemplateParser;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalResourceHttpHandler implements HttpHandler {
	@Data
	public static class RedirectEntry {
		private final String prefix, rootDirectory;
		private final boolean internal;
	}
	
	private final List<RedirectEntry> redirects;
	private final RedirectEntry defaultRedirect;
	private final TemplateParser templateParser;
	private final HttpHandler next;

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (exchange.getRequestMethod() != Methods.GET) { next.handleRequest(exchange); } 
		int statusCode = serveInternalResource(exchange);
		
		if (statusCode != StatusCodes.OK) {
			exchange.setStatusCode(statusCode);
			next.handleRequest(exchange);
		}
	}
	
	private int serveInternalResource(HttpServerExchange exchange) {
		String relativePath = exchange.getRelativePath();
		RedirectEntry activeRedirectEntry = defaultRedirect;
		for (RedirectEntry redirectEntry : redirects) {
			if (relativePath.startsWith(redirectEntry.getPrefix())) {
				activeRedirectEntry = redirectEntry;
				relativePath = relativePath.substring(redirectEntry.getPrefix().length());
				break;
			}
		}
		String resourceLoc = activeRedirectEntry.getRootDirectory()+relativePath;
		
		ByteBuffer resourceData = null;
		if (activeRedirectEntry.isInternal()) {
			try {
				resourceData = ByteBuffer.wrap(IOUtils.resourceToByteArray(resourceLoc));
			} catch (IOException e) {
				if (e.getMessage().contains("Resource not found")) {
					return StatusCodes.NOT_FOUND;
				}
				
				e.printStackTrace();
				return StatusCodes.INTERNAL_SERVER_ERROR;
			}
		} else {
			File resourceFile = new File(resourceLoc);
			if (!resourceFile.canRead()) { return StatusCodes.NOT_FOUND; }
			
			try (FileInputStream in = new FileInputStream(resourceFile)) {
				resourceData = ByteBuffer.wrap(IOUtils.toByteArray(in));
			} catch (IOException e) {
				e.printStackTrace();
				return StatusCodes.INTERNAL_SERVER_ERROR;
			}
		}
		
		String contentType = getContentType(relativePath);
		if (!contentType.isEmpty()) { exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, contentType); }
		if (contentType.equals("text/html")) {
			resourceData = templateParser.parseHTML(resourceData, relativePath, exchange.getQueryParameters());
		}
		exchange.getResponseSender().send(resourceData);
		
		return StatusCodes.OK;
	}
	
	private static final Pattern EXTENSION_REGEX = Pattern.compile("\\.([^.]+)$");
	private String getContentType(String resourcePath) {
		Matcher extensionMatcher = EXTENSION_REGEX.matcher(resourcePath);
		extensionMatcher.find();
		String extension = extensionMatcher.group(1).toLowerCase();
		switch (extension) {
		case "html":
			return "text/html";
		case "css":
			return "text/css";
		case "png":
			return "image/png";
		case "jpeg":
		case "jpg":
			return "image/jpeg";
		case "gif":
			return "image/gif";
		case "js":
			return "application/javascript";
		}
		
		return "";
	}
}
