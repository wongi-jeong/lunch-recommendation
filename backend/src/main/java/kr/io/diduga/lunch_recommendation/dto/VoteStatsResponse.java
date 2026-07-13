package kr.io.diduga.lunch_recommendation.dto;

import java.util.List;

/**
 * 투표 상세 + 집계 정보 응답 DTO.
 * - vote: 기본 메타데이터 및 옵션
 * - totalVoters: 총 투표자 수
 * - optionCounts: 각 옵션별 투표 수 (index 기준)
 * - myOptionIndex: 현재 사용자가 선택한 옵션 index (없으면 null)
 */
public class VoteStatsResponse {

	private VoteResponse vote;
	private int totalVoters;
	private List<Integer> optionCounts;
	private Integer myOptionIndex;

	public VoteResponse getVote() {
		return vote;
	}

	public void setVote(VoteResponse vote) {
		this.vote = vote;
	}

	public int getTotalVoters() {
		return totalVoters;
	}

	public void setTotalVoters(int totalVoters) {
		this.totalVoters = totalVoters;
	}

	public List<Integer> getOptionCounts() {
		return optionCounts;
	}

	public void setOptionCounts(List<Integer> optionCounts) {
		this.optionCounts = optionCounts;
	}

	public Integer getMyOptionIndex() {
		return myOptionIndex;
	}

	public void setMyOptionIndex(Integer myOptionIndex) {
		this.myOptionIndex = myOptionIndex;
	}
}

