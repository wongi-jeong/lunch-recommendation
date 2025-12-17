package kr.io.diduga.lunch_recommendation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class RestaurantService {

    private final RestTemplate restTemplate = new RestTemplate();

    // 로컬 환경변수 GOOGLE_MAPS_API_KEY 에서 API 키를 주입받습니다.
    // (Spring Boot 는 환경변수를 자동으로 Property 로 매핑해 줍니다.)
    @Value("${GOOGLE_MAPS_API_KEY}")
    private String googlePlacesApiKey;

    private static final String GOOGLE_PLACES_NEARBY_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

    /**
     * 현재 위치 기준 주변 음식점을 조회합니다.
     *
     * @param latitude  위도
     * @param longitude 경도
     * @param radius    검색 반경(미터)
     * @param keyword   검색 키워드(예: "korean", "sushi") - 없으면 null 또는 빈 문자열
     * @return Google Places API 응답 JSON 문자열
     */
    public String searchNearbyRestaurants(double latitude, double longitude, int radius, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder //
            .fromUriString(GOOGLE_PLACES_NEARBY_SEARCH_URL) //
            .queryParam("location", latitude + "," + longitude) //
            .queryParam("radius", radius) //
            .queryParam("type", "restaurant") //
            .queryParam("key", googlePlacesApiKey);

        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("keyword", keyword);
        }

        String url = builder.toUriString();

        try {
            return restTemplate.getForObject(url, String.class);
        }
        catch (RestClientException e) {
            // 실제 서비스에서는 로깅 및 예외 변환을 추가하는 것이 좋습니다.
            throw new IllegalStateException("Google Places API 호출 중 오류가 발생했습니다.", e);
        }
    }

}