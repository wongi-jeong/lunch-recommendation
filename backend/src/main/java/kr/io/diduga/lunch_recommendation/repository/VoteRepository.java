package kr.io.diduga.lunch_recommendation.repository;

import kr.io.diduga.lunch_recommendation.entity.VoteEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 투표 저장소.
 */
public interface VoteRepository extends JpaRepository<VoteEntity, String> {

	List<VoteEntity> findByMemberIdAndStatusOrderByCreatedAtDesc(Long memberId, VoteEntity.Status status);
}
