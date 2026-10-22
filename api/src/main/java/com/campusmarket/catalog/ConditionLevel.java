package com.campusmarket.catalog;

public enum ConditionLevel {
	NEW("全新"),
	LIKE_NEW("几乎全新"),
	GOOD("轻微使用痕迹"),
	FAIR("明显使用痕迹");

	private final String label;

	ConditionLevel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
