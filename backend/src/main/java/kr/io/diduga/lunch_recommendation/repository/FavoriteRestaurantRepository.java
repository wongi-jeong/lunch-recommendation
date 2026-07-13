package kr.io.diduga.lunch_recommendation.repository;

import kr.io.diduga.lunch_recommendation.entity.FavoriteRestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 즐겨찾기 식당 저장소.
 */
public interface FavoriteRestaurantRepository extends JpaRepository<FavoriteRestaurantEntity, Long> {

	List<FavoriteRestaurantEntity> findByMemberIdOrderByFavoritedAtDesc(Long memberId);

	Optional<FavoriteRestaurantEntity> findByMemberIdAndRestaurantId(Long memberId, String restaurantId);

	@Modifying
	@Query("DELETE FROM FavoriteRestaurantEntity e WHERE e.memberId = :memberId AND e.restaurantId = :restaurantId")
	int deleteByMemberIdAndRestaurantId(@Param("memberId") Long memberId, @Param("restaurantId") String restaurantId);

	@Modifying
	@Query("DELETE FROM FavoriteRestaurantEntity e WHERE e.memberId = :memberId")
	int deleteByMemberId(@Param("memberId") Long memberId);
}

