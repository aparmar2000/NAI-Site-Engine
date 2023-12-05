package aparmar.naisiteengine.httphandlers;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.QUERY_PARAM_ENTRY_ID;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.utils.NaiSiteEngineUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntryRatingUpdateHttpHandler implements HttpHandler {
	private final static String ENDPOINT_PATH = "/update-rating";
	private final static Set<HttpString> ACCEPTED_METHODS = ImmutableSet.of(Methods.PUT, Methods.POST);
	
	private final EntryManager entryManager;
	private final HttpHandler next;

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (!exchange.getRelativePath().equals(ENDPOINT_PATH)) { next.handleRequest(exchange); return; }
		if (!ACCEPTED_METHODS.contains(exchange.getRequestMethod())) { next.handleRequest(exchange); return; }

		HeaderValues contentTypeHeader = exchange.getRequestHeaders().get("Content-Type");
		if (contentTypeHeader == null || contentTypeHeader.getFirst() == null) { throwBadRequest(exchange); return; }
		if (!contentTypeHeader.getFirst().equals("application/x-www-form-urlencoded")) { throwBadRequest(exchange); return; }
		
		Map<String, String> formData = NaiSiteEngineUtils.parseFormUrlencoded(exchange);
		if (formData == null) { throwBadRequest(exchange); return; }
		int newRating = Optional.ofNullable(formData.get("rating"))
				.map(Integer::parseInt)
				.orElse(-1);
		if (newRating < 0) { throwBadRequest(exchange); return; }

		int entryId = Optional.ofNullable(exchange.getQueryParameters().get(QUERY_PARAM_ENTRY_ID))
				.map(Deque::getFirst)
				.map(Integer::parseInt)
				.orElse(-1);
		EntryData updatedEntry = entryManager.getGeneratedEntryById(entryId);
		if (updatedEntry == null) { throwBadRequest(exchange); return; }
		
		updatedEntry.setHalfStarRating(newRating);
		entryManager.saveExistingGeneratedEntry(updatedEntry.getId());
		
		exchange.getResponseSender().close();
	}
	
	private void throwBadRequest(HttpServerExchange exchange) throws Exception {
		exchange.setStatusCode(StatusCodes.BAD_REQUEST);
		next.handleRequest(exchange);
	}

}
