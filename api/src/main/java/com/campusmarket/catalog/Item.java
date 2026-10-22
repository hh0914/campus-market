package com.campusmarket.catalog;

import com.campusmarket.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Item {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(nullable = false, length = 80)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "original_price", precision = 12, scale = 2)
	private BigDecimal originalPrice;

	@Enumerated(EnumType.STRING)
	@Column(name = "condition_level", nullable = false, length = 16)
	private ConditionLevel conditionLevel;

	@Column(length = 50)
	private String campus;

	@Enumerated(EnumType.STRING)
	@Column(name = "trade_method", nullable = false, length = 16)
	private TradeMethod tradeMethod = TradeMethod.MEETUP;

	@Column(length = 200)
	private String tags;

	@Column(name = "cover_url", length = 255)
	private String coverUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ItemStatus status = ItemStatus.DRAFT;

	@Column(name = "reject_reason", length = 200)
	private String rejectReason;

	@Column(name = "view_count", nullable = false)
	private int viewCount;

	@Column(name = "favorite_count", nullable = false)
	private int favoriteCount;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "sold_at")
	private LocalDateTime soldAt;

	@Version
	@Column(nullable = false)
	private Long version;

	@OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<ItemImage> images = new ArrayList<>();

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public void addImage(String url) {
		ItemImage image = new ItemImage();
		image.setItem(this);
		image.setUrl(url);
		image.setSortOrder(images.size());
		images.add(image);
	}

	public void clearImages() {
		images.clear();
	}

	public Long getSellerId() {
		return seller == null ? null : seller.getId();
	}
}
