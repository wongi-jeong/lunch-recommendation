package kr.io.diduga.lunch_recommendation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인 요청 DTO.
 */
public class LoginRequest {

	@NotBlank(message = "이메일을 입력해주세요")
	@Email(message = "올바른 이메일 형식을 입력해주세요")
	@Size(max = 255)
	private String email;

	@NotBlank(message = "비밀번호를 입력해주세요")
	@Size(max = 32)
	private String password;

	public LoginRequest() {
	}

	public LoginRequest(String email, String password) {
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
