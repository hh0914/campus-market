package com.campusmarket.trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutJob {

	private static final Logger log = LoggerFactory.getLogger(OrderTimeoutJob.class);

	private final OrderService orderService;

	public OrderTimeoutJob(OrderService orderService) {
		this.orderService = orderService;
	}

	@Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
	public void cancelExpiredOrders() {
		try {
			int cancelled = orderService.cancelExpiredOrders();
			if (cancelled > 0) {
				log.info("超时订单清理完成，共取消 {} 单", cancelled);
			}
		}
		catch (RuntimeException ex) {
			log.error("超时订单清理失败", ex);
		}
	}
}
