package kr.io.diduga.lunch_recommendation.service;

import kr.io.diduga.lunch_recommendation.dto.MemberResponse;
import kr.io.diduga.lunch_recommendation.dto.VoteCreateRequest;
import kr.io.diduga.lunch_recommendation.dto.VoteResponse;
import kr.io.diduga.lunch_recommendation.dto.VoteStatsResponse;
import kr.io.diduga.lunch_recommendation.dto.VoteSubmitRequest;
import kr.io.diduga.lunch_recommendation.entity.VoteEntity;
import kr.io.diduga.lunch_recommendation.entity.VoteNotificationReadEntity;
import kr.io.diduga.lunch_recommendation.entity.VoteRecordEntity;
import kr.io.diduga.lunch_recommendation.repository.VoteNotificationReadRepository;
import kr.io.diduga.lunch_recommendation.repository.VoteRecordRepository;
import kr.io.diduga.lunch_recommendation.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 투표 비즈니스 로직.
 */
@Service
public class VoteService {

	private final VoteRepository voteRepository;
	private final MemberService memberService;
	private final VoteRecordRepository voteRecordRepository;
	private final VoteNotificationReadRepository voteNotificationReadRepository;
	private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

	public VoteService(VoteRepository voteRepository, MemberService memberService,
			VoteRecordRepository voteRecordRepository,
			VoteNotificationReadRepository voteNotificationReadRepository) {
		this.voteRepository = voteRepository;
		this.memberService = memberService;
		this.voteRecordRepository = voteRecordRepository;
		this.voteNotificationReadRepository = voteNotificationReadRepository;
	}

	/**
	 * 로그인한 회원으로 투표 생성. id는 요청에 있으면 사용, 없으면 8자 UUID 생성.
	 */
	@Transactional
	public VoteResponse create(String token, VoteCreateRequest request) {
		MemberResponse me = memberService.getMe(token);
		Long memberId = me.getId();

		String id = request.getId() != null && !request.getId().isBlank()
				? request.getId().trim()
				: UUID.randomUUID().toString().replace("-", "").substring(0, 8);

		if (voteRepository.existsById(id)) {
			id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		}

		String optionsJson;
		try {
			optionsJson = objectMapper.writeValueAsString(request.getOptions());
		} catch (Exception e) {
			throw new IllegalArgumentException("옵션 JSON 변환 실패", e);
		}

		VoteEntity entity = new VoteEntity(id, memberId, request.getTitle(), optionsJson,
				request.getTimer() != null ? request.getTimer() : "none");
		VoteEntity saved = voteRepository.save(entity);
		return VoteResponse.fromEntity(saved);
	}

	/**
	 * 개별 투표 참여.
	 * - 로그인 사용자는 memberId 기준으로 1회만 허용
	 * - 비로그인 사용자는 voterId/ fingerprint 기준으로 1회만 허용
	 * 완료 후 최신 집계 정보를 반환.
	 */
	@Transactional
	public VoteStatsResponse submitVote(String token, String voteId, VoteSubmitRequest request) {
		VoteEntity entity = voteRepository.findById(voteId)
				.orElseThrow(() -> new IllegalArgumentException("투표를 찾을 수 없습니다."));

		if (entity.getStatus() == VoteEntity.Status.ENDED) {
			throw new IllegalStateException("이미 종료된 투표입니다.");
		}

		VoteResponse base = VoteResponse.fromEntity(entity);
		List<VoteCreateRequest.VoteOptionDto> options = base.getOptions();
		if (options == null || options.isEmpty()) {
			throw new IllegalStateException("투표 옵션이 존재하지 않습니다.");
		}

		Integer optionIndex = request.getOptionIndex();
		if (optionIndex == null || optionIndex < 0 || optionIndex >= options.size()) {
			throw new IllegalArgumentException("유효하지 않은 옵션입니다.");
		}

		Long memberId = null;
		if (token != null && !token.isBlank()) {
			MemberResponse me = memberService.getMe(token);
			memberId = me.getId();
		}

		String voterId = request.getVoterId();
		String fingerprint = request.getFingerprint();

		boolean exists = false;
		if (memberId != null && voteRecordRepository.existsByVoteIdAndMemberId(voteId, memberId)) {
			exists = true;
		}
		if (!exists && voterId != null && !voterId.isBlank()
				&& voteRecordRepository.existsByVoteIdAndAnonymousId(voteId, voterId)) {
			exists = true;
		}
		if (!exists && fingerprint != null && !fingerprint.isBlank()
				&& voteRecordRepository.existsByVoteIdAndFingerprint(voteId, fingerprint)) {
			exists = true;
		}

		if (exists) {
			throw new IllegalArgumentException("이미 이 투표에 참여하셨습니다.");
		}

		VoteRecordEntity record = new VoteRecordEntity(voteId, optionIndex, memberId, voterId, fingerprint);
		voteRecordRepository.save(record);

		return getStatsInternal(entity, base, token, voterId, fingerprint);
	}

	/**
	 * 투표 상세 및 집계 정보 조회.
	 * - token, voterId, fingerprint 를 모두 고려해 현재 사용자의 선택 옵션 index 도 함께 반환.
	 */
	@Transactional(readOnly = true)
	public VoteStatsResponse getStats(String token, String voteId, String voterId, String fingerprint) {
		VoteEntity entity = voteRepository.findById(voteId)
				.orElseThrow(() -> new IllegalArgumentException("투표를 찾을 수 없습니다."));
		VoteResponse base = VoteResponse.fromEntity(entity);
		return getStatsInternal(entity, base, token, voterId, fingerprint);
	}

	private VoteStatsResponse getStatsInternal(VoteEntity entity, VoteResponse base, String token, String voterId,
			String fingerprint) {
		List<VoteRecordEntity> records = voteRecordRepository.findByVoteId(entity.getId());

		int optionSize = base.getOptions() != null ? base.getOptions().size() : 0;
		List<Integer> optionCounts = new ArrayList<>(Collections.nCopies(optionSize, 0));
		for (VoteRecordEntity r : records) {
			Integer idx = r.getOptionIndex();
			if (idx != null && idx >= 0 && idx < optionSize) {
				optionCounts.set(idx, optionCounts.get(idx) + 1);
			}
		}

		Long memberId = null;
		if (token != null && !token.isBlank()) {
			try {
				MemberResponse me = memberService.getMe(token);
				memberId = me.getId();
			} catch (MemberService.InvalidCredentialsException ex) {
				// 토큰이 유효하지 않은 경우: 로그인 사용자 아님으로 처리
			}
		}

		Integer myIndex = null;
		Optional<VoteRecordEntity> mine = Optional.empty();
		if (memberId != null) {
			mine = voteRecordRepository.findFirstByVoteIdAndMemberId(entity.getId(), memberId);
		}
		if (mine.isEmpty() && voterId != null && !voterId.isBlank()) {
			mine = voteRecordRepository.findFirstByVoteIdAndAnonymousId(entity.getId(), voterId);
		}
		if (mine.isEmpty() && fingerprint != null && !fingerprint.isBlank()) {
			mine = voteRecordRepository.findFirstByVoteIdAndFingerprint(entity.getId(), fingerprint);
		}
		if (mine.isPresent()) {
			myIndex = mine.get().getOptionIndex();
		}

		VoteStatsResponse stats = new VoteStatsResponse();
		stats.setVote(base);
		stats.setTotalVoters(records.size());
		stats.setOptionCounts(optionCounts);
		stats.setMyOptionIndex(myIndex);
		return stats;
	}

	/**
	 * ID로 투표 조회. 링크로 접근한 사용자가 생성자 여부 확인용으로 사용.
	 */
	public Optional<VoteResponse> getById(String id) {
		return voteRepository.findById(id).map(VoteResponse::fromEntity);
	}

	/**
	 * 로그인한 회원의 진행 중인 투표 목록.
	 */
	public List<VoteResponse> getOngoing(String token) {
		MemberResponse me = memberService.getMe(token);
		List<VoteEntity> list = voteRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(me.getId(),
				VoteEntity.Status.IN_PROGRESS);
		return list.stream().map(VoteResponse::fromEntity).collect(Collectors.toList());
	}

	/**
	 * 로그인한 회원의 종료된 투표 목록. 알림 읽음 여부(read) 포함.
	 */
	public List<VoteResponse> getEnded(String token) {
		MemberResponse me = memberService.getMe(token);
		Long memberId = me.getId();
		List<VoteEntity> list = voteRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(memberId,
				VoteEntity.Status.ENDED);
		Set<String> readVoteIds = voteNotificationReadRepository.findByMemberId(memberId).stream()
				.map(VoteNotificationReadEntity::getVoteId)
				.collect(Collectors.toSet());
		return list.stream().map(entity -> {
			VoteResponse dto = VoteResponse.fromEntity(entity);
			dto.setRead(readVoteIds.contains(entity.getId()));
			return dto;
		}).collect(Collectors.toList());
	}

	/**
	 * 종료된 투표 알림을 읽음 처리. 본인이 생성한 투표만 가능.
	 */
	@Transactional
	public void markNotificationRead(String token, String voteId) {
		MemberResponse me = memberService.getMe(token);
		VoteEntity entity = voteRepository.findById(voteId)
				.orElseThrow(() -> new IllegalArgumentException("투표를 찾을 수 없습니다."));
		if (!entity.getMemberId().equals(me.getId())) {
			throw new IllegalArgumentException("본인의 투표 알림만 읽음 처리할 수 있습니다.");
		}
		if (voteNotificationReadRepository.existsByMemberIdAndVoteId(me.getId(), voteId)) {
			return;
		}
		voteNotificationReadRepository.save(new VoteNotificationReadEntity(me.getId(), voteId));
	}

	/**
	 * 투표 종료. 본인 투표만 종료 가능.
	 */
	@Transactional
	public VoteResponse endVote(String token, String voteId) {
		MemberResponse me = memberService.getMe(token);
		VoteEntity entity = voteRepository.findById(voteId)
				.orElseThrow(() -> new IllegalArgumentException("투표를 찾을 수 없습니다."));
		if (!entity.getMemberId().equals(me.getId())) {
			throw new IllegalArgumentException("본인의 투표만 종료할 수 있습니다.");
		}
		if (entity.getStatus() == VoteEntity.Status.ENDED) {
			return VoteResponse.fromEntity(entity);
		}
		entity.setStatus(VoteEntity.Status.ENDED);
		entity.setEndedAt(Instant.now());
		VoteEntity saved = voteRepository.save(entity);
		return VoteResponse.fromEntity(saved);
	}
}
