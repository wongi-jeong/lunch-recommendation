package kr.io.diduga.lunch_recommendation.dto;

import kr.io.diduga.lunch_recommendation.entity.VoteEntity;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 투표 응답 DTO.
 */
public class VoteResponse {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private String id;
	private Long creatorMemberId;
	private String title;
	private String status;
	private List<VoteCreateRequest.VoteOptionDto> options;
	private String timer;
	private Instant createdAt;
	private Instant endedAt;
	/** 종료된 투표 알림 읽음 여부. GET /api/votes/me/ended 응답에서만 사용. */
	private Boolean read;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getCreatorMemberId() {
		return creatorMemberId;
	}

	public void setCreatorMemberId(Long creatorMemberId) {
		this.creatorMemberId = creatorMemberId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<VoteCreateRequest.VoteOptionDto> getOptions() {
		return options;
	}

	public void setOptions(List<VoteCreateRequest.VoteOptionDto> options) {
		this.options = options;
	}

	public String getTimer() {
		return timer;
	}

	public void setTimer(String timer) {
		this.timer = timer;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getEndedAt() {
		return endedAt;
	}

	public void setEndedAt(Instant endedAt) {
		this.endedAt = endedAt;
	}

	@JsonProperty("isRead")
	public Boolean getRead() {
		return read;
	}

	public void setRead(Boolean read) {
		this.read = read;
	}

	public static VoteResponse fromEntity(VoteEntity entity) {
		VoteResponse dto = new VoteResponse();
		dto.setId(entity.getId());
		dto.setCreatorMemberId(entity.getMemberId());
		dto.setTitle(entity.getTitle());
		dto.setStatus(entity.getStatus().name());
		dto.setTimer(entity.getTimer());
		dto.setCreatedAt(entity.getCreatedAt());
		dto.setEndedAt(entity.getEndedAt());
		try {
			List<VoteCreateRequest.VoteOptionDto> options = OBJECT_MAPPER.readValue(entity.getOptionsJson(),
					new TypeReference<List<VoteCreateRequest.VoteOptionDto>>() {
					});
			dto.setOptions(options);
		} catch (Exception e) {
			dto.setOptions(List.of());
		}
		return dto;
	}
}
