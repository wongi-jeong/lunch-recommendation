package kr.io.diduga.lunch_recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;

/**
 * 식당 도메인 엔티티.
 *
 * Lombok 없이 명시적으로 코드 작성.
 * JPA 관례에 맞춰 불변에 가깝게 설계하고, 값 수정을 최소화한다.
 */
@Entity
@Table(name = "restaurant")
public class RestaurantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 식당 ID (PK)

    @Column(nullable = false, length = 100)
    private String name; // 이름

    // 표현 가능한 값의 범위는 -9.9 ~ 9.9 이며, 소수점 첫째 자리까지 표현할 수 있다.
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating; // 평점 (예: 4.5)

    @Column(name = "address", nullable = false, length = 255)
    private String address; // 가게 위치(주소)

    @Column(name = "food_type", nullable = false, length = 50)
    private String foodType; // 음식 종류 (예: 한식, 일식, 양식 등)

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl; // 썸네일 이미지 링크(혹은 외부 링크)

    /**
     * 내 현재 위치와의 거리 (미터 단위 등).
     * - 요청 시 동적으로 계산되는 값이므로 DB에는 저장하지 않고 조회 응답용으로만 사용.
     */
    @Transient
    private Integer distanceMeters;

    /**
     * 엔티티 생성용 생성자.
     * id 는 영속화 시점에 생성되므로 제외한다.
     */
    private RestaurantEntity(
            String name,
            BigDecimal rating,
            String address,
            String foodType,
            String thumbnailUrl
    ) {
        this.name = name;
        this.rating = rating;
        this.address = address;
        this.foodType = foodType;
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * 정적 팩토리 메서드.
     * 생성 시에만 필수 값들을 세팅하고 이후에는 조회 전용으로 사용하는 패턴.
     */
    public static RestaurantEntity create(
            String name,
            BigDecimal rating,
            String address,
            String foodType,
            String thumbnailUrl
    ) {
        return new RestaurantEntity(name, rating, address, foodType, thumbnailUrl);
    }

    // ========= getter 들 =========

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public String getAddress() {
        return address;
    }

    public String getFoodType() {
        return foodType;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    /**
     * 거리 필드는 응답 시 계산해서 세팅하는 용도이므로, setter 를 최소한으로 노출.
     * 도메인 규칙 상 필요하다면 별도 서비스/도메인 메서드에서만 사용하도록 패키지 수준으로 두는 것도 가능.
     */
    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
}