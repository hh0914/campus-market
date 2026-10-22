package com.campusmarket.credit;

public enum CreditBadge {
	ELITE("精英卖家"), RELIABLE("可靠用户"), NORMAL("普通用户"), WATCH("观察期");

	private final String label;

	CreditBadge(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public static CreditBadge of(int score) {
		if (score >= 95) {
			return ELITE;
		}
		if (score >= 85) {
			return RELIABLE;
		}
		if (score >= 70) {
			return NORMAL;
		}
		return WATCH;
	}
}
