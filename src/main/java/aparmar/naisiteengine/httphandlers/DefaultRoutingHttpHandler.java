package aparmar.naisiteengine.httphandlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultRoutingHttpHandler implements HttpHandler {
	private final HttpHandler next;

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (exchange.getRelativePath().length()<=1) { exchange.setRelativePath("/index.html"); }
		next.handleRequest(exchange);
	}

}
