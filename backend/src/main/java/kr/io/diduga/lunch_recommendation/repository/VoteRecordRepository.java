package kr.io.diduga.lunch_recommendation.repository;

import kr.io.diduga.lunch_recommendation.entity.VoteRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 개별 투표 기록 저장소.
 */
public interface VoteRecordRepository extends JpaRepository<VoteRecordEntity, Long> {

	List<VoteRecordEntity> findByVoteId(String voteId);

	boolean existsByVoteIdAndMemberId(String voteId, Long memberId);

	boolean existsByVoteIdAndAnonymousId(String voteId, String anonymousId);

	boolean existsByVoteIdAndFingerprint(String voteId, String fingerprint);

	Optional<VoteRecordEntity> findFirstByVoteIdAndMemberId(String voteId, Long memberId);

	Optional<VoteRecordEntity> findFirstByVoteIdAndAnonymousId(String voteId, String anonymousId);

	Optional<VoteRecordEntity> findFirstByVoteIdAndFingerprint(String voteId, String fingerprint);
}

