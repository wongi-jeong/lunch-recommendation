package kr.io.diduga.lunch_recommendation.controller;

import jakarta.validation.Valid;
import kr.io.diduga.lunch_recommendation.dto.LoginRequest;
import kr.io.diduga.lunch_recommendation.dto.MemberResponse;
import kr.io.diduga.lunch_recommendation.dto.ProfileImageUpdateRequest;
import kr.io.diduga.lunch_recommendation.dto.SignUpRequest;
import kr.io.diduga.lunch_recommendation.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API 컨트롤러 (회원가입, 로그인).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final MemberService memberService;

	public AuthController(MemberService memberService) {
		this.memberService = memberService;
	}

	/**
	 * 회원가입 API. 성공 시 201 Created, 회원 정보 반환.
	 */
	@PostMapping("/signup")
	public ResponseEntity<MemberResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		MemberResponse member = memberService.signUp(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(member);
	}

	/**
	 * 로그인 API. 성공 시 200 OK, 회원 정보 반환.
	 */
	@PostMapping("/login")
	public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request) {
		MemberResponse member = memberService.login(request);
		return ResponseEntity.ok(member);
	}

	/**
	 * 로그인한 회원 정보 조회. 헤더 X-Auth-Token에 로그인 시 저장한 값(회원 ID 또는 이메일) 전달.
	 */
	@GetMapping("/me")
	public ResponseEntity<MemberResponse> me(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
		MemberResponse member = memberService.getMe(token);
		return ResponseEntity.ok(member);
	}

	/**
	 * 로그인한 회원의 프로필 사진 번호 변경 (0~7). 성공 시 변경된 회원 정보 반환.
	 */
	@PatchMapping("/me")
	public ResponseEntity<MemberResponse> updateProfileImage(
			@RequestHeader(value = "X-Auth-Token", required = false) String token,
			@Valid @RequestBody ProfileImageUpdateRequest request) {
		MemberResponse member = memberService.updateProfileImage(token, request.getProfileImageIndex());
		return ResponseEntity.ok(member);
	}

	@ExceptionHandler(MemberService.InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(MemberService.InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(MemberService.DuplicateEmailException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateEmail(MemberService.DuplicateEmailException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(MemberService.InvalidPasswordException.class)
	public ResponseEntity<ErrorResponse> handleInvalidPassword(MemberService.InvalidPasswordException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
	}

	@ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			org.springframework.web.bind.MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage()).reduce((a, b) -> a + "; " + b)
				.orElse("요청 유효성 검사 실패");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
	}

	public record ErrorResponse(String message) {
	}
}
