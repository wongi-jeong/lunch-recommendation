package kr.io.diduga.lunch_recommendation.dto;

/**
 * 룰렛 결과 저장 요청 DTO.
 */
public class RouletteHistorySaveRequest {

	private String winnerName;
	private String winnerPlaceId;
	private String shareData;

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
}
