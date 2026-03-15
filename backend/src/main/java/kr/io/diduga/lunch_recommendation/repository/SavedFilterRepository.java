package kr.io.diduga.lunch_recommendation.repository;

import kr.io.diduga.lunch_recommendation.entity.SavedFilterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 저장 필터 저장소.
 */
public interface SavedFilterRepository extends JpaRepository<SavedFilterEntity, Long> {

	List<SavedFilterEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
