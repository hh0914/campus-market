package com.campusmarket.trade;

/**
 * 前端展示用的交易方式视图，避免直接依赖 catalog 包。
 */
public record TradeMethodView(String code, String label) {

	public static TradeMethodView of(com.campusmarket.catalog.TradeMethod method) {
		return new TradeMethodView(method.name(), method.getLabel());
	}
}
