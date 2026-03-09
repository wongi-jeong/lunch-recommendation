package kr.io.diduga.lunch_recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 투표 도메인 엔티티. 로그인한 회원이 생성한 투표를 저장한다.
 */
@Entity
@Table(name = "vote")
public class VoteEntity {

	public enum Status {
		IN_PROGRESS,
		ENDED
	}

	@Id
	@Column(name = "id", nullable = false, length = 32)
	private String id;

	@Column(name = "creator_member_id", nullable = false)
	private Long memberId;

	/** DB에 member_id 컬럼도 있으면 동일 값으로 채움 (스키마 호환) */
	@Column(name = "member_id", nullable = false)
	private Long memberIdSynced;

	@Column(nullable = false, length = 200)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.IN_PROGRESS;

	/** 투표 옵션(식당 목록) JSON 문자열 */
	@Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
	private String optionsJson;

	@Column(name = "timer", length = 20)
	private String timer;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "ended_at")
	private Instant endedAt;

	protected VoteEntity() {
	}

	public VoteEntity(String id, Long memberId, String title, String optionsJson, String timer) {
		this.id = id;
		this.memberId = memberId;
		this.memberIdSynced = memberId;
		this.title = title;
		this.optionsJson = optionsJson;
		this.timer = timer != null ? timer : "none";
		this.createdAt = Instant.now();
		this.status = Status.IN_PROGRESS;
	}

	public String getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public Long getMemberIdSynced() {
		return memberIdSynced;
	}

	public void setMemberIdSynced(Long memberIdSynced) {
		this.memberIdSynced = memberIdSynced;
	}

	public String getTitle() {
		return title;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getOptionsJson() {
		return optionsJson;
	}

	public String getTimer() {
		return timer;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getEndedAt() {
		return endedAt;
	}

	public void setEndedAt(Instant endedAt) {
		this.endedAt = endedAt;
	}
}
