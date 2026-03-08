package kr.io.diduga.lunch_recommendation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO. 비밀번호 형식(영문·숫자·특수문자 조합) 검증은 서비스 계층에서 수행.
 */
public class SignUpRequest {

	@NotBlank(message = "이메일을 입력해주세요")
	@Email(message = "올바른 이메일 형식을 입력해주세요")
	@Size(max = 255)
	private String email;

	@NotBlank(message = "비밀번호를 입력해주세요")
	@Size(min = 8, max = 32, message = "비밀번호는 8자 이상 32자 이하로 입력해주세요")
	private String password;

	public SignUpRequest() {
	}

	public SignUpRequest(String email, String password) {
		this.email = email;
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
