package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.version.VersionController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PaletteFilter extends TextFilter<List<String>> {

	private static final Comparator[] comparators = {
		Comparator.CONTAINS,
		Comparator.CONTAINS_NOT,
		Comparator.INTERSECTS,
		Comparator.EQUAL,
		Comparator.NOT_EQUAL
	};

	public PaletteFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private PaletteFilter(Operator operator, Comparator comparator, List<String> value) {
		super(FilterType.PALETTE, operator, comparator, value);
		setRawValue(String.join(",", value == null ? new ArrayList<>(0) : value));
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public boolean matches(ChunkData data) {
		return switch (getComparator()) {
			case CONTAINS -> contains(value, data);
			case CONTAINS_NOT -> containsNot(value, data);
			case INTERSECTS -> intersects(value, data);
			case EQUAL -> equals(value, data);
			case NOT_EQUAL -> notEquals(value, data);
			default -> false;
		};
	}

	@Override
	public boolean contains(List<String> value, ChunkData data) {
		if (data.getRegion() == null || data.getRegion().getData() == null) {
			return false;
		}
		return VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"))
				.matchBlockNames(data.getRegion().getData(), value);
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
		return VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"))
				.matchAnyBlockName(data.getRegion().getData(), value);
	}

	public boolean equals(List<String> value, ChunkData data) {
		if (data.getRegion() == null || data.getRegion().getData() == null) {
			return false;
		}
		return VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"))
			.paletteEquals(data.getRegion().getData(), value);
	}

	public boolean notEquals(List<String> values, ChunkData data) {
		return !equals(values, data);
	}

	@Override
	public void setFilterValue(String raw) {
		String[] blockNames = TextHelper.parseBlockNames(raw);
		if (blockNames == null) {
			setValid(false);
			setValue(null);
		} else {
			setValid(true);
			setValue(Arrays.asList(blockNames));
			setRawValue(raw);
		}
	}

	@Override
	public String getFormatText() {
		return "<block>[,<block>,...]";
	}

	@Override
	public String toString() {
		return "Palette " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public PaletteFilter clone() {
		return new PaletteFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}
}
