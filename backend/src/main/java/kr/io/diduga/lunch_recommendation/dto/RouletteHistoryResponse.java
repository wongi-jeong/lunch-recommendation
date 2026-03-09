package kr.io.diduga.lunch_recommendation.dto;

import java.time.Instant;

import kr.io.diduga.lunch_recommendation.entity.RouletteHistoryEntity;

/**
 * 룰렛 결과 히스토리 응답 DTO.
 */
public class RouletteHistoryResponse {

	private Long id;
	private String winnerName;
	private String winnerPlaceId;
	private String shareData;
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getWinnerName() {
		return winnerName;
	}

	public void setWinnerName(String winnerName) {
		this.winnerName = winnerName;
	}

	public String getWinnerPlaceId() {
		return winnerPlaceId;
	}

	public void setWinnerPlaceId(String winnerPlaceId) {
		this.winnerPlaceId = winnerPlaceId;
	}

	public String getShareData() {
		return shareData;
	}

	public void setShareData(String shareData) {
		this.shareData = shareData;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public static RouletteHistoryResponse fromEntity(RouletteHistoryEntity entity) {
		RouletteHistoryResponse dto = new RouletteHistoryResponse();
		dto.setId(entity.getId());
		dto.setWinnerName(entity.getWinnerName());
		dto.setWinnerPlaceId(entity.getWinnerPlaceId());
		dto.setShareData(entity.getShareData());
		dto.setCreatedAt(entity.getCreatedAt());
		return dto;
	}
}
