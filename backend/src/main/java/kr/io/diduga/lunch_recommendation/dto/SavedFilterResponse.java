package kr.io.diduga.lunch_recommendation.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.io.diduga.lunch_recommendation.entity.SavedFilterEntity;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 저장 필터 응답 DTO.
 */
public class SavedFilterResponse {

	private Long id;
	private String name;
	private Integer distance;
	private List<String> foodTypes;
	private Boolean openOnly;
	private Instant createdAt;

	public static SavedFilterResponse fromEntity(SavedFilterEntity entity) {
		SavedFilterResponse r = new SavedFilterResponse();
		r.setId(entity.getId());
		r.setName(entity.getName());
		r.setDistance(entity.getDistance());
		r.setOpenOnly(entity.getOpenOnly());
		r.setCreatedAt(entity.getCreatedAt());
		r.setFoodTypes(parseFoodTypes(entity.getFoodTypesJson()));
		return r;
	}

	private static List<String> parseFoodTypes(String json) {
		if (json == null || json.isBlank()) {
			return Collections.emptyList();
		}
		try {
			return new ObjectMapper().readValue(json, new TypeReference<List<String>>() {});
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getDistance() {
		return distance;
	}

	public void setDistance(Integer distance) {
		this.distance = distance;
	}

	public List<String> getFoodTypes() {
		return foodTypes;
	}

	public void setFoodTypes(List<String> foodTypes) {
		this.foodTypes = foodTypes;
	}

	public Boolean getOpenOnly() {
		return openOnly;
	}

	public void setOpenOnly(Boolean openOnly) {
		this.openOnly = openOnly;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
