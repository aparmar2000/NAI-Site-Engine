package aparmar.naisiteengine.entry;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
public class TagGroupData {
	@JsonProperty(value="group-name", required=true)
	private String name;
	@JsonProperty(value="short-name", required=true)
	private String shortName;
	@JsonProperty(value="generation-context-prefix")
	private String generationContextPrefix = "";
	@JsonProperty(value="max-selected-values")
	private int maxSelectedValues = 1;
	@JsonProperty(value="value-options", required=true)
	@Getter(value = AccessLevel.PRIVATE)
	private String[] values;
	
	public TagEntry[] getTagEntries() {
		return Arrays.stream(values)
				.map(tag->new TagEntry(tag, this))
				.toArray(TagEntry[]::new);
	}
	
	@Data
	public static class TagEntry {
		private final String name;
		private final TagGroupData tagGroup;
		
		public static String toTextGenerationContextForm(TagEntry[] entries) {
			Set<TagGroupData> presentTagGroups = new HashSet<>();
			HashMap<TagGroupData, List<TagEntry>> groupedTagEntries = new HashMap<>();
			for (TagEntry entry : entries) {
				presentTagGroups.add(entry.getTagGroup());
				List<TagEntry> tagList = groupedTagEntries.getOrDefault(entry.getTagGroup(), new LinkedList<>());
				tagList.add(entry);
				groupedTagEntries.put(entry.getTagGroup(), tagList);
			}
			
			TagGroupData[] sortedGroups = presentTagGroups.stream()
				.sorted(Comparator.comparing(TagGroupData::getName))
				.toArray(TagGroupData[]::new);
			
			StringBuilder result = new StringBuilder();
			for (int i=0; i<sortedGroups.length; i++) {
				TagGroupData group = sortedGroups[i];
				
				result.append("[ ");
				result.append(group.getGenerationContextPrefix());
				result.append(": ");
				result.append(groupedTagEntries.get(group).stream()
						.map(TagEntry::getName)
						.collect(Collectors.joining(", ")));
				result.append(" ]");

				result.append("\n");
			}
			
			return result.toString();
		}
	}
}
