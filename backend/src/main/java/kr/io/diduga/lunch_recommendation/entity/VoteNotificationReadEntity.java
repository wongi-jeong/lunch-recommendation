package kr.io.diduga.lunch_recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * 종료된 투표 알림 읽음 여부. 회원별·투표별 1건만 저장.
 */
@Entity
@Table(name = "vote_notification_read", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "member_id", "vote_id" })
})
public class VoteNotificationReadEntity {

	@Id
	@Column(name = "id", nullable = false, length = 36)
	private String id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "vote_id", nullable = false, length = 32)
	private String voteId;

	@Column(name = "read_at", nullable = false)
	private Instant readAt;

	protected VoteNotificationReadEntity() {
	}

	public VoteNotificationReadEntity(Long memberId, String voteId) {
		this.id = UUID.randomUUID().toString();
		this.memberId = memberId;
		this.voteId = voteId;
		this.readAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getVoteId() {
		return voteId;
	}

	public Instant getReadAt() {
		return readAt;
	}
}
