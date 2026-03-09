package kr.io.diduga.lunch_recommendation.dto;

import java.util.List;

/**
 * 즐겨찾기 식당 등록/갱신 요청 DTO.
 */
public class FavoriteRestaurantRequest {

	/** 클라이언트에서 사용하는 식당 ID (Google Place ID 등) */
	private String id;
	private String name;
	private String address;
	private String googleMapsUri;
	private Double latitude;
	private Double longitude;
	private Double rating;
	private Integer distanceMeters;
	private String photoName;
	private List<String> categories;
	/** epoch milli 또는 null */
	private Long favoritedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getGoogleMapsUri() {
		return googleMapsUri;
	}

	public void setGoogleMapsUri(String googleMapsUri) {
		this.googleMapsUri = googleMapsUri;
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

	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}

	public Integer getDistanceMeters() {
		return distanceMeters;
	}

	public void setDistanceMeters(Integer distanceMeters) {
		this.distanceMeters = distanceMeters;
	}

	public String getPhotoName() {
		return photoName;
	}

	public void setPhotoName(String photoName) {
		this.photoName = photoName;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public Long getFavoritedAt() {
		return favoritedAt;
	}

	public void setFavoritedAt(Long favoritedAt) {
		this.favoritedAt = favoritedAt;
	}
}

