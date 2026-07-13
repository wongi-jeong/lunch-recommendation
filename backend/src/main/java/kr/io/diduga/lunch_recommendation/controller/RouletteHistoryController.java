package kr.io.diduga.lunch_recommendation.controller;

import java.util.List;

import kr.io.diduga.lunch_recommendation.dto.RouletteHistoryResponse;
import kr.io.diduga.lunch_recommendation.dto.RouletteHistorySaveRequest;
import kr.io.diduga.lunch_recommendation.service.MemberService;
import kr.io.diduga.lunch_recommendation.service.RouletteHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 룰렛 결과 히스토리 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/roulette-history")
public class RouletteHistoryController {

	private final RouletteHistoryService rouletteHistoryService;

	public RouletteHistoryController(RouletteHistoryService rouletteHistoryService) {
		this.rouletteHistoryService = rouletteHistoryService;
	}

	/**
	 * 룰렛 결과 저장. 로그인 필요 (X-Auth-Token).
	 */
	@PostMapping
	public ResponseEntity<RouletteHistoryResponse> save(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@RequestBody RouletteHistorySaveRequest request) {
		MemberService.InvalidCredentialsException.requireToken(token);
		RouletteHistoryResponse saved = rouletteHistoryService.save(token, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	/**
	 * 로그인한 회원의 룰렛 결과 히스토리 목록 조회.
	 */
	@GetMapping("/me")
	public ResponseEntity<List<RouletteHistoryResponse>> getMyHistory(
			@RequestHeader(value = "X-Auth-Token", required = false) String token) {
		MemberService.InvalidCredentialsException.requireToken(token);
		List<RouletteHistoryResponse> list = rouletteHistoryService.getMyHistory(token);
		return ResponseEntity.ok(list);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<AuthController.ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(new AuthController.ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(MemberService.InvalidCredentialsException.class)
	public ResponseEntity<AuthController.ErrorResponse> handleUnauthorized(
			MemberService.InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthController.ErrorResponse(ex.getMessage()));
	}
}
