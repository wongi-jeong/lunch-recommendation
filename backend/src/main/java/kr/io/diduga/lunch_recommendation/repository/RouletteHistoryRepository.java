package kr.io.diduga.lunch_recommendation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.io.diduga.lunch_recommendation.entity.RouletteHistoryEntity;

/**
 * 룰렛 결과 히스토리 저장소.
 */
public interface RouletteHistoryRepository extends JpaRepository<RouletteHistoryEntity, Long> {

	List<RouletteHistoryEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
