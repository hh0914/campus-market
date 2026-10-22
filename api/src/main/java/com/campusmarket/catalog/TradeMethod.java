package com.campusmarket.catalog;

public enum TradeMethod {
	MEETUP("校内面交"),
	EXPRESS("快递寄送");

	private final String label;

	TradeMethod(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
