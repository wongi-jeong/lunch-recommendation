package kr.io.diduga.lunch_recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.io.diduga.lunch_recommendation.dto.RestaurantDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private static final Logger logger = Logger.getLogger(RestaurantService.class.getName());
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // application.properties 파일의 GOOGLE_MAPS_API_KEY 값을 주입받습니다.
    @Value("${GOOGLE_MAPS_API_KEY}")
    private String googlePlacesApiKey;

    private static final String GOOGLE_PLACES_NEARBY_SEARCH_URL = "https://places.googleapis.com/v1/places:searchNearby";
    private static final String GOOGLE_PLACES_PHOTO_MEDIA_URL = "https://places.googleapis.com/v1/%s/media";
    private static final String GOOGLE_DISTANCE_MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

    // 필드 마스크: 필요한 정보만 요청
    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.location,places.types,places.rating,places.photos.name,places.photos.widthPx,places.photos.heightPx,places.currentOpeningHours.openNow,places.googleMapsUri";

    private static final HttpMethod HTTP_METHOD_POST = HttpMethod.POST;

    /**
     * 필터 카테고리별 Google Places API 타입 매핑
     * 이미지에 제시된 카테고리 구조를 기반으로 구성
     */
    private static final Map<String, Set<String>> FILTER_CATEGORY_MAP = new HashMap<>();
 
    static {
        // 한식
        FILTER_CATEGORY_MAP.put("한식", Set.of(
                "korean_restaurant",
                "barbecue_restaurant",
                "buffet_restaurant"
        ));

        // 일식
        FILTER_CATEGORY_MAP.put("일식", Set.of(
                "japanese_restaurant",
                "ramen_restaurant",
                "sushi_restaurant"
        ));

        // 중식
        FILTER_CATEGORY_MAP.put("중식", Set.of(
                "chinese_restaurant"
        ));

        // 양식
        FILTER_CATEGORY_MAP.put("양식", Set.of(
                "italian_restaurant",
                "french_restaurant",
                "american_restaurant",
                "spanish_restaurant",
                "greek_restaurant",
                "steak_house",
                "cafe"
        ));

        // 아시안
        FILTER_CATEGORY_MAP.put("아시안", Set.of(
                "vietnamese_restaurant",
                "thai_restaurant",
                "indian_restaurant",
                "indonesian_restaurant",
                "mediterranean_restaurant",
                "turkish_restaurant",
                "middle_eastern_restaurant",
                "lebanese_restaurant",
                "brazilian_restaurant",
                "afghani_restaurant",
                "african_restaurant"
        ));

        // 패스트푸드
        FILTER_CATEGORY_MAP.put("패스트푸드", Set.of(
                "fast_food_restaurant",
                "hamburger_restaurant",
                "sandwich_shop",
                "deli",
                "cafeteria",
                "bagel_shop"
        ));

        // 고기
        FILTER_CATEGORY_MAP.put("고기", Set.of(
                "bar_and_grill",
                "barbecue_restaurant",
                "steak_house"
        ));

        // 면/국물
        FILTER_CATEGORY_MAP.put("면/국물", Set.of(
                "ramen_restaurant",
                "noodle_restaurant",
                "vietnamese_restaurant"
                // soup-based는 Google API 타입이 아니므로 내부 태그로 처리 필요
        ));

        // 비건
        FILTER_CATEGORY_MAP.put("비건", Set.of(
                "vegan_restaurant",
                "vegetarian_restaurant",
                "salad_bar"
        ));
    }

    /**
     * 현재 위치 기준 주변 음식점을 조회하고, RestaurantDto 리스트로 변환합니다.
     *
     * @param latitude  위도
     * @param longitude 경도
     * @param radius    검색 반경(미터)
     * @return 주변 식당 정보 DTO 리스트
     */
    public List<RestaurantDto> searchNearbyRestaurants(double latitude, double longitude, int radius) {
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
            String responseJson = restTemplate.exchange(
                    GOOGLE_PLACES_NEARBY_SEARCH_URL,
                    HTTP_METHOD_POST,
                    requestEntity,
                    String.class
            ).getBody();

            return mapToRestaurantDtos(responseJson);
        }
        catch (RestClientException e) {
            // 실제 서비스에서는 로깅 및 예외 변환을 추가하는 것이 좋습니다.
            throw new IllegalStateException("Google Places API 호출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * RestaurantDto 리스트에 대해 거리 계산을 수행합니다.
     * 필터링된 결과에 대해서만 호출하여 Distance Matrix API 호출을 최소화합니다.
     * Place ID를 사용하여 더 정확한 거리 계산을 수행합니다.
     *
     * @param restaurants 거리 계산할 식당 DTO 리스트
     * @param userLatitude 사용자 현재 위치 위도
     * @param userLongitude 사용자 현재 위치 경도
     * @return 거리 정보가 설정된 RestaurantDto 리스트
     */
    public List<RestaurantDto> calculateDistancesForRestaurants(
            List<RestaurantDto> restaurants,
            double userLatitude,
            double userLongitude
    ) {
        if (restaurants == null || restaurants.isEmpty()) {
            return restaurants;
        }

        // 출발지(사용자 위치)와 목적지(식당)를 동일한 형식(좌표)으로 통일하여 혼합 사용 문제 방지
        // Place ID를 사용하면 출발지(좌표)와 목적지(Place ID)가 혼합되어 ZERO_RESULTS가 발생할 수 있음
        // 따라서 출발지와 동일하게 좌표를 사용하여 일관성 유지
        List<DestinationInfo> destinationInfos = new ArrayList<>();
        for (RestaurantDto dto : restaurants) {
            if (dto.getLatitude() != null && dto.getLongitude() != null) {
                // 출발지와 동일한 형식(좌표)으로 통일
                destinationInfos.add(new DestinationInfo(
                        String.format("%.6f,%.6f", dto.getLatitude(), dto.getLongitude()),
                        dto.getLatitude(),
                        dto.getLongitude()
                ));
            } else {
                destinationInfos.add(new DestinationInfo(null, null, null));
            }
        }

        // Distance Matrix API를 사용하여 도보 거리 계산
        logger.info("거리 계산 시작 - 사용자 위치: " + userLatitude + ", " + userLongitude + ", 목적지 수: " + destinationInfos.size());
        DistanceResult distanceResult = calculateWalkingDistancesWithPlaceId(
                userLatitude,
                userLongitude,
                destinationInfos
        );
        logger.info("거리 계산 완료 - 계산된 거리 수: " + distanceResult.distances.size() + ", null이 아닌 거리 수: " + distanceResult.distances.stream().filter(d -> d != null).count());

        // 계산된 거리와 타입을 DTO에 설정
        for (int i = 0; i < restaurants.size(); i++) {
            RestaurantDto dto = restaurants.get(i);
            if (i < distanceResult.distances.size() && distanceResult.distances.get(i) != null) {
                dto.setDistanceMeters(distanceResult.distances.get(i));
                dto.setDistanceType(distanceResult.distanceTypes.get(i));
                logger.fine("거리 설정 완료 - 식당: " + dto.getName() + ", 거리: " + distanceResult.distances.get(i) + "m, 타입: " + distanceResult.distanceTypes.get(i));
            } else {
                logger.warning("거리 설정 실패 - 식당: " + dto.getName() + ", 인덱스: " + i + ", distances.size(): " + distanceResult.distances.size());
            }
        }

        return restaurants;
    }

    /**
     * Google Places API 응답 JSON을 파싱하여 RestaurantDto 리스트로 변환합니다.
     * 거리 계산은 하지 않고 기본 정보만 설정합니다.
     *
     * @param responseJson Google Places API 응답 JSON
     * @return RestaurantDto 리스트 (거리 정보는 null)
     */
    private List<RestaurantDto> mapToRestaurantDtos(String responseJson) {
        List<RestaurantDto> result = new ArrayList<>();

        if (responseJson == null || responseJson.isBlank()) {
            return result;
        }

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode places = root.path("places");

            if (!places.isArray()) {
                return result;
            }

            for (JsonNode placeNode : places) {
                String googlePlaceId = placeNode.path("id").asText(null);
                String name = placeNode.path("displayName").path("text").asText(null);
                String address = placeNode.path("formattedAddress").asText(null);

                BigDecimal rating = null;
                if (placeNode.has("rating")) {
                    // rating 이 정수/실수 모두 올 수 있으므로 문자열로 받아 BigDecimal 로 변환
                    rating = new BigDecimal(placeNode.get("rating").asText());
                }

                // types 배열을 JSON 문자열로 저장
                String placeTypesJson = null;
                JsonNode typesNode = placeNode.path("types");
                if (!typesNode.isMissingNode() && !typesNode.isNull()) {
                    placeTypesJson = objectMapper.writeValueAsString(typesNode);
                }

                // 사진 name 배열 (0번째를 썸네일로 사용)
                List<String> photoNames = new ArrayList<>();
                String thumbnailUrl = null; // v1 API 에서는 바로 URL 이 오지 않아, 추후 별도 API 로 구성

                JsonNode photos = placeNode.path("photos");
                if (photos.isArray()) {
                    for (JsonNode p : photos) {
                        String n = p.path("name").asText(null);
                        if (n != null && !n.isBlank()) {
                            photoNames.add(n);
                        }
                    }
                }

                String googleMapsUri = placeNode.path("googleMapsUri").asText(null);

                // 위치 정보 (위도, 경도) 파싱
                Double latitude = null;
                Double longitude = null;
                JsonNode locationNode = placeNode.path("location");
                if (!locationNode.isMissingNode() && !locationNode.isNull()) {
                    if (locationNode.has("latitude")) {
                        latitude = locationNode.get("latitude").asDouble();
                    }
                    if (locationNode.has("longitude")) {
                        longitude = locationNode.get("longitude").asDouble();
                    }
                }

                RestaurantDto dto = RestaurantDto.of(
                        name,
                        rating,
                        address,
                        placeTypesJson,
                        thumbnailUrl,
                        googlePlaceId,
                        googleMapsUri,
                        photoNames.isEmpty() ? null : photoNames,
                        null,
                        null, // distanceType은 나중에 설정
                        latitude,
                        longitude
                );

                result.add(dto);
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Google Places API 응답 파싱 중 오류가 발생했습니다.", e);
        }

        return result;
    }

    /**
     * 거리 계산 결과를 담는 내부 클래스
     */
    private static class DistanceResult {
        final List<Integer> distances;
        final List<String> distanceTypes;

        DistanceResult(List<Integer> distances, List<String> distanceTypes) {
            this.distances = distances;
            this.distanceTypes = distanceTypes;
        }
    }

    /**
     * 목적지 정보를 담는 내부 클래스 (Place ID 또는 좌표)
     */
    private static class DestinationInfo {
        final String identifier;  // "place_id:..." 또는 "위도,경도"
        final Double latitude;    // 직선 거리 계산용
        final Double longitude;   // 직선 거리 계산용

        DestinationInfo(String identifier, Double latitude, Double longitude) {
            this.identifier = identifier;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    /**
     * 목적지 정보와 원래 인덱스를 함께 저장하는 내부 클래스
     */
    private static class DestinationWithIndex {
        final DestinationInfo destinationInfo;
        final int originalIndex;

        DestinationWithIndex(DestinationInfo destinationInfo, int originalIndex) {
            this.destinationInfo = destinationInfo;
            this.originalIndex = originalIndex;
        }
    }

    /**
     * Google Distance Matrix API를 사용하여 사용자 위치에서 각 목적지까지의 도보 거리를 계산합니다.
     * Place ID를 우선 사용하고, 없으면 좌표를 사용합니다.
     * Distance Matrix API는 최대 25개의 목적지를 한 번에 처리할 수 있으므로, 25개를 초과하는 경우 배치로 나누어 처리합니다.
     * 도보 경로를 찾을 수 없는 경우 직선 거리로 대체합니다.
     *
     * @param userLatitude 사용자 현재 위치 위도
     * @param userLongitude 사용자 현재 위치 경도
     * @param destinationInfos 목적지 정보 리스트 (Place ID 또는 좌표)
     * @return 거리와 타입을 포함한 DistanceResult 객체
     */
    private DistanceResult calculateWalkingDistancesWithPlaceId(
            double userLatitude,
            double userLongitude,
            List<DestinationInfo> destinationInfos
    ) {
        List<Integer> distances = new ArrayList<>();
        List<String> distanceTypes = new ArrayList<>();

        if (destinationInfos == null || destinationInfos.isEmpty()) {
            return new DistanceResult(distances, distanceTypes);
        }

        // 초기화: 모든 거리와 타입을 null로 설정
        for (int i = 0; i < destinationInfos.size(); i++) {
            distances.add(null);
            distanceTypes.add(null);
        }

        // 유효한 목적지와 인덱스를 수집
        List<DestinationWithIndex> validDestinations = new ArrayList<>();
        for (int i = 0; i < destinationInfos.size(); i++) {
            DestinationInfo info = destinationInfos.get(i);
            if (info != null && info.identifier != null && !info.identifier.isBlank()) {
                validDestinations.add(new DestinationWithIndex(info, i));
            }
        }

        if (validDestinations.isEmpty()) {
            logger.warning("유효한 목적지가 없어 거리 계산을 건너뜁니다.");
            return new DistanceResult(distances, distanceTypes);
        }

        // Distance Matrix API는 최대 25개의 목적지를 한 번에 처리할 수 있음
        final int MAX_DESTINATIONS_PER_REQUEST = 25;
        String origin = String.format("%.6f,%.6f", userLatitude, userLongitude);
        
        logger.info("Distance Matrix API 호출 준비 - 출발지: " + origin + ", 유효한 목적지 수: " + validDestinations.size());
        long placeIdCount = validDestinations.stream().filter(d -> d.destinationInfo.identifier.startsWith("place_id:")).count();
        logger.info("Place ID 사용 목적지 수: " + placeIdCount + ", 좌표 사용 목적지 수: " + (validDestinations.size() - placeIdCount));
        logger.info("API 키 존재 여부: " + (googlePlacesApiKey != null && !googlePlacesApiKey.isBlank()));
        if (googlePlacesApiKey == null || googlePlacesApiKey.isBlank()) {
            logger.severe("API 키가 설정되지 않았습니다! GOOGLE_MAPS_API_KEY 환경변수를 확인하세요.");
            return new DistanceResult(distances, distanceTypes);
        }

        // 배치로 나누어 처리
        for (int batchStart = 0; batchStart < validDestinations.size(); batchStart += MAX_DESTINATIONS_PER_REQUEST) {
            int batchEnd = Math.min(batchStart + MAX_DESTINATIONS_PER_REQUEST, validDestinations.size());
            List<DestinationWithIndex> batch = validDestinations.subList(batchStart, batchEnd);

            String responseJson = null;
            try {
                // 배치의 목적지 식별자를 파이프(|)로 구분하여 연결 (Place ID 또는 좌표)
                String destinations = batch.stream()
                        .map(dest -> dest.destinationInfo.identifier)
                        .collect(Collectors.joining("|"));

                // UriComponentsBuilder를 사용하여 URL을 안전하게 구성 (자동 인코딩)
                String url = UriComponentsBuilder.fromUriString(GOOGLE_DISTANCE_MATRIX_URL)
                        .queryParam("origins", origin)
                        .queryParam("destinations", destinations)
                        .queryParam("mode", "walking")
                        .queryParam("language", "ko")
                        .queryParam("key", googlePlacesApiKey)
                        .build()
                        .toUriString();

                logger.info("Distance Matrix API 호출: " + url.replace(googlePlacesApiKey, "***"));
                logger.info("배치 크기: " + batch.size() + ", 출발지: " + origin + ", 목적지: " + destinations);
                logger.info("목적지 형식 - Place ID 사용: " + batch.stream().filter(d -> d.destinationInfo.identifier.startsWith("place_id:")).count() + 
                        "개, 좌표 사용: " + batch.stream().filter(d -> !d.destinationInfo.identifier.startsWith("place_id:")).count() + "개");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                try {
                    responseJson = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            requestEntity,
                            String.class
                    ).getBody();
                    logger.info("Distance Matrix API 응답 받음 - 길이: " + (responseJson != null ? responseJson.length() : 0));
                } catch (Exception e) {
                    logger.severe("Distance Matrix API HTTP 호출 중 예외 발생: " + e.getClass().getName() + " - " + e.getMessage());
                    throw e;
                }

                if (responseJson == null || responseJson.isBlank()) {
                    logger.warning("Distance Matrix API 응답이 비어있습니다.");
                    continue;
                }

                // 응답 파싱
                logger.info("Distance Matrix API 응답 내용 (전체): " + responseJson);
                JsonNode root = objectMapper.readTree(responseJson);
                String status = root.path("status").asText("");
                logger.info("Distance Matrix API 응답 status: " + status);

                if (!"OK".equals(status)) {
                    String errorMessage = root.path("error_message").asText("알 수 없는 오류");
                    logger.severe("Distance Matrix API 오류 - status: " + status + ", error_message: " + errorMessage);
                    // 응답 전체를 로그에 출력하여 디버깅
                    logger.severe("Distance Matrix API 전체 응답: " + responseJson);
                    continue;
                }

                JsonNode rows = root.path("rows");
                if (!rows.isArray() || rows.size() == 0) {
                    logger.warning("Distance Matrix API 응답에 rows가 없습니다.");
                    continue;
                }

                JsonNode elements = rows.get(0).path("elements");
                if (!elements.isArray()) {
                    logger.warning("Distance Matrix API 응답에 elements가 없습니다.");
                    continue;
                }

                // 배치 결과를 원래 인덱스에 매핑
                for (int i = 0; i < batch.size() && i < elements.size(); i++) {
                    DestinationWithIndex destWithIndex = batch.get(i);
                    JsonNode element = elements.get(i);
                    String elementStatus = element.path("status").asText("");
                    
                    // ZERO_RESULTS인 경우 상세 정보 로깅
                    if ("ZERO_RESULTS".equals(elementStatus)) {
                        boolean isPlaceId = destWithIndex.destinationInfo.identifier.startsWith("place_id:");
                        logger.warning("ZERO_RESULTS 발생 - 인덱스: " + destWithIndex.originalIndex + 
                                ", 출발지 형식: 좌표 (" + origin + ")" +
                                ", 목적지 형식: " + (isPlaceId ? "Place ID" : "좌표") + " (" + destWithIndex.destinationInfo.identifier + ")" +
                                ", 혼합 사용 여부: " + (isPlaceId ? "예 (출발지=좌표, 목적지=Place ID)" : "아니오") +
                                ", element: " + element.toString());
                        logger.warning("참고: 출발지와 목적지의 형식이 다를 경우(좌표 vs Place ID) ZERO_RESULTS가 발생할 수 있습니다.");
                    }

                    if ("OK".equals(elementStatus)) {
                        JsonNode distanceNode = element.path("distance");
                        if (!distanceNode.isMissingNode() && distanceNode.has("value")) {
                            int distanceValue = distanceNode.get("value").asInt();
                            distances.set(destWithIndex.originalIndex, distanceValue);
                            distanceTypes.set(destWithIndex.originalIndex, "WALKING");
                            logger.fine("도보 거리 계산 성공: 인덱스 " + destWithIndex.originalIndex + ", 거리 " + distanceValue + "m");
                        } else {
                            logger.warning("Distance Matrix API 응답에 distance.value가 없습니다. 인덱스: " + destWithIndex.originalIndex);
                            // 직선 거리로 대체
                            if (destWithIndex.destinationInfo.latitude != null && destWithIndex.destinationInfo.longitude != null) {
                                int straightLineDistance = calculateStraightLineDistance(
                                        userLatitude, userLongitude, 
                                        String.format("%.6f,%.6f", destWithIndex.destinationInfo.latitude, destWithIndex.destinationInfo.longitude));
                                if (straightLineDistance > 0) {
                                    distances.set(destWithIndex.originalIndex, straightLineDistance);
                                    distanceTypes.set(destWithIndex.originalIndex, "STRAIGHT_LINE");
                                    logger.info("직선 거리로 대체: 인덱스 " + destWithIndex.originalIndex + ", 거리 " + straightLineDistance + "m");
                                }
                            }
                        }
                    } else if ("ZERO_RESULTS".equals(elementStatus)) {
                        // 도보 경로를 찾을 수 없는 경우 직선 거리로 대체
                        logger.info("도보 경로를 찾을 수 없어 직선 거리로 대체: 인덱스 " + destWithIndex.originalIndex);
                        if (destWithIndex.destinationInfo.latitude != null && destWithIndex.destinationInfo.longitude != null) {
                            int straightLineDistance = calculateStraightLineDistance(
                                    userLatitude, userLongitude,
                                    String.format("%.6f,%.6f", destWithIndex.destinationInfo.latitude, destWithIndex.destinationInfo.longitude));
                            if (straightLineDistance > 0) {
                                distances.set(destWithIndex.originalIndex, straightLineDistance);
                                distanceTypes.set(destWithIndex.originalIndex, "STRAIGHT_LINE");
                                logger.info("직선 거리 계산 완료: 인덱스 " + destWithIndex.originalIndex + ", 거리 " + straightLineDistance + "m");
                            }
                        }
                    } else {
                        logger.warning("Distance Matrix API element status가 OK가 아닙니다: " + elementStatus + ", 인덱스: " + destWithIndex.originalIndex);
                        // 다른 오류인 경우에도 직선 거리로 대체 시도
                        if (destWithIndex.destinationInfo.latitude != null && destWithIndex.destinationInfo.longitude != null) {
                            int straightLineDistance = calculateStraightLineDistance(
                                    userLatitude, userLongitude,
                                    String.format("%.6f,%.6f", destWithIndex.destinationInfo.latitude, destWithIndex.destinationInfo.longitude));
                            if (straightLineDistance > 0) {
                                distances.set(destWithIndex.originalIndex, straightLineDistance);
                                distanceTypes.set(destWithIndex.originalIndex, "STRAIGHT_LINE");
                                logger.info("직선 거리로 대체: 인덱스 " + destWithIndex.originalIndex + ", 거리 " + straightLineDistance + "m");
                            }
                        }
                    }
                }

            } catch (RestClientException e) {
                logger.severe("Distance Matrix API 호출 실패: " + e.getClass().getName() + " - " + e.getMessage());
                if (e.getCause() != null) {
                    logger.severe("원인: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
                }
                e.printStackTrace();
                // 배치 처리 실패 시 해당 배치의 거리는 null로 유지
                // 다음 배치는 계속 처리
            } catch (JsonProcessingException e) {
                logger.severe("Distance Matrix API 응답 파싱 실패: " + e.getMessage());
                logger.severe("파싱 실패한 응답 내용: " + (responseJson != null ? responseJson.substring(0, Math.min(500, responseJson.length())) : "null"));
                e.printStackTrace();
                // 배치 처리 실패 시 해당 배치의 거리는 null로 유지
                // 다음 배치는 계속 처리
            } catch (Exception e) {
                logger.severe("Distance Matrix API 처리 중 예상치 못한 오류: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        return new DistanceResult(distances, distanceTypes);
    }

    /**
     * Haversine 공식을 사용하여 두 좌표 간의 직선 거리(미터)를 계산합니다.
     * Distance Matrix API가 ZERO_RESULTS를 반환할 때 대체로 사용됩니다.
     *
     * @param userLatitude 사용자 위도
     * @param userLongitude 사용자 경도
     * @param destinationCoordinate 목적지 좌표 (형식: "위도,경도")
     * @return 직선 거리(미터), 계산 실패 시 0
     */
    private int calculateStraightLineDistance(
            double userLatitude,
            double userLongitude,
            String destinationCoordinate
    ) {
        try {
            // 좌표 파싱
            String[] parts = destinationCoordinate.split(",");
            if (parts.length != 2) {
                logger.warning("잘못된 좌표 형식: " + destinationCoordinate);
                return 0;
            }

            double destLatitude = Double.parseDouble(parts[0].trim());
            double destLongitude = Double.parseDouble(parts[1].trim());

            // Haversine 공식으로 직선 거리 계산
            final double EARTH_RADIUS_METERS = 6371000; // 지구 반지름 (미터)

            double lat1Rad = Math.toRadians(userLatitude);
            double lat2Rad = Math.toRadians(destLatitude);
            double deltaLatRad = Math.toRadians(destLatitude - userLatitude);
            double deltaLonRad = Math.toRadians(destLongitude - userLongitude);

            double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                    + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                    * Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distanceMeters = EARTH_RADIUS_METERS * c;

            return (int) Math.round(distanceMeters);
        } catch (NumberFormatException e) {
            logger.warning("좌표 파싱 실패: " + destinationCoordinate + " - " + e.getMessage());
            return 0;
        } catch (Exception e) {
            logger.warning("직선 거리 계산 실패: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 필터링 정보를 기반으로 RestaurantDto 리스트를 필터링합니다.
     *
     * @param restaurants RestaurantDto 리스트
     * @param filterCategories 필터링할 카테고리 리스트 (예: ["한식", "일식"])
     * @return 필터링된 RestaurantDto 리스트
     */
    public List<RestaurantDto> filterRestaurantsByCategories(List<RestaurantDto> restaurants, List<String> filterCategories) {
        if (filterCategories == null || filterCategories.isEmpty()) {
            return restaurants;
        }

        // 필터 카테고리에 해당하는 모든 Google API 타입을 수집
        Set<String> targetTypes = new HashSet<>();
        for (String category : filterCategories) {
            Set<String> types = FILTER_CATEGORY_MAP.get(category);
            if (types != null) {
                targetTypes.addAll(types);
            }
        }

        if (targetTypes.isEmpty()) {
            return restaurants;
        }

        // placeTypesJson에서 해당 타입이 하나라도 포함된 식당만 필터링
        return restaurants.stream()
                .filter(restaurant -> {
                    String placeTypesJson = restaurant.getPlaceTypesJson();
                    if (placeTypesJson == null || placeTypesJson.isBlank()) {
                        return false;
                    }

                    try {
                        JsonNode typesNode = objectMapper.readTree(placeTypesJson);
                        if (!typesNode.isArray()) {
                            return false;
                        }

                        // placeTypesJson 배열에 targetTypes 중 하나라도 포함되어 있는지 확인
                        for (JsonNode typeNode : typesNode) {
                            String type = typeNode.asText();
                            if (targetTypes.contains(type)) {
                                return true;
                            }
                        }
                        return false;
                    } catch (JsonProcessingException e) {
                        // JSON 파싱 실패 시 해당 식당은 제외
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Google Places API getMedia 를 호출하여 photo name 에 대응하는 이미지 URI 를 조회합니다.
     *
     * @param photoName places.photos[].name (예: places/ChIJ.../photos/...)
     * @return photoUri, 없거나 오류 시 null
     */
    public String getPhotoUri(String photoName) {
        if (photoName == null || photoName.isBlank()) {
            return null;
        }
        String url = String.format(GOOGLE_PLACES_PHOTO_MEDIA_URL + "?maxHeightPx=400&skipHttpRedirect=true", photoName);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", googlePlacesApiKey);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        try {
            String json = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class).getBody();
            if (json == null || json.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(json);
            return root.path("photoUri").asText(null);
        } catch (RestClientException | JsonProcessingException e) {
            return null;
        }
    }

}
