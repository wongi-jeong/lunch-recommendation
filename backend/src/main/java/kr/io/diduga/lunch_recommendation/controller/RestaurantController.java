package kr.io.diduga.lunch_recommendation.controller;

import kr.io.diduga.lunch_recommendation.dto.RestaurantDto;
import kr.io.diduga.lunch_recommendation.service.RestaurantService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    /**
     * 주변 음식점 검색 엔드포인트.
     *
     * 예시:
     * GET /api/restaurants/nearby?lat=37.5665&lng=126.9780&radius=1000&categories=한식,일식
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<RestaurantDto>> getNearbyRestaurants(
            @RequestParam("lat") double latitude,
            @RequestParam("lng") double longitude,
            @RequestParam(name = "radius", defaultValue = "1000") int radius,
            @RequestParam(name = "categories", required = false) List<String> filterCategories,
            @RequestParam(name = "excludePlaceIds", required = false) List<String> excludePlaceIds,
            @RequestParam(name = "maxResults", defaultValue = "5") int maxResults
    ) {
        if (filterCategories == null || filterCategories.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // 1. Google Places API 호출 (카테고리 기반 includedTypes로 검색)
        List<RestaurantDto> restaurants = restaurantService.searchNearbyRestaurants(latitude, longitude, radius, filterCategories);
        
        // 2. 카테고리 필터링
        restaurants = restaurantService.filterRestaurantsByCategories(restaurants, filterCategories);
        
        // 3. 이미 표시 중인 식당 제외 (googlePlaceId 기준)
        if (excludePlaceIds != null && !excludePlaceIds.isEmpty()) {
            Set<String> excludeSet = new HashSet<>(excludePlaceIds);
            restaurants = restaurants.stream()
                    .filter(r -> r.getGooglePlaceId() == null || !excludeSet.contains(r.getGooglePlaceId()))
                    .collect(Collectors.toList());
        }
        
        // 4. 필터링된 결과에 대해서만 거리 계산 (Distance Matrix API 호출 최소화)
        restaurants = restaurantService.calculateDistancesForRestaurants(restaurants, latitude, longitude);
        
        // 5. 최대 개수로 제한 (초과 시 랜덤 선정)
        if (restaurants.size() > maxResults) {
            List<RestaurantDto> shuffled = new ArrayList<>(restaurants);
            Collections.shuffle(shuffled);
            restaurants = shuffled.stream().limit(maxResults).collect(Collectors.toList());
        }
        return ResponseEntity.ok(restaurants);
    }

    /**
     * photo name(places.photos[].name)으로 이미지 URI를 조회하고 302 리다이렉트합니다.
     * 프론트에서 img src 로 사용 시 photoName 배열 0번째를 쿼리로 전달합니다.
     */
    @GetMapping("/photo")
    public ResponseEntity<Void> getPhotoRedirect(@RequestParam("name") String photoName) {
        String photoUri = restaurantService.getPhotoUri(photoName);
        if (photoUri == null || photoUri.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(photoUri));
        return ResponseEntity.status(302).headers(headers).build();
    }

}
