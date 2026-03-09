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
 * 회원별 즐겨찾기 식당 엔티티.
 */
@Entity
@Table(name = "favorite_restaurant", indexes = {
		@Index(name = "idx_favorite_member", columnList = "member_id"),
		@Index(name = "idx_favorite_member_restaurant", columnList = "member_id,restaurant_id", unique = true)
})
public class FavoriteRestaurantEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	/** Google Place ID 또는 외부 식별자 */
	@Column(name = "restaurant_id", nullable = false, length = 128)
	private String restaurantId;

	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Column(name = "address", length = 500)
	private String address;

	@Column(name = "google_maps_uri", length = 500)
	private String googleMapsUri;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

	@Column(name = "rating")
	private Double rating;

	@Column(name = "distance_meters")
	private Integer distanceMeters;

	@Column(name = "photo_name", length = 255)
	private String photoName;

	/** 카테고리 문자열 배열(JSON) */
	@Column(name = "categories_json", columnDefinition = "TEXT")
	private String categoriesJson;

	@Column(name = "favorited_at", nullable = false)
	private Instant favoritedAt;

	protected FavoriteRestaurantEntity() {
	}

	public FavoriteRestaurantEntity(Long memberId, String restaurantId, String name, String address,
			String googleMapsUri, Double latitude, Double longitude, Double rating, Integer distanceMeters,
			String photoName, String categoriesJson, Instant favoritedAt) {
		this.memberId = memberId;
		this.restaurantId = restaurantId;
		this.name = name;
		this.address = address;
		this.googleMapsUri = googleMapsUri;
		this.latitude = latitude;
		this.longitude = longitude;
		this.rating = rating;
		this.distanceMeters = distanceMeters;
		this.photoName = photoName;
		this.categoriesJson = categoriesJson;
		this.favoritedAt = favoritedAt != null ? favoritedAt : Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getRestaurantId() {
		return restaurantId;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public String getGoogleMapsUri() {
		return googleMapsUri;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public Double getRating() {
		return rating;
	}

	public Integer getDistanceMeters() {
		return distanceMeters;
	}

	public String getPhotoName() {
		return photoName;
	}

	public String getCategoriesJson() {
		return categoriesJson;
	}

	public Instant getFavoritedAt() {
		return favoritedAt;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setGoogleMapsUri(String googleMapsUri) {
		this.googleMapsUri = googleMapsUri;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}

	public void setDistanceMeters(Integer distanceMeters) {
		this.distanceMeters = distanceMeters;
	}

	public void setPhotoName(String photoName) {
		this.photoName = photoName;
	}

	public void setCategoriesJson(String categoriesJson) {
		this.categoriesJson = categoriesJson;
	}

	public void setFavoritedAt(Instant favoritedAt) {
		this.favoritedAt = favoritedAt;
	}
}

