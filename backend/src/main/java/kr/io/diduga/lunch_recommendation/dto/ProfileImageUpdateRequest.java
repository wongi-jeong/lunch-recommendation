package kr.io.diduga.lunch_recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 프로필 사진 번호 변경 요청 (0~7).
 */
public class ProfileImageUpdateRequest {

	@Min(0)
	@Max(7)
	private Integer profileImageIndex = 0;

	public Integer getProfileImageIndex() {
		return profileImageIndex != null ? profileImageIndex : 0;
	}

	public void setProfileImageIndex(Integer profileImageIndex) {
		this.profileImageIndex = profileImageIndex != null ? profileImageIndex : 0;
	}
}
