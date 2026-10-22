package com.campusmarket.moderation;

public enum ReportReason {
	COUNTERFEIT("假冒伪劣"), SCAM("欺诈行为"), ABUSE("辱骂骚扰"), PROHIBITED("违禁物品"), OTHER("其他");

	private final String label;

	ReportReason(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
