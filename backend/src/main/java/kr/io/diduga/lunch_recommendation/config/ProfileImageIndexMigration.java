package kr.io.diduga.lunch_recommendation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 앱 기동 시 member 테이블에 profile_image_index 컬럼이 없으면 추가.
 * Hibernate ddl-auto=update가 컬럼을 추가하지 못한 경우 대비.
 */
@Component
@Order(1)
public class ProfileImageIndexMigration implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ProfileImageIndexMigration.class);

	private final JdbcTemplate jdbcTemplate;

	public ProfileImageIndexMigration(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			jdbcTemplate.execute("ALTER TABLE member ADD COLUMN profile_image_index INT DEFAULT 0");
			log.info("member.profile_image_index 컬럼 추가 완료");
		} catch (Exception e) {
			String msg = e.getMessage() != null ? e.getMessage() : "";
			if (msg.contains("Duplicate column") || msg.contains("already exists")) {
				log.debug("member.profile_image_index 컬럼이 이미 존재함");
			} else {
				log.warn("profile_image_index 컬럼 추가 스킵: {}", msg);
			}
		}
	}
}
