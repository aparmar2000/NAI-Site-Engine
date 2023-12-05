package aparmar.naisiteengine.utils.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

public class GenericYamlDeserializer {
	private final HashMap<Class<?>, IYamlDeserializer<?>> customDeserializers = new HashMap<>();
	
	public <T> void registerCustomDeserializer(IYamlDeserializer<T> deserializer, Class<T> clazz) {
		customDeserializers.put(clazz, deserializer);
	}
	
	public <T> T loadFromYamlFile(File yamlFile, Class<T> clazz) throws IOException, ReflectiveOperationException {
		Load yamlReader = new Load(LoadSettings.builder().build());
		try (FileInputStream in = new FileInputStream(yamlFile)) {
			@SuppressWarnings("unchecked")
			Map<String, Object> yamlData = (Map<String, Object>) yamlReader.loadFromInputStream(in);
			
			return loadFromYamlData(yamlData, clazz);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T loadFromYamlData(Map<String, Object> yamlData, Class<T> clazz) throws ReflectiveOperationException {
		if (customDeserializers.containsKey(clazz)) {
			IYamlDeserializer<T> customDeserializer = (IYamlDeserializer<T>) customDeserializers.get(clazz);
			return customDeserializer.deserialize(yamlData);
		}
		
		T result = clazz.newInstance();
		
		HashMap<String, Field> fieldNameMap = Arrays.stream(clazz.getFields())
			.filter(f->!f.isSynthetic())
			.filter(f->!Modifier.isFinal(f.getModifiers()))
			.filter(f->!Modifier.isTransient(f.getModifiers()))
			.collect(Collectors.toMap(
					f->f.isAnnotationPresent(YamlSerializedName.class) ? f.getAnnotation(YamlSerializedName.class).value() : f.getName(),
					Function.identity(), 
					(a,b)->a, HashMap<String, Field>::new));
		
		for (Map.Entry<String, Object> yamlEntry : yamlData.entrySet()) {
			String entryKey = yamlEntry.getKey();
			if (fieldNameMap.containsKey(entryKey)) {
				
				continue;
			}
		}
		
		return result;
	}
	
	private <T, F> void setFieldFromYamlData(T instance, Class<T> instanceClazz, Field field, String fieldName, Class<F> fieldType, Object yamlValue) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		F parsedValue = loadValueFromYamlData(fieldName, fieldType, yamlValue);
		
		Method setterMethod = null;
		try {
			setterMethod = instanceClazz.getDeclaredMethod(fieldToSetterName(field), field.getType());
		} catch (NoSuchMethodException | SecurityException e) {}
		
		if (setterMethod != null) {
			setterMethod.setAccessible(true);
			setterMethod.invoke(instance, parsedValue);
			setterMethod.setAccessible(false);
		} else {
			field.setAccessible(true);
			field.set(instance, parsedValue);
			field.setAccessible(false);
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> T loadValueFromYamlData(String fieldName, Class<T> fieldType, Object yamlValue) throws InstantiationException, IllegalAccessException {
		if (fieldType == Boolean.TYPE) {
			if (yamlValue.getClass() != Boolean.TYPE) {
				throw new YamlParseException(String.format("Expected field '%' to be of type %, but it was of type %", fieldName, fieldType, yamlValue.getClass()));
			}
			
			return (T) yamlValue;
		}
		
		if (fieldType == Byte.TYPE
				|| fieldType == Integer.TYPE
				|| fieldType == Long.TYPE
				|| fieldType == Short.TYPE
				|| fieldType == Float.TYPE
				|| fieldType == Double.TYPE) {
			if (yamlValue.getClass() != Byte.TYPE
					&& yamlValue.getClass() != Integer.TYPE 
					&& yamlValue.getClass() != Long.TYPE 
					&& yamlValue.getClass() != Short.TYPE 
					&& yamlValue.getClass() != Float.TYPE 
					&& yamlValue.getClass() != Double.TYPE) {
				throw new YamlParseException(String.format("Expected field '%' to be of type %, but it was of type %", fieldName, fieldType, yamlValue.getClass()));
			}
			
			return (T) yamlValue;
		}
		
		if (fieldType == String.class) {
			if (yamlValue.getClass() != String.class) {
				throw new YamlParseException(String.format("Expected field '%' to be of type %, but it was of type %", fieldName, fieldType, yamlValue.getClass()));
			}
			
			return (T) yamlValue;
		}
		
		if (fieldType == Character.TYPE) {
			if (yamlValue.getClass() != String.class) {
				throw new YamlParseException(String.format("Expected field '%' to be of type %, but it was of type %", fieldName, fieldType, yamlValue.getClass()));
			}
			
			return (T) (Object) ((String) yamlValue).charAt(0);
		}
		
		if (List.class.isAssignableFrom(fieldType)) {
			loadListFromYaml(fieldName, fieldType, fieldType.getTypeParameters()[0], yamlValue);
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private <T, L extends List<T>> L loadListFromYaml(String fieldName, Class<L> listType, Class<T> listValueType, Object yamlValue) throws InstantiationException, IllegalAccessException {
		if (!List.class.isAssignableFrom(yamlValue.getClass())) {
			throw new YamlParseException(String.format("Expected field '%' to be a List, but it was of type %", fieldName, yamlValue.getClass()));
		}
		
		List<?> yamlList = (List<?>) yamlValue;
		L resultList = listType.newInstance();
		for (Object yamlListValue : yamlList) {
			T parsedListValue = loadValueFromYamlData(fieldName+" entry", listValueType, yamlListValue);
			resultList.add(parsedListValue);
		}
		
		return resultList;
	}
	
	private String fieldToSetterName(Field field) {
		return "set"+field.getName().substring(0, 1).toUpperCase()+field.getName().substring(1);
	}
}
