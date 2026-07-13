package kr.io.diduga.lunch_recommendation.controller;

import jakarta.validation.Valid;
import kr.io.diduga.lunch_recommendation.dto.SavedFilterRequest;
import kr.io.diduga.lunch_recommendation.dto.SavedFilterResponse;
import kr.io.diduga.lunch_recommendation.service.MemberService;
import kr.io.diduga.lunch_recommendation.service.SavedFilterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 저장 필터 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/saved-filters")
public class SavedFilterController {

	private final SavedFilterService savedFilterService;

	public SavedFilterController(SavedFilterService savedFilterService) {
		this.savedFilterService = savedFilterService;
	}

	@PostMapping
	public ResponseEntity<SavedFilterResponse> save(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@Valid @RequestBody SavedFilterRequest request) {
		MemberService.InvalidCredentialsException.requireToken(token);
		SavedFilterResponse saved = savedFilterService.save(token, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@GetMapping("/me")
	public ResponseEntity<List<SavedFilterResponse>> listMyFilters(
			@RequestHeader(value = "X-Auth-Token", required = false) String token) {
		MemberService.InvalidCredentialsException.requireToken(token);
		List<SavedFilterResponse> list = savedFilterService.listMyFilters(token);
		return ResponseEntity.ok(list);
	}

	@PutMapping("/{id}")
	public ResponseEntity<SavedFilterResponse> update(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@PathVariable("id") Long id,
			@Valid @RequestBody SavedFilterRequest request) {
		MemberService.InvalidCredentialsException.requireToken(token);
		SavedFilterResponse updated = savedFilterService.update(token, id, request);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@PathVariable("id") Long id) {
		MemberService.InvalidCredentialsException.requireToken(token);
		savedFilterService.delete(token, id);
		return ResponseEntity.noContent().build();
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
