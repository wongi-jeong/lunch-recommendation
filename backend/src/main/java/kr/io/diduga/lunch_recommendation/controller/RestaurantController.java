package kr.io.diduga.lunch_recommendation.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import kr.io.diduga.lunch_recommendation.service.RestaurantService;

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
     * GET /api/restaurants/nearby?lat=37.5665&lng=126.9780&radius=1000&keyword=korean
     */
    @GetMapping("/nearby")
    public ResponseEntity<String> getNearbyRestaurants( //
        @RequestParam("lat") double latitude, //
        @RequestParam("lng") double longitude, //
        @RequestParam(name = "radius", defaultValue = "1000") int radius, //
        @RequestParam(name = "keyword", required = false) String keyword //
    ) {
        String responseJson = restaurantService.searchNearbyRestaurants(latitude, longitude, radius, keyword);
        return ResponseEntity.ok(responseJson);
    }

}