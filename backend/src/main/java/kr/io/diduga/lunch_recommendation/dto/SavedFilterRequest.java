package kr.io.diduga.lunch_recommendation.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 저장 필터 등록 요청 DTO. 필터 이름 최대 12자.
 */
public class SavedFilterRequest {

	/** 필터 표시 이름 (최대 12자) */
	@Size(max = 12, message = "필터 이름은 최대 12글자까지 입력할 수 있어요.")
	private String name;
	/** 거리(m): 300, 800, 1200 */
	private Integer distance;
	/** 음식 종류 ID 목록 */
	private List<String> foodTypes;
	/** 영업중인 가게만 보기 */
	private Boolean openOnly;

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
}
