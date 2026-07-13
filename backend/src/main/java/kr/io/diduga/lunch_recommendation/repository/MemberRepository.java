package kr.io.diduga.lunch_recommendation.repository;

import java.util.Optional;
import kr.io.diduga.lunch_recommendation.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

	boolean existsByEmail(String email);

	Optional<MemberEntity> findByEmail(String email);

	/** 프로필 사진 번호만 DB에 반영 (JPQL로 엔티티 기준 갱신) */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE MemberEntity e SET e.profileImageIndex = :idx WHERE e.id = :id")
	int updateProfileImageIndex(@Param("id") Long id, @Param("idx") int idx);
}
