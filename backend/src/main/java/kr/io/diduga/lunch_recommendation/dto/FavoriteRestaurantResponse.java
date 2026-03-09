package kr.io.diduga.lunch_recommendation.dto;

import kr.io.diduga.lunch_recommendation.entity.FavoriteRestaurantEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 즐겨찾기 식당 응답 DTO.
 */
public class FavoriteRestaurantResponse {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
	private Instant favoritedAt;

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

	public Instant getFavoritedAt() {
		return favoritedAt;
	}

	public void setFavoritedAt(Instant favoritedAt) {
		this.favoritedAt = favoritedAt;
	}

	public static FavoriteRestaurantResponse fromEntity(FavoriteRestaurantEntity entity) {
		FavoriteRestaurantResponse dto = new FavoriteRestaurantResponse();
		dto.setId(entity.getRestaurantId());
		dto.setName(entity.getName());
		dto.setAddress(entity.getAddress());
		dto.setGoogleMapsUri(entity.getGoogleMapsUri());
		dto.setLatitude(entity.getLatitude());
		dto.setLongitude(entity.getLongitude());
		dto.setRating(entity.getRating());
		dto.setDistanceMeters(entity.getDistanceMeters());
		dto.setPhotoName(entity.getPhotoName());
		dto.setFavoritedAt(entity.getFavoritedAt());
		List<String> categories = new ArrayList<>();
		try {
			if (entity.getCategoriesJson() != null) {
				categories = OBJECT_MAPPER.readValue(entity.getCategoriesJson(), new TypeReference<List<String>>() {
				});
			}
		} catch (Exception ignored) {
		}
		dto.setCategories(categories);
		return dto;
	}
}

