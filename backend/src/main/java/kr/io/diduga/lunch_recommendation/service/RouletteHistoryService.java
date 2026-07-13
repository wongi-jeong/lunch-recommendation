package kr.io.diduga.lunch_recommendation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.io.diduga.lunch_recommendation.dto.MemberResponse;
import kr.io.diduga.lunch_recommendation.dto.RouletteHistoryResponse;
import kr.io.diduga.lunch_recommendation.dto.RouletteHistorySaveRequest;
import kr.io.diduga.lunch_recommendation.entity.RouletteHistoryEntity;
import kr.io.diduga.lunch_recommendation.repository.RouletteHistoryRepository;

/**
 * 룰렛 결과 히스토리 비즈니스 로직.
 */
@Service
public class RouletteHistoryService {

	private final RouletteHistoryRepository rouletteHistoryRepository;
	private final MemberService memberService;

	public RouletteHistoryService(RouletteHistoryRepository rouletteHistoryRepository, MemberService memberService) {
		this.rouletteHistoryRepository = rouletteHistoryRepository;
		this.memberService = memberService;
	}

	/**
	 * 로그인한 회원의 룰렛 결과를 저장한다.
	 */
	@Transactional
	public RouletteHistoryResponse save(String token, RouletteHistorySaveRequest request) {
		MemberResponse me = memberService.getMe(token);
		Long memberId = me.getId();

		String winnerName = request.getWinnerName() != null ? request.getWinnerName().trim() : "";
		String winnerPlaceId = request.getWinnerPlaceId();
		String shareData = request.getShareData();
		if (shareData == null || shareData.isBlank()) {
			throw new IllegalArgumentException("shareData가 비어 있습니다.");
		}

		RouletteHistoryEntity entity = new RouletteHistoryEntity(memberId, winnerName, winnerPlaceId, shareData);
		RouletteHistoryEntity saved = rouletteHistoryRepository.save(entity);
		return RouletteHistoryResponse.fromEntity(saved);
	}

	/**
	 * 로그인한 회원의 룰렛 결과 히스토리 목록을 최신순으로 반환한다.
	 */
	public List<RouletteHistoryResponse> getMyHistory(String token) {
		MemberResponse me = memberService.getMe(token);
		List<RouletteHistoryEntity> list = rouletteHistoryRepository.findByMemberIdOrderByCreatedAtDesc(me.getId());
		return list.stream().map(RouletteHistoryResponse::fromEntity).collect(Collectors.toList());
	}
}
