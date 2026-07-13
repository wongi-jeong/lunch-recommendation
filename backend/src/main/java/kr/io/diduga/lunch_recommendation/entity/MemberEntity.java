package kr.io.diduga.lunch_recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 회원 도메인 엔티티. Lombok 없이 명시적으로 코드 작성.
 */
@Entity
@Table(name = "member")
public class MemberEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/** 프로필 사진 번호 (0~7). 기본 0. 기존 행은 null일 수 있음. */
	@Column(name = "profile_image_index")
	private Integer profileImageIndex = 0;

	protected MemberEntity() {
	}

	public MemberEntity(String email, String passwordHash) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.createdAt = Instant.now();
		this.profileImageIndex = 0;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Integer getProfileImageIndex() {
		return profileImageIndex != null ? profileImageIndex : 0;
	}

	public void setProfileImageIndex(Integer profileImageIndex) {
		this.profileImageIndex = profileImageIndex != null ? profileImageIndex : 0;
	}
}
