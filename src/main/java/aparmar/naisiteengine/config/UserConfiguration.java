package aparmar.naisiteengine.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class UserConfiguration {
	@JsonProperty("generation")
	private final GenerationConfiguration generationConfig;
}
