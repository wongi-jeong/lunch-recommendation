package kr.io.diduga.lunch_recommendation.service;

import jakarta.persistence.EntityManager;
import kr.io.diduga.lunch_recommendation.dto.LoginRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kr.io.diduga.lunch_recommendation.dto.MemberResponse;
import kr.io.diduga.lunch_recommendation.dto.SignUpRequest;
import kr.io.diduga.lunch_recommendation.entity.MemberEntity;
import kr.io.diduga.lunch_recommendation.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 비즈니스 로직 처리 서비스.
 */
@Service
public class MemberService {

	private static final Logger log = LoggerFactory.getLogger(MemberService.class);

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final EntityManager entityManager;

	public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
			EntityManager entityManager) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.entityManager = entityManager;
	}

	private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?";

	private static boolean isPasswordFormatValid(String password) {
		if (password == null || password.length() < 8 || password.length() > 32) {
			return false;
		}
		boolean hasLetter = password.chars().anyMatch(Character::isLetter);
		boolean hasDigit = password.chars().anyMatch(Character::isDigit);
		boolean hasSpecial = password.chars().anyMatch(ch -> SPECIAL_CHARS.indexOf(ch) >= 0);
		return (hasLetter && hasDigit) || (hasLetter && hasSpecial) || (hasDigit && hasSpecial);
	}

	/**
	 * 회원가입 처리. 이메일 중복 시 예외를 던진다.
	 *
	 * @param request
	 *            회원가입 요청
	 * @return 저장된 회원 정보
	 */
	@Transactional
	public MemberResponse signUp(SignUpRequest request) {
		if (memberRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateEmailException("이미 등록된 이메일입니다.");
		}
		if (!isPasswordFormatValid(request.getPassword())) {
			throw new InvalidPasswordException("영문 대소문자/숫자/특수문자 중 2가지 이상 조합으로 8~32자로 입력해주세요.");
		}

		String encodedPassword = passwordEncoder.encode(request.getPassword());
		MemberEntity entity = new MemberEntity(request.getEmail(), encodedPassword);
		MemberEntity saved = memberRepository.save(entity);

		return toResponse(saved);
	}

	/**
	 * 로그인 처리. 이메일·비밀번호 불일치 시 예외를 던진다.
	 *
	 * @param request 로그인 요청
	 * @return 회원 정보
	 */
	public MemberResponse login(LoginRequest request) {
		MemberEntity entity = memberRepository.findByEmail(request.getEmail().trim())
				.orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));
		if (!passwordEncoder.matches(request.getPassword(), entity.getPasswordHash())) {
			throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
		}
		log.debug("로그인 memberId={} profileImageIndex={}", entity.getId(), entity.getProfileImageIndex());
		return toResponse(entity);
	}

	/**
	 * 로그인한 회원 정보 조회. token은 로그인 시 저장한 회원 ID(문자열) 또는 이메일.
	 *
	 * @param token 세션에 저장된 authToken (회원 ID 또는 이메일)
	 * @return 회원 정보
	 */
	public MemberResponse getMe(String token) {
		MemberEntity entity = getEntityByToken(token);
		return toResponse(entity);
	}

	/**
	 * 로그인한 회원의 프로필 사진 번호 변경 (0~7). 네이티브 UPDATE 후 DB에서 재조회해 응답(재로그인 시 유지).
	 */
	@Transactional
	public MemberResponse updateProfileImage(String token, int profileImageIndex) {
		MemberEntity entity = getEntityByToken(token);
		Long id = entity.getId();
		int index = Math.max(0, Math.min(7, profileImageIndex));
		int updated = memberRepository.updateProfileImageIndex(id, index);
		log.debug("프로필 이미지 변경 memberId={} index={} updateCount={}", id, index, updated);
		entityManager.clear();
		MemberEntity reloaded = memberRepository.findById(id)
				.orElseThrow(() -> new InvalidCredentialsException("회원 정보를 찾을 수 없습니다."));
		int saved = reloaded.getProfileImageIndex();
		log.debug("프로필 이미지 재조회 memberId={} profileImageIndex={}", id, saved);
		return toResponse(reloaded);
	}

	private MemberEntity getEntityByToken(String token) {
		if (token == null || token.isBlank()) {
			throw new InvalidCredentialsException("인증 정보가 없습니다.");
		}
		String trimmed = token.trim();
		MemberEntity entity = null;
		try {
			Long id = Long.parseLong(trimmed);
			entity = memberRepository.findById(id).orElse(null);
		} catch (NumberFormatException ignored) {
		}
		if (entity == null) {
			entity = memberRepository.findByEmail(trimmed).orElse(null);
		}
		if (entity == null) {
			throw new InvalidCredentialsException("인증 정보가 올바르지 않습니다.");
		}
		return entity;
	}

	private MemberResponse toResponse(MemberEntity entity) {
		return new MemberResponse(
				entity.getId(),
				entity.getEmail(),
				entity.getCreatedAt(),
				entity.getProfileImageIndex());
	}

	/** 이메일 중복 시 던지는 비즈니스 예외. */
	public static class DuplicateEmailException extends RuntimeException {
		public DuplicateEmailException(String message) {
			super(message);
		}
	}

	/** 비밀번호 형식 불일치 시 던지는 비즈니스 예외. */
	public static class InvalidPasswordException extends RuntimeException {
		public InvalidPasswordException(String message) {
			super(message);
		}
	}

	/** 로그인 실패(이메일 없음 또는 비밀번호 불일치) 시 던지는 비즈니스 예외. */
	public static class InvalidCredentialsException extends RuntimeException {
		public InvalidCredentialsException(String message) {
			super(message);
		}

		/** 토큰이 없거나 비어 있으면 예외 발생. API에서 로그인 필수 시 사용. */
		public static void requireToken(String token) {
			if (token == null || token.isBlank()) {
				throw new InvalidCredentialsException("로그인이 필요합니다.");
			}
		}
	}
}
