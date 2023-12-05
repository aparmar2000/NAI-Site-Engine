package aparmar.naisiteengine.utils.yaml;

import java.util.Map;

public interface IYamlDeserializer<T> {
	public T deserialize(Map<String, Object> yamlData);
}
