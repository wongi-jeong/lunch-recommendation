package kr.io.diduga.lunch_recommendation.dto;

import java.math.BigDecimal;

/**
 * 식당 정보를 API 응답으로 전달하기 위한 DTO.
 *
 * - Google Places API 응답을 기반으로 구성
 * - 아직 DB에 저장되지 않은 상태이므로 엔티티의 PK(id)는 포함하지 않음
 */
public class RestaurantDto {

    private String name;               // 식당 이름
    private BigDecimal rating;         // 평점
    private String address;            // 주소
    private String placeTypesJson;     // place types 전체(JSON 문자열)
    private String thumbnailUrl;       // 썸네일 이미지 URL(추후 구현용, 현재는 null 일 수 있음)
    private String googlePlaceId;      // Google Places placeId
    private String googleMapsUri;      // Google Maps 상세 URL
    private String photoName;          // 대표 사진 name
    private Integer distanceMeters;    // 기준 위치와의 거리(미터) - 필요 시 계산해서 세팅

    public RestaurantDto() {
        // Jackson 역직렬화를 위한 기본 생성자
    }

    private RestaurantDto(
            String name,
            BigDecimal rating,
            String address,
            String placeTypesJson,
            String thumbnailUrl,
            String googlePlaceId,
            String googleMapsUri,
            String photoName,
            Integer distanceMeters
    ) {
        this.name = name;
        this.rating = rating;
        this.address = address;
        this.placeTypesJson = placeTypesJson;
        this.thumbnailUrl = thumbnailUrl;
        this.googlePlaceId = googlePlaceId;
        this.googleMapsUri = googleMapsUri;
        this.photoName = photoName;
        this.distanceMeters = distanceMeters;
    }

    public static RestaurantDto of(
            String name,
            BigDecimal rating,
            String address,
            String placeTypesJson,
            String thumbnailUrl,
            String googlePlaceId,
            String googleMapsUri,
            String photoName,
            Integer distanceMeters
    ) {
        return new RestaurantDto(
                name,
                rating,
                address,
                placeTypesJson,
                thumbnailUrl,
                googlePlaceId,
                googleMapsUri,
                photoName,
                distanceMeters
        );
    }

    // ========= getter / setter =========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPlaceTypesJson() {
        return placeTypesJson;
    }

    public void setPlaceTypesJson(String placeTypesJson) {
        this.placeTypesJson = placeTypesJson;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public void setGooglePlaceId(String googlePlaceId) {
        this.googlePlaceId = googlePlaceId;
    }

    public String getGoogleMapsUri() {
        return googleMapsUri;
    }

    public void setGoogleMapsUri(String googleMapsUri) {
        this.googleMapsUri = googleMapsUri;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
}


