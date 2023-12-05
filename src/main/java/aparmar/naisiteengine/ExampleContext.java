package aparmar.naisiteengine;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import aparmar.nai.utils.tokenization.TokenizedChunk;
import aparmar.nai.utils.tokenization.Tokenizers;
import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryManager;
import aparmar.naisiteengine.entry.TagGroupData.TagEntry;
import aparmar.naisiteengine.templating.TemplateParser;
import lombok.Getter;

public class ExampleContext {
	private final TokenizedChunk memoryChunk;
	
	private final TemplateParser templateParser;
	private final EntryManager entryManager;
	
	@Getter
	private double baseExampleContextAlloc = 0.35;
	@Getter
	private double generatedExampleContextAlloc = 0.65;
	@Getter
	private int contextLength;
	@Getter
	private Tokenizers tokenizer;
	
	public ExampleContext(EntryManager entryManager, TemplateParser templateParser, Tokenizers tokenizer, int contextLength) {
		this.entryManager = entryManager;
		this.templateParser = templateParser;
		this.tokenizer = tokenizer;
		this.contextLength = contextLength;
		
		memoryChunk = new TokenizedChunk(tokenizer, "");
	}
	
	// Setters
	
	public void setMemoryText(String memory) {
		memoryChunk.setTextChunk(memory);
	}
	
	public void setTokenizer(Tokenizers newTokenizer) {
		memoryChunk.setTokenizer(newTokenizer);
	}
	
	public void setExampleContextAllocations(double baseExampleContextAlloc, double generatedExampleContextAlloc) {
		if (baseExampleContextAlloc+generatedExampleContextAlloc>1) {
			throw new IllegalArgumentException(
					"Combined context allocations cannot exceed 100% - "
					+ "was ("+baseExampleContextAlloc+"+"+generatedExampleContextAlloc+")"
					+ " = "+(baseExampleContextAlloc+generatedExampleContextAlloc));
		}
		
		this.baseExampleContextAlloc = baseExampleContextAlloc;
		this.generatedExampleContextAlloc = generatedExampleContextAlloc;
	}
	
	public TokenizedChunk buildContext(TagEntry[] tags, int textGenerationConfigIndex, int generationReservedTokens, boolean useGenerated, long seed) {
		Random rng = new Random(seed);
		TokenizedChunk finalContext = new TokenizedChunk(getTokenizer(), "");
		finalContext.appendTokenizedChunk(memoryChunk);
		
		final int availableTokenBudget = contextLength-generationReservedTokens-memoryChunk.tokenLength()-20;
		final int baseExampleTokenThreshold = (int) Math.round(availableTokenBudget*baseExampleContextAlloc);
		final int generatedExampleTokenThreshold = (int) Math.round(availableTokenBudget*generatedExampleContextAlloc);

		TokenizedChunk baseExamplesChunk = new TokenizedChunk(getTokenizer(), "");
		EntryData[] orderedBaseExamples = entryManager.getTemplateEntries();// TODO: Sort by shared tags.
		for (EntryData baseExample : orderedBaseExamples) {
			
			TokenizedChunk tokenizedBaseExample = new TokenizedChunk(getTokenizer(), "\n"+baseExample.mergedContextString(templateParser, textGenerationConfigIndex));
			TokenizedChunk mergedChunks = 
					TokenizedChunk.mergeTokenizedChunks(finalContext, tokenizedBaseExample, baseExamplesChunk);
			if (mergedChunks.tokenLength()<=baseExampleTokenThreshold) {
				baseExamplesChunk.prependTokenizedChunk(tokenizedBaseExample);
			}
		}
		finalContext.appendTokenizedChunk(baseExamplesChunk);

		if (useGenerated) {
			TokenizedChunk generatedExamplesChunk = new TokenizedChunk(getTokenizer(), "");
			EntryData[] orderedGeneratedExamples = entryManager.getScoredGeneratedEntries();
			Arrays.sort(orderedGeneratedExamples, Comparator.comparingInt(EntryData::getRating));
			for (EntryData generatedExample : orderedGeneratedExamples) {
				
				TokenizedChunk tokenizedGeneratedExample = new TokenizedChunk(getTokenizer(), "\n"+generatedExample.mergedContextString(templateParser, textGenerationConfigIndex));
				TokenizedChunk mergedChunks = 
						TokenizedChunk.mergeTokenizedChunks(finalContext, tokenizedGeneratedExample, generatedExamplesChunk);
				if (mergedChunks.tokenLength()<=baseExampleTokenThreshold+generatedExampleTokenThreshold) {
					generatedExamplesChunk.prependTokenizedChunk(tokenizedGeneratedExample);
				}
			}
			finalContext.appendTokenizedChunk(generatedExamplesChunk);
		}
		
		finalContext.appendString("\n");
		return finalContext;
	}
}
