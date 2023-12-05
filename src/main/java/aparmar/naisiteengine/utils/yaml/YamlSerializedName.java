package aparmar.naisiteengine.utils.yaml;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface YamlSerializedName {

	  /**
	   * @return the desired name of the field when it is serialized or deserialized
	   */
	  String value();
}
