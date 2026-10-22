package com.campusmarket;

import com.campusmarket.catalog.Category;
import com.campusmarket.catalog.CategoryRepository;
import com.campusmarket.credit.Reputation;
import com.campusmarket.credit.ReputationRepository;
import com.campusmarket.moderation.Report;
import com.campusmarket.moderation.ReportRepository;
import com.campusmarket.moderation.ReportStatus;
import com.campusmarket.trade.Wallet;
import com.campusmarket.trade.WalletRepository;
import com.campusmarket.user.Role;
import com.campusmarket.user.User;
import com.campusmarket.user.UserRepository;
import com.campusmarket.user.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 核心业务链路回归：注册认证、商品审核、托管交易、评价、举报治理与信用限制。
 * 使用 H2 内存库（Flyway 关闭、JPA create-drop），每个用例结束后回滚。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CampusMarketFlowTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private WalletRepository walletRepository;

	@Autowired
	private ReputationRepository reputationRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registerThenCampusVerification() throws Exception {
		JsonNode registered = call(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerBody("2025001", "新生甲", "fresh1@campus.edu")), null);

		String accessToken = registered.path("accessToken").asText();
		assertThat(accessToken).isNotBlank();
		assertThat(registered.path("user").path("verified").asBoolean()).isFalse();

		JsonNode sent = call(post("/api/v1/auth/verification/send"), accessToken);
		String code = sent.path("code").asText();
		assertThat(code).hasSize(6);

		JsonNode confirmed = call(post("/api/v1/auth/verification/confirm").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"" + code + "\"}"), accessToken);
		assertThat(confirmed.path("verified").asBoolean()).isTrue();

		// 学号重复注册应被拒绝
		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content(registerBody("2025001", "另一个昵称", "other1@campus.edu")))
				.andExpect(status().isConflict());

		// 密码错误应返回 401
		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"account\":\"2025001\",\"password\":\"WrongPass123\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void escrowTradeCompletesAndReviewUpdatesCredit() throws Exception {
		String seller = registerAndVerify("2025002", "卖家甲", "seller1@campus.edu");
		String buyer = registerAndVerify("2025003", "买家乙", "buyer1@campus.edu");
		long sellerId = userIdOfToken(seller);
		long buyerId = userIdOfToken(buyer);

		long itemId = publishApprovedItem(seller, sellerId);
		recharge(buyer, 500);

		JsonNode order = call(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"itemId\":" + itemId + "}"), buyer);
		long orderId = order.path("id").asLong();
		assertThat(order.path("status").asText()).isEqualTo("PENDING_PAY");
		assertThat(order.path("actions").path("pay").asBoolean()).isTrue();

		JsonNode paid = call(post("/api/v1/orders/" + orderId + "/pay"), buyer);
		assertThat(paid.path("status").asText()).isEqualTo("PAID");

		int buyerFrozen = 100;
		assertThat(walletBalance(buyer).path("frozen").decimalValue())
				.isEqualByComparingTo(String.valueOf(buyerFrozen));

		JsonNode shipped = call(post("/api/v1/orders/" + orderId + "/ship"), seller);
		assertThat(shipped.path("status").asText()).isEqualTo("SHIPPED");

		// 非买家不能确认收货
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/confirm").header(HttpHeaders.AUTHORIZATION, "Bearer " + seller))
				.andExpect(status().isForbidden());

		JsonNode completed = call(post("/api/v1/orders/" + orderId + "/confirm"), buyer);
		assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");
		assertThat(completed.path("feeAmount").decimalValue()).isEqualByComparingTo("2.00");
		assertThat(completed.path("sellerIncome").decimalValue()).isEqualByComparingTo("98.00");

		// 资金结算：买家余额 400、冻结 0；卖家余额 98
		JsonNode buyerWallet = walletBalance(buyer);
		assertThat(buyerWallet.path("balance").decimalValue()).isEqualByComparingTo("400.00");
		assertThat(buyerWallet.path("frozen").decimalValue()).isEqualByComparingTo("0.00");
		assertThat(walletBalance(seller).path("balance").decimalValue()).isEqualByComparingTo("98.00");

		// 商品变为已售出
		JsonNode sold = call(get("/api/v1/items/" + itemId), buyer);
		assertThat(sold.path("status").asText()).isEqualTo("SOLD");

		// 买家评价后卖家信用分重新计算为 100
		call(post("/api/v1/orders/" + orderId + "/reviews").contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":5,\"tags\":\"发货迅速,沟通顺畅\",\"comment\":\"书很新，交易顺利\"}"), buyer);
		JsonNode detail = call(get("/api/v1/orders/" + orderId), buyer);
		assertThat(detail.path("reviewedByMe").asBoolean()).isTrue();

		Reputation reputation = reputationRepository.findByUserId(sellerId).orElseThrow();
		assertThat(reputation.getTotalReviews()).isEqualTo(1);
		assertThat(reputation.getCreditScore()).isEqualTo(100);
		assertThat(reputation.getCompletedDeals()).isEqualTo(1);

		// 重复评价被拒绝
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/reviews").header(HttpHeaders.AUTHORIZATION, "Bearer " + buyer)
						.contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
				.andExpect(status().isConflict());
		// 买家信用分不受卖家评价影响（暂无评价）
		assertThat(reputationRepository.findByUserId(buyerId).orElseThrow().getCompletedDeals()).isEqualTo(1);
	}

	@Test
	void refundReturnsEscrowAndRestoresItem() throws Exception {
		String seller = registerAndVerify("2025004", "卖家丙", "seller2@campus.edu");
		String buyer = registerAndVerify("2025005", "买家丁", "buyer2@campus.edu");
		long sellerId = userIdOfToken(seller);

		long itemId = publishApprovedItem(seller, sellerId);
		recharge(buyer, 300);

		long orderId = call(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"itemId\":" + itemId + "}"), buyer).path("id").asLong();
		call(post("/api/v1/orders/" + orderId + "/pay"), buyer);
		call(post("/api/v1/orders/" + orderId + "/refund").contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"临时不需要了\"}"), buyer);

		JsonNode refunded = call(post("/api/v1/orders/" + orderId + "/refund/approve"), seller);
		assertThat(refunded.path("status").asText()).isEqualTo("REFUNDED");

		JsonNode buyerWallet = walletBalance(buyer);
		assertThat(buyerWallet.path("balance").decimalValue()).isEqualByComparingTo("300.00");
		assertThat(buyerWallet.path("frozen").decimalValue()).isEqualByComparingTo("0.00");
		assertThat(walletBalance(seller).path("balance").decimalValue()).isEqualByComparingTo("0.00");

		JsonNode item = call(get("/api/v1/items/" + itemId), seller);
		assertThat(item.path("status").asText()).isEqualTo("ON_SALE");
	}

	@Test
	void reportIsHandledAndLowCreditCannotPublish() throws Exception {
		String reporter = registerAndVerify("2025006", "举报人", "reporter@campus.edu");
		String target = registerAndVerify("2025007", "被举报人", "target@campus.edu");
		long targetId = userIdOfToken(target);

		JsonNode report = call(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON)
				.content("{\"targetType\":\"USER\",\"targetId\":" + targetId
						+ ",\"reasonType\":\"ABUSE\",\"description\":\"在聊天中多次辱骂其他同学\"}"), reporter);
		long reportId = report.path("id").asLong();
		assertThat(report.path("status").asText()).isEqualTo("PENDING");

		String admin = createAdminAndLogin();
		JsonNode claimed = call(post("/api/v1/admin/reports/" + reportId + "/claim"), admin);
		assertThat(claimed.path("status").asText()).isEqualTo("PROCESSING");

		JsonNode resolved = call(post("/api/v1/admin/reports/" + reportId + "/resolve")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"action\":\"WARN\",\"remark\":\"核实存在辱骂行为，予以警告\"}"), admin);
		assertThat(resolved.path("status").asText()).isEqualTo("RESOLVED");

		assertThat(userRepository.findById(targetId).orElseThrow().getStatus()).isEqualTo(UserStatus.WARNED);
		Report stored = reportRepository.findById(reportId).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(ReportStatus.RESOLVED);
		assertThat(stored.getHandlerId()).isNotNull();

		// 普通学生不能处理工单
		mockMvc.perform(post("/api/v1/admin/reports/" + reportId + "/reject")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + reporter)
						.contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"越权尝试\"}"))
				.andExpect(status().isForbidden());

		// 信用分低于阈值时禁止发布商品
		Category category = createCategory();
		Reputation reputation = reputationRepository.findByUserId(targetId).orElseThrow();
		reputation.setCreditScore(50);
		reputationRepository.save(reputation);

		mockMvc.perform(post("/api/v1/items").header(HttpHeaders.AUTHORIZATION, "Bearer " + target)
						.contentType(MediaType.APPLICATION_JSON)
						.content(itemBody(category.getId()).toString()))
				.andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ 辅助

	private JsonNode call(MockHttpServletRequestBuilder builder, String token) throws Exception {
		if (token != null) {
			builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		}
		MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
		assertThat(body.path("code").asInt()).as("业务返回码").isZero();
		return body.path("data");
	}

	private String registerAndVerify(String studentNo, String nickname, String email) throws Exception {
		JsonNode registered = call(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(registerBody(studentNo, nickname, email)), null);
		String token = registered.path("accessToken").asText();
		String code = call(post("/api/v1/auth/verification/send"), token).path("code").asText();
		call(post("/api/v1/auth/verification/confirm").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"" + code + "\"}"), token);
		return token;
	}

	private String registerBody(String studentNo, String nickname, String email) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("studentNo", studentNo);
		node.put("nickname", nickname);
		node.put("email", email);
		node.put("password", "Campus2026");
		return node.toString();
	}

	private long userIdOfToken(String token) throws Exception {
		JsonNode me = call(get("/api/v1/auth/me"), token);
		return me.path("id").asLong();
	}

	private String createAdminAndLogin() throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		User admin = new User();
		admin.setStudentNo("adm" + suffix);
		admin.setEmail("admin" + suffix + "@campus.edu");
		admin.setNickname("测试管理员");
		admin.setPasswordHash(passwordEncoder.encode("Admin2026"));
		admin.setRole(Role.ADMIN);
		admin.setStatus(UserStatus.ACTIVE);
		admin.setVerified(true);
		userRepository.save(admin);
		walletRepository.save(Wallet.forUser(admin.getId()));
		reputationRepository.save(Reputation.forUser(admin.getId()));

		JsonNode login = call(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"account\":\"" + admin.getStudentNo() + "\",\"password\":\"Admin2026\"}"), null);
		return login.path("accessToken").asText();
	}

	private long publishApprovedItem(String sellerToken, long sellerId) throws Exception {
		Category category = createCategory();
		JsonNode created = call(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON)
				.content(itemBody(category.getId()).toString()), sellerToken);
		long itemId = created.path("id").asLong();
		assertThat(created.path("status").asText()).isEqualTo("PENDING_REVIEW");

		String admin = createAdminAndLogin();
		JsonNode approved = call(post("/api/v1/admin/items/" + itemId + "/approve"), admin);
		assertThat(approved.path("status").asText()).isEqualTo("ON_SALE");

		JsonNode all = call(get("/api/v1/items?size=50"), sellerToken);
		JsonNode search = call(get("/api/v1/items").param("keyword", "高等数学"), sellerToken);
		assertThat(all.path("total").asLong()).isGreaterThanOrEqualTo(1);
		assertThat(search.path("total").asLong()).isGreaterThanOrEqualTo(1);
		return itemId;
	}

	private ObjectNode itemBody(long categoryId) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("title", "高等数学教材 同济第七版");
		node.put("description", "九成新，附课堂笔记与真题册，可当面验收后再确认收货。");
		node.put("price", 100.00);
		node.put("originalPrice", 200.00);
		node.put("conditionLevel", "GOOD");
		node.put("categoryId", categoryId);
		node.put("campus", "主校区");
		node.put("tradeMethod", "MEETUP");
		node.put("tags", "教材,高数");
		node.putArray("imageUrls");
		node.put("submitNow", true);
		return node;
	}

	private Category createCategory() {
		Category category = new Category();
		category.setName("测试分类");
		category.setSlug("test-" + UUID.randomUUID().toString().substring(0, 8));
		category.setIcon("📚");
		category.setSortOrder(10);
		category.setEnabled(true);
		return categoryRepository.save(category);
	}

	private void recharge(String token, double amount) throws Exception {
		call(post("/api/v1/wallet/recharge").contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":" + amount + "}"), token);
	}

	private JsonNode walletBalance(String token) throws Exception {
		return call(get("/api/v1/wallet"), token);
	}
}
