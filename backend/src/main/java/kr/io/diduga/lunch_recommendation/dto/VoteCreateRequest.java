package kr.io.diduga.lunch_recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 투표 생성 요청 DTO.
 */
public class VoteCreateRequest {

	/** 클라이언트에서 생성한 투표 ID (8자 등). 없으면 서버에서 생성 */
	@Size(max = 32)
	private String id;

	@NotBlank(message = "제목을 입력해주세요.")
	@Size(max = 200)
	private String title;

	@Size(max = 20)
	private String timer = "none";

	@NotNull(message = "투표 옵션이 필요합니다.")
	@Size(min = 2, message = "최소 2개 이상의 옵션이 필요합니다.")
	private List<VoteOptionDto> options;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTimer() {
		return timer;
	}

	public void setTimer(String timer) {
		this.timer = timer != null ? timer : "none";
	}

	public List<VoteOptionDto> getOptions() {
		return options;
	}

	public void setOptions(List<VoteOptionDto> options) {
		this.options = options;
	}

	/**
	 * 투표 옵션(식당) 한 건.
	 */
	public static class VoteOptionDto {
		private String name;
		private String googlePlaceId;
		private String address;
		private List<String> photoName;
		private Double rating;
		private Boolean openNow;
		private List<String> categories;
		private Double latitude;
		private Double longitude;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getGooglePlaceId() {
			return googlePlaceId;
		}

		public void setGooglePlaceId(String googlePlaceId) {
			this.googlePlaceId = googlePlaceId;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public List<String> getPhotoName() {
			return photoName;
		}

		public void setPhotoName(List<String> photoName) {
			this.photoName = photoName;
		}

		public Double getRating() {
			return rating;
		}

		public void setRating(Double rating) {
			this.rating = rating;
		}

		public Boolean getOpenNow() {
			return openNow;
		}

		public void setOpenNow(Boolean openNow) {
			this.openNow = openNow;
		}

		public List<String> getCategories() {
			return categories;
		}

		public void setCategories(List<String> categories) {
			this.categories = categories;
		}

		public Double getLatitude() {
			return latitude;
		}

		public void setLatitude(Double latitude) {
			this.latitude = latitude;
		}

		public Double getLongitude() {
			return longitude;
		}

		public void setLongitude(Double longitude) {
			this.longitude = longitude;
		}
	}
}
