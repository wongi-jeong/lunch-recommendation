package kr.io.diduga.lunch_recommendation.controller;

import jakarta.validation.Valid;
import kr.io.diduga.lunch_recommendation.dto.FavoriteRestaurantRequest;
import kr.io.diduga.lunch_recommendation.dto.FavoriteRestaurantResponse;
import kr.io.diduga.lunch_recommendation.service.FavoriteRestaurantService;
import kr.io.diduga.lunch_recommendation.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 즐겨찾기 식당 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteRestaurantController {

	private final FavoriteRestaurantService favoriteRestaurantService;

	public FavoriteRestaurantController(FavoriteRestaurantService favoriteRestaurantService) {
		this.favoriteRestaurantService = favoriteRestaurantService;
	}

	/**
	 * 즐겨찾기 추가/갱신.
	 */
	@PostMapping
	public ResponseEntity<FavoriteRestaurantResponse> addOrUpdate(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@Valid @RequestBody FavoriteRestaurantRequest request) {
		MemberService.InvalidCredentialsException.requireToken(token);
		FavoriteRestaurantResponse saved = favoriteRestaurantService.addOrUpdate(token, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	/**
	 * 즐겨찾기 삭제.
	 */
	@DeleteMapping("/{restaurantId}")
	public ResponseEntity<Void> remove(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@PathVariable("restaurantId") String restaurantId) {
		MemberService.InvalidCredentialsException.requireToken(token);
		favoriteRestaurantService.remove(token, restaurantId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 내 즐겨찾기 목록 조회.
	 */
	@GetMapping("/me")
	public ResponseEntity<List<FavoriteRestaurantResponse>> listMyFavorites(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@RequestParam(value = "limit", required = false) Integer limit) {
		MemberService.InvalidCredentialsException.requireToken(token);
		List<FavoriteRestaurantResponse> list = favoriteRestaurantService.listMyFavorites(token, limit);
		return ResponseEntity.ok(list);
	}

	@ExceptionHandler(MemberService.InvalidCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleInvalidCredentials(MemberService.InvalidCredentialsException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
	}
}

