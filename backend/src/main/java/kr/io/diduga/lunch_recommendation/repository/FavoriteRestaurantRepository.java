package kr.io.diduga.lunch_recommendation.repository;

import kr.io.diduga.lunch_recommendation.entity.FavoriteRestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 즐겨찾기 식당 저장소.
 */
public interface FavoriteRestaurantRepository extends JpaRepository<FavoriteRestaurantEntity, Long> {

	List<FavoriteRestaurantEntity> findByMemberIdOrderByFavoritedAtDesc(Long memberId);

	Optional<FavoriteRestaurantEntity> findByMemberIdAndRestaurantId(Long memberId, String restaurantId);

	void deleteByMemberIdAndRestaurantId(Long memberId, String restaurantId);
}

