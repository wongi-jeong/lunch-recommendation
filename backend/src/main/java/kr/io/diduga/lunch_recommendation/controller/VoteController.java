package kr.io.diduga.lunch_recommendation.controller;

import jakarta.validation.Valid;
import kr.io.diduga.lunch_recommendation.dto.VoteCreateRequest;
import kr.io.diduga.lunch_recommendation.dto.VoteResponse;
import kr.io.diduga.lunch_recommendation.service.MemberService;
import kr.io.diduga.lunch_recommendation.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 투표 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/votes")
public class VoteController {

	private final VoteService voteService;

	public VoteController(VoteService voteService) {
		this.voteService = voteService;
	}

	/**
	 * 투표 생성. 로그인 필요 (X-Auth-Token).
	 */
	@PostMapping
	public ResponseEntity<VoteResponse> create(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@Valid @RequestBody VoteCreateRequest request) {
		MemberService.InvalidCredentialsException.requireToken(token);
		VoteResponse created = voteService.create(token, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	/**
	 * 투표 단건 조회 (생성자 정보 등). 인증 없이 조회 가능.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<VoteResponse> getById(@PathVariable String id) {
		return voteService.getById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * 내 진행 중인 투표 목록.
	 */
	@GetMapping("/me/ongoing")
	public ResponseEntity<List<VoteResponse>> getOngoing(
			@RequestHeader(value = "X-Auth-Token", required = false) String token) {
		MemberService.InvalidCredentialsException.requireToken(token);
		List<VoteResponse> list = voteService.getOngoing(token);
		return ResponseEntity.ok(list);
	}

	/**
	 * 내 종료된 투표 목록.
	 */
	@GetMapping("/me/ended")
	public ResponseEntity<List<VoteResponse>> getEnded(
			@RequestHeader(value = "X-Auth-Token", required = false) String token) {
		MemberService.InvalidCredentialsException.requireToken(token);
		List<VoteResponse> list = voteService.getEnded(token);
		return ResponseEntity.ok(list);
	}

	/**
	 * 투표 종료하기.
	 */
	@PatchMapping("/{id}/end")
	public ResponseEntity<VoteResponse> endVote(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@PathVariable String id) {
		MemberService.InvalidCredentialsException.requireToken(token);
		VoteResponse updated = voteService.endVote(token, id);
		return ResponseEntity.ok(updated);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<AuthController.ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(new AuthController.ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(MemberService.InvalidCredentialsException.class)
	public ResponseEntity<AuthController.ErrorResponse> handleUnauthorized(MemberService.InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthController.ErrorResponse(ex.getMessage()));
	}
}
