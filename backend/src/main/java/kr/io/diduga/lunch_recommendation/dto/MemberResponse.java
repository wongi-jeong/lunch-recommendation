package kr.io.diduga.lunch_recommendation.dto;

import java.time.Instant;

/**
 * 회원 정보 응답 DTO. 비밀번호는 절대 노출하지 않음.
 */
public class MemberResponse {

	private Long id;
	private String email;
	private Instant createdAt;
	private Integer profileImageIndex;

	public MemberResponse() {
	}

	public MemberResponse(Long id, String email, Instant createdAt) {
		this.id = id;
		this.email = email;
		this.createdAt = createdAt;
		this.profileImageIndex = 0;
	}

	public MemberResponse(Long id, String email, Instant createdAt, Integer profileImageIndex) {
		this.id = id;
		this.email = email;
		this.createdAt = createdAt;
		this.profileImageIndex = profileImageIndex != null ? profileImageIndex : 0;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Integer getProfileImageIndex() {
		return profileImageIndex != null ? profileImageIndex : 0;
	}

	public void setProfileImageIndex(Integer profileImageIndex) {
		this.profileImageIndex = profileImageIndex != null ? profileImageIndex : 0;
	}
}
