package kr.io.diduga.lunch_recommendation.repository;

import kr.io.diduga.lunch_recommendation.entity.VoteNotificationReadEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 종료된 투표 알림 읽음 저장소.
 */
public interface VoteNotificationReadRepository extends JpaRepository<VoteNotificationReadEntity, String> {

	List<VoteNotificationReadEntity> findByMemberId(Long memberId);

	boolean existsByMemberIdAndVoteId(Long memberId, String voteId);
}
