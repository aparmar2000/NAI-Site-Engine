package aparmar.naisiteengine.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public class LinkBuilder {
	private final StringBuilder linkStringBuilder;
	private boolean hasParams = false;
	
	public LinkBuilder(String pageLink) {
		linkStringBuilder = new StringBuilder(pageLink);
	}
	
	public void addParam(String name, String value) {
		if (!hasParams) {
			linkStringBuilder.append("?");
			hasParams = true;
		} else {
			linkStringBuilder.append("&");
		}
		
		linkStringBuilder.append(name);
		linkStringBuilder.append("=");
		linkStringBuilder.append(value);
	}
	
	public void addParam(String name, Object value) {
		addParam(name, value.toString());
	}
	
	public void addMultiValueParam(String name, Stream<Object> values) {
		values.forEach(v->addParam(name, v));
	}
	
	public void addMultiValueParam(String name, Object[] values) {
		addMultiValueParam(name, Arrays.stream(values));
	}
	
	public void addMultiValueParam(String name, Collection<Object> values) {
		addMultiValueParam(name, values.stream());
	}
	
	public String build() { return linkStringBuilder.toString(); }
}
