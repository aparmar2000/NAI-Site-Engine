package aparmar.naisiteengine;

import static aparmar.nai.utils.HelperConstants.DINKUS;
import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.ENTRY_GEN_THREAD_LOGGER;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import aparmar.nai.NAIAPI;
import aparmar.nai.data.request.TextGenModel;
import aparmar.nai.data.request.TextGenerationParameters;
import aparmar.nai.data.request.TextGenerationRequest;
import aparmar.nai.data.request.TextGenerationParameters.LogitBias;
import aparmar.nai.data.response.TextGenerationResponse;
import aparmar.nai.utils.tokenization.TokenizedChunk;
import aparmar.nai.utils.tokenization.Tokenizers;
import aparmar.naisiteengine.config.UserConfiguration;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntryGenerationManager implements Runnable {
	private final NAIAPI nai;
	private final TextGenerationParameters baseParameters;
	
	private final UserConfiguration userConfig;
	private final EntryManager entryManager;
	private final ExampleContext contextManager;
	
	private final AtomicBoolean stopFlag = new AtomicBoolean(false);

	@Override
	public void run() {
		ENTRY_GEN_THREAD_LOGGER.info("Entry generation thread started.");
		stopFlag.set(false);
		final int targetCacheSize = userConfig.getGenerationConfig().getTargetCacheSize();
		final Random rng = new Random();
		
		while (!stopFlag.get()) {
			Map<String, Integer> unreadCountByCategory = 
					entryManager.getUnreadGeneratedEntryCountByCategory();
			int totalUnread = unreadCountByCategory.values().stream()
					.mapToInt(Integer::intValue)
					.sum();
			while (totalUnread < userConfig.getGenerationConfig().getTargetCacheSize()) {
				ENTRY_GEN_THREAD_LOGGER.info("Entry cache at "+totalUnread+"/"+targetCacheSize+"; Generating more...");
				
				String leastCachedCategory = unreadCountByCategory.entrySet().stream()
					.min(Comparator.comparingInt(Map.Entry::getValue))
					.get().getKey();
				
				try {
					EntryData newArticle = generateEntryInCategory(leastCachedCategory, rng.nextInt(5)==0);
					if (newArticle == null) {
						ENTRY_GEN_THREAD_LOGGER.info("Entry parsing error.");
					} else {
						entryManager.addGeneratedEntry(newArticle);
						ENTRY_GEN_THREAD_LOGGER.info("Entry generated, saving...");
						entryManager.saveGeneratedEntriesToFileSystem();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				
				unreadCountByCategory = 
						entryManager.getUnreadGeneratedEntryCountByCategory();
				totalUnread = unreadCountByCategory.values().stream()
						.mapToInt(Integer::intValue)
						.sum();
			}
			
			try { Thread.sleep(500); } catch (InterruptedException e) { }
		}
		
		ENTRY_GEN_THREAD_LOGGER.info("Entry generation thread stopped.");
	}
	
	private static final int SOFT_MIN_TITLE_LENGTH = 12;
	private static final int SOFT_MAX_TITLE_LENGTH = 16;
	private static final int SOFT_MIN_ARTICLE_LENGTH = 128;
	private static final int SOFT_MAX_ARTICLE_LENGTH = 512;
	private static final int MAX_RETRIES = 4;
	private EntryData generateEntryInCategory(String category, boolean useGenerated) throws IOException {
		long seed = System.currentTimeMillis();
		
		TextGenModel textModel = userConfig.getGenerationConfig().getModel();
		Tokenizers tokenizer = textModel.getTokenizerForModel();
		TokenizedChunk generatedChunk = new TokenizedChunk(
				tokenizer, 
				"[ "+userConfig.getGenerationConfig().getCategoryDescriptor()+": "+category+" ]\n");

		LogitBias newlineBias = LogitBias.builder()
				.sequence(tokenizer.encode("\n"))
				.bias(0)
				.ensureSequenceFinishes(false)
				.unbiasOnceGenerated(true)
				.build();
		LogitBias newlineDinkusBias = LogitBias.builder()
				.sequence(tokenizer.encode("\n"+DINKUS))
				.bias(0)
				.ensureSequenceFinishes(true)
				.unbiasOnceGenerated(true)
				.build();		

		int generationPhase = 0; // 0-Title, 1-Body
		int currentPhaseLength = 0;
		int generations = 0;
		while (!generatedChunk.getTextChunk().contains("\n"+DINKUS)) {
			TokenizedChunk exampleContextChunk = contextManager.buildContext(
					category, 
					baseParameters.getMaxLength()-generatedChunk.tokenLength()-5, 
					useGenerated, seed);
			TokenizedChunk fullContext = TokenizedChunk.mergeTokenizedChunks(tokenizer, exampleContextChunk, generatedChunk);

			TextGenerationParameters parameters = baseParameters.toBuilder().build();
			switch (generationPhase) {
			case 0:
				parameters.getStopSequences().add(tokenizer.encode("\n"));
				
				if (currentPhaseLength>SOFT_MAX_TITLE_LENGTH) {
					newlineBias.setBias(1);
					parameters.getLogitBiases().add(newlineBias);
				}
				
				break;
			case 1:
			default:
				double biasScale = 0;
				if (currentPhaseLength < SOFT_MIN_ARTICLE_LENGTH) {
					biasScale = 1 - (currentPhaseLength/((double)SOFT_MIN_ARTICLE_LENGTH));
					biasScale *= -1;
				}
				if (currentPhaseLength > SOFT_MAX_ARTICLE_LENGTH) {
					biasScale = currentPhaseLength-SOFT_MAX_ARTICLE_LENGTH*0.01;
					biasScale = Math.min(biasScale, 2);
				}
				newlineDinkusBias.setBias(biasScale);
				
				parameters.getLogitBiases().add(newlineDinkusBias);
				parameters.getStopSequences().add(tokenizer.encode("\n"+DINKUS));
				break;
			}

			
			TextGenerationResponse latestGeneration = null;
			int retries = 0;
			while(true) {
				retries++;
				try {
					latestGeneration = nai.generateText(TextGenerationRequest.builder()
							.input(fullContext.getBase64EncodedTokens())
							.model(textModel)
							.parameters(parameters)
							.build());
					break;
				} catch (SocketTimeoutException e) {
					if (retries>=MAX_RETRIES) {
						throw e;
					}
					ENTRY_GEN_THREAD_LOGGER.debug("Request timeout, retrying... "+retries+"/"+MAX_RETRIES);
				}
			}
			
			currentPhaseLength += latestGeneration.getOutput().tokenLength();
			switch (generationPhase) {
			case 0:
				if (latestGeneration.getOutput().getTextChunk().contains("\n")) {
					generationPhase = 1;
					currentPhaseLength = 0;
				}
				break;
			}
			generatedChunk.appendTokenizedChunk(latestGeneration.getOutput());
			
			generations++;
			ENTRY_GEN_THREAD_LOGGER.info("\tGenerating '"+category+"' entry... (step "+generations+", phase "+generationPhase+", length "+String.format("%,d", generatedChunk.getTokens().length)+")");
		}
		
		return EntryData.loadFromString(generatedChunk.getTextChunk());
	}
	
	public void stop() { stopFlag.set(true); }

}
