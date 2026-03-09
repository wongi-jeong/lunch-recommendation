package kr.io.diduga.lunch_recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 투표 참여 요청 DTO.
 * 로그인/비로그인 공통으로 optionIndex 기준으로 한 번만 투표할 수 있다.
 */
public class VoteSubmitRequest {

	/**
	 * 선택한 옵션의 인덱스 (0 기반).
	 * 실제 유효 범위는 서버에서 현재 옵션 개수로 다시 검증한다.
	 */
	@NotNull
	@Min(0)
	@Max(1000)
	private Integer optionIndex;

	/**
	 * 비로그인 사용자의 브라우저 식별 ID (localStorage/cookie 기반).
	 * 로그인 사용자인 경우에도 클라이언트에서 보내주면 중복 방지에 참고한다.
	 */
	private String voterId;

	/**
	 * 브라우저 fingerprint (추가적인 중복 방지용).
	 */
	private String fingerprint;

	public Integer getOptionIndex() {
		return optionIndex;
	}

	public void setOptionIndex(Integer optionIndex) {
		this.optionIndex = optionIndex;
	}

	public String getVoterId() {
		return voterId;
	}

	public void setVoterId(String voterId) {
		this.voterId = voterId;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}
}

