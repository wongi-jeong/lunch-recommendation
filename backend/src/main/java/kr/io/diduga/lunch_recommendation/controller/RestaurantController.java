package kr.io.diduga.lunch_recommendation.controller;

import kr.io.diduga.lunch_recommendation.dto.RestaurantDto;
import kr.io.diduga.lunch_recommendation.service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * GET /api/restaurants/nearby?lat=37.5665&lng=126.9780&radius=1000
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<RestaurantDto>> getNearbyRestaurants(
            @RequestParam("lat") double latitude,
            @RequestParam("lng") double longitude,
            @RequestParam(name = "radius", defaultValue = "1000") int radius
    ) {
        List<RestaurantDto> restaurants = restaurantService.searchNearbyRestaurants(latitude, longitude, radius);
        return ResponseEntity.ok(restaurants);
    }

}
