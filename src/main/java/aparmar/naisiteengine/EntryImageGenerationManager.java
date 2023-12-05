package aparmar.naisiteengine;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.IMAGE_GEN_THREAD_LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import aparmar.nai.NAIAPI;
import aparmar.nai.data.request.imagen.ImageGenerationRequest;
import aparmar.nai.data.response.ImageSetWrapper;
import aparmar.naisiteengine.config.UserConfiguration;
import aparmar.naisiteengine.entry.EntryData;
import aparmar.naisiteengine.entry.EntryFieldConfig;
import aparmar.naisiteengine.entry.EntryFieldConfig.EntryFieldType;
import aparmar.naisiteengine.entry.EntryManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntryImageGenerationManager implements Runnable {
	private final NAIAPI nai;
	private final ImageGenerationRequest baseImageGenerationRequest;
	
	private final UserConfiguration userConfig;
	private final EntryManager entryManager;
	
	private final AtomicBoolean stopFlag = new AtomicBoolean(false);

	@Override
	public void run() {
		IMAGE_GEN_THREAD_LOGGER.info("Entry image generation thread started.");
		stopFlag.set(false);
		
		while (!stopFlag.get()) {
			if (userConfig.getGenerationConfig().isImageGenerationEnabled()) {
				Optional<EntryData> imagelessArticleOptional = Arrays.stream(entryManager.getGeneratedEntries())
						.filter(EntryData::hasImagesToGenerate)
						.findAny();
				if (imagelessArticleOptional.isPresent()) {
					EntryData imagelessArticle = imagelessArticleOptional.get();
					HashMap<Integer, List<EntryFieldConfig>> emptyImageFieldMap = 
							Arrays.stream(imagelessArticle.getEntryType().getEntryFieldConfigs())
								.filter(f->f.getType()==EntryFieldType.IMAGE)
								.filter(f->imagelessArticle.getField(f)==null)
								.collect(Collectors.toMap(
										f->f.getFieldValueConfig().getGenerationConfigIndex(), 
										f->new LinkedList<>(Collections.singletonList(f)), 
										(a,b)->{ a.addAll(b); return a; }, 
										HashMap<Integer, List<EntryFieldConfig>>::new));
					
					try {
						IMAGE_GEN_THREAD_LOGGER.info("\tGenerating entry images...");
						
						for (int imageGenerationSetIndex : emptyImageFieldMap.keySet()) {
							IMAGE_GEN_THREAD_LOGGER.info("\t\tGenerating image set "+imageGenerationSetIndex+"...");
							ImageSetWrapper generatedImageSet = nai.generateImage(
									baseImageGenerationRequest.toBuilder()
										.input(imagelessArticle.imagePrompt(imageGenerationSetIndex))
										.build());
							
							String imgFilename = imagelessArticle.getEntryFilename()+"_"+imageGenerationSetIndex+".png";
							File imgFile = entryManager.getGeneratedEntryDirectory().toPath().resolve(imgFilename).toFile();
	
							IMAGE_GEN_THREAD_LOGGER.info("\t\tSaving image...");
							generatedImageSet.writeImageToFile(0, imgFile);
							emptyImageFieldMap.get(imageGenerationSetIndex)
								.forEach(f->imagelessArticle.setField(f, imgFilename));
							entryManager.saveExistingGeneratedEntry(imagelessArticle.getId());
							IMAGE_GEN_THREAD_LOGGER.info("\t\tSaved.");
						}
						
						IMAGE_GEN_THREAD_LOGGER.info("\tGeneration complete.");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			try { Thread.sleep(500); } catch (InterruptedException e) { }
		}
		IMAGE_GEN_THREAD_LOGGER.info("Entry image generation thread stopped.");
	}
	
	public void stop() { stopFlag.set(true); }

}
