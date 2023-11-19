package aparmar.naisiteengine.httphandlers;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_CATEGORY;

import java.util.Optional;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CategoryResolvingHttpHandler implements HttpHandler {
	private final HttpHandler next;

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String currentCategory = Optional.ofNullable(exchange.getQueryParameters().get(QUERY_PARAM_CATEGORY))
				.map(de->de.getFirst())
				.orElse("");
		if (currentCategory.isEmpty()) {
			if (exchange.getRelativePath().startsWith("/category.html")
					|| exchange.getRelativePath().startsWith("/category-paginate.html")) {
				exchange.addQueryParam(QUERY_PARAM_CATEGORY, "all");
			}
		}
		
		next.handleRequest(exchange);
	}

}
