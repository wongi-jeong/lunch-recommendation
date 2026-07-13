package kr.io.diduga.lunch_recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 회원별 저장 필터 엔티티. 필터 이름 최대 12자, 거리/음식종류/영업중 여부 저장.
 */
@Entity
@Table(name = "saved_filter", indexes = {
		@Index(name = "idx_saved_filter_member", columnList = "member_id")
})
public class SavedFilterEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	/** 필터 표시 이름, 최대 12자 */
	@Column(name = "name", nullable = false, length = 12)
	private String name;

	/** 거리(m): 300, 800, 1200 등 */
	@Column(name = "distance", nullable = false)
	private Integer distance = 300;

	/** 음식 종류 ID 배열 JSON (예: ["korean","japanese"]) */
	@Column(name = "food_types_json", columnDefinition = "TEXT")
	private String foodTypesJson;

	@Column(name = "open_only", nullable = false)
	private Boolean openOnly = false;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected SavedFilterEntity() {
	}

	public SavedFilterEntity(Long memberId, String name, Integer distance, String foodTypesJson, Boolean openOnly) {
		this.memberId = memberId;
		this.name = name != null ? name.trim() : "";
		this.distance = distance != null ? distance : 300;
		this.foodTypesJson = foodTypesJson;
		this.openOnly = openOnly != null && openOnly;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name != null && name.length() > 12 ? name.substring(0, 12) : (name != null ? name.trim() : "");
	}

	public Integer getDistance() {
		return distance;
	}

	public void setDistance(Integer distance) {
		this.distance = distance != null ? distance : 300;
	}

	public String getFoodTypesJson() {
		return foodTypesJson;
	}

	public void setFoodTypesJson(String foodTypesJson) {
		this.foodTypesJson = foodTypesJson;
	}

	public Boolean getOpenOnly() {
		return openOnly;
	}

	public void setOpenOnly(Boolean openOnly) {
		this.openOnly = openOnly != null && openOnly;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
