package kr.io.diduga.lunch_recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 개별 사용자의 투표 기록 엔티티.
 * 로그인 회원/비로그인(익명 ID, fingerprint) 모두를 아우르는 1인 1표 보장을 위해 사용한다.
 */
@Entity
@Table(name = "vote_record", indexes = {
		@Index(name = "idx_vote_record_vote_id", columnList = "vote_id")
})
public class VoteRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "vote_id", nullable = false, length = 32)
	private String voteId;

	@Column(name = "option_index", nullable = false)
	private Integer optionIndex;

	/** 로그인한 회원 ID (없으면 null) */
	@Column(name = "member_id")
	private Long memberId;

	/** 비로그인 사용자의 브라우저 식별용 ID (localStorage/cookie 기반) */
	@Column(name = "anonymous_id", length = 64)
	private String anonymousId;

	/** 브라우저 fingerprint (추가적인 중복 방지용) */
	@Column(name = "fingerprint", length = 128)
	private String fingerprint;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected VoteRecordEntity() {
	}

	public VoteRecordEntity(String voteId, Integer optionIndex, Long memberId, String anonymousId, String fingerprint) {
		this.voteId = voteId;
		this.optionIndex = optionIndex;
		this.memberId = memberId;
		this.anonymousId = anonymousId;
		this.fingerprint = fingerprint;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getVoteId() {
		return voteId;
	}

	public Integer getOptionIndex() {
		return optionIndex;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getAnonymousId() {
		return anonymousId;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

