package aparmar.naisiteengine;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.ENTRY_GEN_THREAD_LOGGER;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import aparmar.nai.NAIAPI;
import aparmar.nai.data.request.TextGenModel;
import aparmar.nai.data.request.TextGenerationParameters;
import aparmar.nai.data.request.TextGenerationParameters.LogitBias;
import aparmar.nai.data.request.TextGenerationRequest;
import aparmar.nai.data.response.TextGenerationResponse;
import aparmar.nai.utils.tokenization.TokenizedChunk;
import aparmar.nai.utils.tokenization.Tokenizers;
import aparmar.naisiteengine.config.SiteConfigManager;
import aparmar.naisiteengine.config.UserConfiguration;
import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.entry.EntryTextGenerationConfig;
import aparmar.naisiteengine.entry.EntryType;
import aparmar.naisiteengine.entry.TagGroupData;
import aparmar.naisiteengine.entry.TagGroupData.TagEntry;
import aparmar.naisiteengine.utils.NaiSiteEngineUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntryGenerationManager implements Runnable {
	private final NAIAPI nai;
	private final TextGenerationParameters baseParameters;
	
	private final UserConfiguration userConfig;
	private final SiteConfigManager siteConfigManager;
	private final EntryManager entryManager;
	private final ExampleContext contextManager;
	
	private final AtomicBoolean stopFlag = new AtomicBoolean(false);

	@Override
	public void run() {
		try {
			ENTRY_GEN_THREAD_LOGGER.info("Entry generation thread started.");
			stopFlag.set(false);
			final int targetCacheSize = userConfig.getGenerationConfig().getTargetCacheSize();
			final Random rng = new Random();
			
			while (!stopFlag.get()) {
				Map<String, Integer> unreadCountByType = 
						entryManager.getUnreadGeneratedEntryCountByType();
				int totalUnread = unreadCountByType.values().stream()
						.mapToInt(Integer::intValue)
						.sum();
				while (totalUnread < userConfig.getGenerationConfig().getTargetCacheSize()) {
					ENTRY_GEN_THREAD_LOGGER.info("Entry cache at "+totalUnread+"/"+targetCacheSize+"; Generating more...");
					
					String leastCachedType = unreadCountByType.entrySet().stream()
						.min(Comparator.comparingInt(Map.Entry::getValue))
						.get().getKey();
					
					try {
						EntryData newArticle = generateEntryOfType(leastCachedType, rng.nextInt(5)==0);
						if (newArticle == null) {
							ENTRY_GEN_THREAD_LOGGER.info("Entry generation failed.");
						} else {
							entryManager.addGeneratedEntry(newArticle);
							ENTRY_GEN_THREAD_LOGGER.info("Entry generated, saving...");
							entryManager.saveGeneratedEntriesToFileSystem();
						}
					} catch (IOException e) {
						ENTRY_GEN_THREAD_LOGGER.error("\t"+e.getLocalizedMessage(), e);
					}
					
					
					unreadCountByType = 
							entryManager.getUnreadGeneratedEntryCountByType();
					totalUnread = unreadCountByType.values().stream()
							.mapToInt(Integer::intValue)
							.sum();
				}
				
				try { Thread.sleep(500); } catch (InterruptedException e) { }
			}
			
			ENTRY_GEN_THREAD_LOGGER.info("Entry generation thread stopped.");
		} catch (Exception e) {
			ENTRY_GEN_THREAD_LOGGER.error(e.getLocalizedMessage(), e);
		}
	}
	
	private static final int MAX_GENERATION_RETRIES = 2;
	private EntryData generateEntryOfType(String entryTypeName, boolean useGenerated) throws IOException {
		long seed = System.currentTimeMillis();
		Random rng = new Random(seed);
		
		TextGenModel textModel = userConfig.getGenerationConfig().getModel();
		EntryType entryType = siteConfigManager.getEntryTypeManager().getEntryTypeByName(entryTypeName);
		
		LinkedList<TagEntry> selectedTags = new LinkedList<>();
		for (TagGroupData tagGroup : siteConfigManager.getTagGroupManager().getTagGroups()) {
			TagEntry[] tagOptions = tagGroup.getTagEntries();
			NaiSiteEngineUtils.shuffleArray(tagOptions, rng);
			for (int i=0;i<rng.nextInt(tagGroup.getMaxSelectedValues())+1;i++) {
				selectedTags.add(tagOptions[i]);
			}
		}
		ENTRY_GEN_THREAD_LOGGER.info(
				"\tSelected tags '"+
				selectedTags.stream()
					.map(TagEntry::getName)
					.collect(Collectors.joining("','"))+
				"' for generation.");
		
		EntryData newEntryData = siteConfigManager.createEntry(entryType);
		newEntryData.setTags(selectedTags.stream().map(TagEntry::getName).toArray(String[]::new));
		for (int i=0;i<entryType.getTextGenerationConfigs().length;i++) {
			int retries = 0;
			while (true) {
				retries++;
				String generatedText = 
						generateForTextGenerationConfig(textModel, selectedTags.toArray(new TagEntry[0]), seed, useGenerated, entryType, i);
				boolean successfullyPopulated = false;
				if (generatedText != null) {
					successfullyPopulated = entryType.populateFieldMapFromTextGen(newEntryData, i, generatedText);
					break;
				}
				
				if (!successfullyPopulated && retries>=MAX_GENERATION_RETRIES) {
					ENTRY_GEN_THREAD_LOGGER.debug("\tGeneration failed for index "+i+", retrying... "+retries+"/"+MAX_GENERATION_RETRIES);
					return null;
				}
			}
		}
		
		return newEntryData;
	}

	private static final int MAX_NETWORK_RETRIES = 4;
	@Nullable
	@CheckForNull
	private String generateForTextGenerationConfig(
			TextGenModel textModel, TagEntry[] tags, long seed, boolean useGenerated,
			EntryType entryType, int textGenerationConfigIndex) throws IOException {
		Tokenizers tokenizer = textModel.getTokenizerForModel();
		EntryTextGenerationConfig textGenerationConfig = entryType.getTextGenerationConfigs()[textGenerationConfigIndex];
		
		int[] encodedEndingSequence = tokenizer.encode(textGenerationConfig.getEndingBias());
		LogitBias endingBias = LogitBias.builder()
				.sequence(encodedEndingSequence)
				.bias(0)
				.ensureSequenceFinishes(false)
				.unbiasOnceGenerated(true)
				.build();
		
		TokenizedChunk tagChunk = new TokenizedChunk(
				tokenizer, 
				TagEntry.toTextGenerationContextForm(tags)+"\n");
		TokenizedChunk generatedChunk = new TokenizedChunk(
				tokenizer, 
				"");
		
		int generations = 0;
		int generatedLength = 0;
		while (!textGenerationConfig.getContentMatcher(generatedChunk.getTextChunk()).find()) {
			TokenizedChunk exampleContextChunk = contextManager.buildContext(
					tags, textGenerationConfigIndex,
					baseParameters.getMaxLength()-generatedChunk.tokenLength()-5, 
					useGenerated, seed);
			TokenizedChunk fullContext = TokenizedChunk.mergeTokenizedChunks(tokenizer, exampleContextChunk, tagChunk, generatedChunk);
			
			TextGenerationParameters parameters = baseParameters.toBuilder().build();
			parameters.setStopSequences(Optional.ofNullable(parameters.getStopSequences()).orElse(new LinkedList<>()));
			parameters.getStopSequences().add(encodedEndingSequence);
			endingBias.setEnsureSequenceFinishes(false);
			if (generatedLength < textGenerationConfig.getTargetMinLength()) {
				double proportionSatisfied = generatedLength/textGenerationConfig.getTargetMinLength();
				endingBias.setBias((1-proportionSatisfied) * -0.05);
			} else if (generatedLength > textGenerationConfig.getTargetMaxLength()*0.75) {
				endingBias.setEnsureSequenceFinishes(true);
				double rampLower = textGenerationConfig.getTargetMaxLength()*0.75;
				double rampUpper = textGenerationConfig.getTargetMaxLength()*0.9;
				double rampRange = rampUpper - rampLower;
				
				endingBias.setBias((generatedLength-rampLower)/rampRange * 1.2);
			}
			parameters.setLogitBiases(Optional.ofNullable(parameters.getLogitBiases()).orElse(new LinkedList<>()));
			parameters.getLogitBiases().add(endingBias);
			
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
					if (retries>=MAX_NETWORK_RETRIES) {
						throw e;
					}
					ENTRY_GEN_THREAD_LOGGER.debug("\tRequest timeout, retrying... "+retries+"/"+MAX_NETWORK_RETRIES);
				}
			}
			
			generatedChunk.appendTokenizedChunk(latestGeneration.getOutput());
			generatedLength = generatedChunk.tokenLength();
			
			if (generatedLength > textGenerationConfig.getTargetMaxLength()) {
				ENTRY_GEN_THREAD_LOGGER.info("\tEntry-in-progress too long ("+generatedLength+"/"+textGenerationConfig.getTargetMaxLength()+"), discarded.");
				return null;
			}
			
			generations++;
			ENTRY_GEN_THREAD_LOGGER.info("\tGenerating '"+entryType.getName()+"' entry... (step "+generations+", length "+String.format("%,d", generatedLength)+")");
		}
		
		if (generatedLength < textGenerationConfig.getTargetMinLength()) {
			ENTRY_GEN_THREAD_LOGGER.info("\tGenerated entry too short ("+generatedLength+"/"+textGenerationConfig.getTargetMinLength()+"), discarded.");
			return null;
		}
		
		return generatedChunk.getTextChunk();
	}
	
	public void stop() { stopFlag.set(true); }

}
