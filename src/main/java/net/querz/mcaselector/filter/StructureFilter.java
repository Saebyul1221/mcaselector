package net.querz.mcaselector.filter;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class StructureFilter extends TextFilter<List<String>> {

	private static final Map<String, String> validNames = new HashMap<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(StructureFilter.class.getClassLoader().getResourceAsStream("mapping/all_structures.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validNames.put(line.toLowerCase(), line);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_structures.txt", ex);
		}
	}

	public StructureFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private StructureFilter(Operator operator, Comparator comparator, List<String> value) {
		super(FilterType.STRUCTURES, operator, comparator, value);
		setRawValue(String.join(",", value == null ? new ArrayList<>(0) : value));
	}

	@Override
	public boolean contains(List<String> value, ChunkData data) {
		if (data.getRegion() == null || data.getRegion().getData() == null) {
			return false;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		CompoundTag structures = chunkFilter.getStructures(data.getRegion().getData());
		if (structures == null) {
			return false;
		}
		CompoundTag references = structures.getCompoundTag("References");
		if (references == null) {
			return false;
		}
		for (String name : value) {
			Tag<?> structure = references.get(name);
			if (structure == null || structure.valueToString().equals("[]")) {
				structure = references.get(validNames.get(name));
				if (structure == null || structure.valueToString().equals("[]")) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean containsNot(List<String> value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(List<String> value, ChunkData data) {
		if (data.getRegion() == null || data.getRegion().getData() == null) {
			return false;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		CompoundTag structures = chunkFilter.getStructures(data.getRegion().getData());
		if (structures == null) {
			return false;
		}
		CompoundTag references = structures.getCompoundTag("References");
		if (references == null) {
			return false;
		}
		for (String name : getFilterValue()) {
			long[] refs = ValidationHelper.silent(() -> references.getLongArray(name), null);
			if (refs != null && refs.length > 0) {
				return true;
			}
			refs = ValidationHelper.silent(() -> references.getLongArray(validNames.get(name)), null);
			if (refs != null && refs.length > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setFilterValue(String raw) {
		String[] rawStructureNames = raw.replace(" ", "").split(",");
		if (raw.isEmpty() || rawStructureNames.length == 0) {
			setValid(false);
			setValue(null);
		} else {
			for (int i = 0; i < rawStructureNames.length; i++) {
				String name = rawStructureNames[i].toLowerCase();
				if (!validNames.containsKey(rawStructureNames[i]) && (!validNames.containsKey(name) || !validNames.get(name).equals(rawStructureNames[i]))) {
					if (name.startsWith("'") && name.endsWith("'") && name.length() >= 2 && !name.contains("\"")) {
						rawStructureNames[i] = name.substring(1, name.length() - 1);
						continue;
					}
					setValue(null);
					setValid(false);
					return;
				}
				rawStructureNames[i] = name;
			}
			setValid(true);
			setValue(Arrays.asList(rawStructureNames));
			setRawValue(raw);
		}
	}

	@Override
	public String getFormatText() {
		return "<structure>[,<structure>,...]";
	}

	@Override
	public String toString() {
		return "Structures " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public StructureFilter clone() {
	    return new StructureFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}
}
