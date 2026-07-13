package kr.io.diduga.lunch_recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.io.diduga.lunch_recommendation.dto.FavoriteRestaurantRequest;
import kr.io.diduga.lunch_recommendation.dto.FavoriteRestaurantResponse;
import kr.io.diduga.lunch_recommendation.dto.MemberResponse;
import kr.io.diduga.lunch_recommendation.entity.FavoriteRestaurantEntity;
import kr.io.diduga.lunch_recommendation.repository.FavoriteRestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 즐겨찾기 식당 비즈니스 로직.
 */
@Service
public class FavoriteRestaurantService {

	private final FavoriteRestaurantRepository favoriteRestaurantRepository;
	private final MemberService memberService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public FavoriteRestaurantService(FavoriteRestaurantRepository favoriteRestaurantRepository,
			MemberService memberService) {
		this.favoriteRestaurantRepository = favoriteRestaurantRepository;
		this.memberService = memberService;
	}

	private Long getMemberId(String token) {
		MemberResponse me = memberService.getMe(token);
		return me.getId();
	}

	@Transactional
	public FavoriteRestaurantResponse addOrUpdate(String token, FavoriteRestaurantRequest request) {
		if (request == null || request.getId() == null || request.getId().isBlank()) {
			throw new IllegalArgumentException("식당 ID가 필요합니다.");
		}
		Long memberId = getMemberId(token);
		String restaurantId = request.getId().trim();

		String categoriesJson;
		try {
			categoriesJson = objectMapper.writeValueAsString(
					request.getCategories() != null ? request.getCategories() : Collections.emptyList());
		} catch (Exception e) {
			throw new IllegalArgumentException("카테고리 직렬화 실패", e);
		}

		Instant favoritedAt = request.getFavoritedAt() != null
				? Instant.ofEpochMilli(request.getFavoritedAt())
				: Instant.now();

		// DB 제약(nullable, length) 방지를 위한 null/길이 정규화
		String name = request.getName() != null ? request.getName().trim() : "";
		String address = request.getAddress() != null ? request.getAddress() : null;
		String googleMapsUri = request.getGoogleMapsUri() != null ? request.getGoogleMapsUri() : null;
		String photoName = request.getPhotoName();
		if (photoName != null && photoName.length() > 1024) {
			photoName = photoName.substring(0, 1024);
		}

		Optional<FavoriteRestaurantEntity> existingOpt = favoriteRestaurantRepository
				.findByMemberIdAndRestaurantId(memberId, restaurantId);

		FavoriteRestaurantEntity entity;
		if (existingOpt.isPresent()) {
			entity = existingOpt.get();
			entity.setName(name);
			entity.setAddress(address);
			entity.setGoogleMapsUri(googleMapsUri);
			entity.setLatitude(request.getLatitude());
			entity.setLongitude(request.getLongitude());
			entity.setRating(request.getRating());
			entity.setDistanceMeters(request.getDistanceMeters());
			entity.setPhotoName(photoName);
			entity.setCategoriesJson(categoriesJson);
			entity.setFavoritedAt(favoritedAt);
		} else {
			entity = new FavoriteRestaurantEntity(memberId, restaurantId, name, address,
					googleMapsUri, request.getLatitude(), request.getLongitude(), request.getRating(),
					request.getDistanceMeters(), photoName, categoriesJson, favoritedAt);
		}

		FavoriteRestaurantEntity saved = favoriteRestaurantRepository.save(entity);
		return FavoriteRestaurantResponse.fromEntity(saved);
	}

	@Transactional
	public void remove(String token, String restaurantId) {
		Long memberId = getMemberId(token);
		if (restaurantId == null || restaurantId.isBlank()) {
			return;
		}
		favoriteRestaurantRepository.deleteByMemberIdAndRestaurantId(memberId, restaurantId.trim());
	}

	@Transactional(readOnly = true)
	public List<FavoriteRestaurantResponse> listMyFavorites(String token, Integer limit) {
		Long memberId = getMemberId(token);
		List<FavoriteRestaurantEntity> list = favoriteRestaurantRepository
				.findByMemberIdOrderByFavoritedAtDesc(memberId);
		if (limit != null && limit > 0 && list.size() > limit) {
			list = list.subList(0, limit);
		}
		return list.stream().map(FavoriteRestaurantResponse::fromEntity).collect(Collectors.toList());
	}
}

