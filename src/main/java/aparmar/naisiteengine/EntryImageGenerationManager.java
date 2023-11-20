package aparmar.naisiteengine;

import static aparmar.naisiteengine.utils.NaiSiteEngineConstants.IMAGE_GEN_THREAD_LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import aparmar.nai.NAIAPI;
import aparmar.nai.data.request.imagen.ImageGenerationRequest;
import aparmar.nai.data.response.ImageSetWrapper;
import aparmar.naisiteengine.config.UserConfiguration;
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
				Optional<EntryData> imagelessArticleOptional = entryManager.tryGetGeneratedEntryWithoutImage();
				if (imagelessArticleOptional.isPresent()) {
					EntryData imagelessArticle = imagelessArticleOptional.get();
					try {
						IMAGE_GEN_THREAD_LOGGER.info("\tGenerating entry image...");
						ImageSetWrapper generatedImageSet = nai.generateImage(baseImageGenerationRequest.toBuilder()
								.input(imagelessArticle.imagePrompt())
								.build());
						
						String imgFilename = imagelessArticle.getEntryFilename()+".png";
						File imgFile = entryManager.getGeneratedEntryDirectory().toPath().resolve(imgFilename).toFile();

						IMAGE_GEN_THREAD_LOGGER.info("Saving entry image...");
						generatedImageSet.writeImageToFile(0, imgFile);
						imagelessArticle.setImgFilename(imgFilename);
						
						entryManager.saveGeneratedEntriesToFileSystem();
						IMAGE_GEN_THREAD_LOGGER.info("Saved.");
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
