package kr.io.diduga.lunch_recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 룰렛 결과 히스토리 엔티티. 로그인한 회원이 공유한 룰렛 결과를 저장한다.
 */
@Entity
@Table(name = "roulette_history")
public class RouletteHistoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "winner_name", nullable = false, length = 200)
	private String winnerName;

	@Column(name = "winner_place_id", length = 64)
	private String winnerPlaceId;

	/** 룰렛 공유 페이지로 복원하기 위한 쿼리 데이터 (URI 인코딩 전의 원본 문자열). */
	@Column(name = "share_data", nullable = false, columnDefinition = "TEXT")
	private String shareData;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected RouletteHistoryEntity() {
	}

	public RouletteHistoryEntity(Long memberId, String winnerName, String winnerPlaceId, String shareData) {
		this.memberId = memberId;
		this.winnerName = winnerName != null ? winnerName : "";
		this.winnerPlaceId = winnerPlaceId;
		this.shareData = shareData != null ? shareData : "";
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getWinnerName() {
		return winnerName;
	}

	public String getWinnerPlaceId() {
		return winnerPlaceId;
	}

	public String getShareData() {
		return shareData;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
