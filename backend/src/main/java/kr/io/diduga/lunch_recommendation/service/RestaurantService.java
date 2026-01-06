package kr.io.diduga.lunch_recommendation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RestaurantService {

    private final RestTemplate restTemplate = new RestTemplate();

    // 로컬 환경변수 GOOGLE_MAPS_API_KEY 에서 API 키를 주입받습니다.
    // (Spring Boot 는 환경변수를 자동으로 Property 로 매핑해 줍니다.)
    @Value("${GOOGLE_MAPS_API_KEY}")
    private String googlePlacesApiKey;

    private static final String GOOGLE_PLACES_NEARBY_SEARCH_URL = "https://places.googleapis.com/v1/places:searchNearby";

    // 필드 마스크: 필요한 정보만 요청
    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.location,places.types,places.rating,places.photos.name,places.photos.widthPx,places.photos.heightPx,places.currentOpeningHours.openNow,places.googleMapsUri";

    private static final HttpMethod HTTP_METHOD_POST = HttpMethod.POST;

    /**
     * 현재 위치 기준 주변 음식점을 조회합니다.
     *
     * @param latitude  위도
     * @param longitude 경도
     * @param radius    검색 반경(미터)
     * @return Google Places API 응답 JSON 문자열
     */
    public String searchNearbyRestaurants(double latitude, double longitude, int radius) {
        // 요청 본문 구성
        Map<String, Object> requestBody = new HashMap<>();
        
        // 위치 제한 설정
        Map<String, Object> locationRestriction = new HashMap<>();
        Map<String, Object> circle = new HashMap<>();
        Map<String, Object> center = new HashMap<>();
        center.put("latitude", latitude);
        center.put("longitude", longitude);
        circle.put("center", center);
        circle.put("radius", radius);
        locationRestriction.put("circle", circle);
        requestBody.put("locationRestriction", locationRestriction);
        
        // 식당 타입 지정
        requestBody.put("includedTypes", new String[]{"restaurant"});
        
        // 한국어 응답
        requestBody.put("languageCode", "ko");
        
        // 최대 결과 개수 (기본값: 10)
        requestBody.put("maxResultCount", 10);

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googlePlacesApiKey);
        headers.set("X-Goog-FieldMask", FIELD_MASK);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            return restTemplate.exchange(
                GOOGLE_PLACES_NEARBY_SEARCH_URL,
                HTTP_METHOD_POST,
                requestEntity,
                String.class
            ).getBody();
        }
        catch (RestClientException e) {
            // 실제 서비스에서는 로깅 및 예외 변환을 추가하는 것이 좋습니다.
            throw new IllegalStateException("Google Places API 호출 중 오류가 발생했습니다.", e);
        }
    }

}