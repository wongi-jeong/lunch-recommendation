package kr.io.diduga.lunch_recommendation.service;

import kr.io.diduga.lunch_recommendation.dto.MemberResponse;
import kr.io.diduga.lunch_recommendation.dto.VoteCreateRequest;
import kr.io.diduga.lunch_recommendation.dto.VoteResponse;
import kr.io.diduga.lunch_recommendation.entity.VoteEntity;
import kr.io.diduga.lunch_recommendation.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 투표 비즈니스 로직.
 */
@Service
public class VoteService {

	private final VoteRepository voteRepository;
	private final MemberService memberService;
	private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

	public VoteService(VoteRepository voteRepository, MemberService memberService) {
		this.voteRepository = voteRepository;
		this.memberService = memberService;
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
	 * 로그인한 회원의 종료된 투표 목록.
	 */
	public List<VoteResponse> getEnded(String token) {
		MemberResponse me = memberService.getMe(token);
		List<VoteEntity> list = voteRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(me.getId(),
				VoteEntity.Status.ENDED);
		return list.stream().map(VoteResponse::fromEntity).collect(Collectors.toList());
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
		entity.setEndedAt(java.time.Instant.now());
		VoteEntity saved = voteRepository.save(entity);
		return VoteResponse.fromEntity(saved);
	}
}
