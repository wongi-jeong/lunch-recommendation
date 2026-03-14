package kr.io.diduga.lunch_recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원 탈퇴 요청 DTO. 비밀번호 재확인 필수.
 */
public class WithdrawRequest {

	@NotBlank(message = "비밀번호를 입력해주세요")
	@Size(max = 32)
	private String password;

	/** 탈퇴 사유 코드 (선택). 서비스 개선용 통계에 활용 가능 */
	@Size(max = 50)
	private String reasonCode;

	public WithdrawRequest() {
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(String reasonCode) {
		this.reasonCode = reasonCode;
	}
}
