package kr.io.diduga.lunch_recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.io.diduga.lunch_recommendation.dto.MemberResponse;
import kr.io.diduga.lunch_recommendation.dto.SavedFilterRequest;
import kr.io.diduga.lunch_recommendation.dto.SavedFilterResponse;
import kr.io.diduga.lunch_recommendation.entity.SavedFilterEntity;
import kr.io.diduga.lunch_recommendation.repository.SavedFilterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 저장 필터 비즈니스 로직. 필터 이름 최대 12자.
 */
@Service
public class SavedFilterService {

	private static final int MAX_NAME_LENGTH = 12;
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final SavedFilterRepository savedFilterRepository;
	private final MemberService memberService;

	public SavedFilterService(SavedFilterRepository savedFilterRepository, MemberService memberService) {
		this.savedFilterRepository = savedFilterRepository;
		this.memberService = memberService;
	}

	private Long getMemberId(String token) {
		MemberResponse me = memberService.getMe(token);
		return me.getId();
	}

	@Transactional
	public SavedFilterResponse save(String token, SavedFilterRequest request) {
		Long memberId = getMemberId(token);
		String name = request.getName() != null ? request.getName().trim() : "";
		if (name.isEmpty()) {
			throw new IllegalArgumentException("필터 이름을 입력해주세요.");
		}
		if (name.length() > MAX_NAME_LENGTH) {
			name = name.substring(0, MAX_NAME_LENGTH);
		}
		Integer distance = request.getDistance() != null ? request.getDistance() : 300;
		Boolean openOnly = Boolean.TRUE.equals(request.getOpenOnly());
		String foodTypesJson;
		try {
			List<String> types = request.getFoodTypes() != null ? request.getFoodTypes() : Collections.emptyList();
			foodTypesJson = objectMapper.writeValueAsString(types);
		} catch (Exception e) {
			throw new IllegalArgumentException("음식 종류 데이터 처리 실패", e);
		}
		SavedFilterEntity entity = new SavedFilterEntity(memberId, name, distance, foodTypesJson, openOnly);
		SavedFilterEntity saved = savedFilterRepository.save(entity);
		return SavedFilterResponse.fromEntity(saved);
	}

	@Transactional(readOnly = true)
	public List<SavedFilterResponse> listMyFilters(String token) {
		Long memberId = getMemberId(token);
		return savedFilterRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
				.stream()
				.map(SavedFilterResponse::fromEntity)
				.collect(Collectors.toList());
	}

	@Transactional
	public SavedFilterResponse update(String token, Long id, SavedFilterRequest request) {
		Long memberId = getMemberId(token);
		SavedFilterEntity entity = savedFilterRepository.findById(id).orElse(null);
		if (entity == null || !entity.getMemberId().equals(memberId)) {
			throw new IllegalArgumentException("저장된 필터를 찾을 수 없어요.");
		}
		String name = request.getName() != null ? request.getName().trim() : "";
		if (name.isEmpty()) {
			throw new IllegalArgumentException("필터 이름을 입력해주세요.");
		}
		if (name.length() > MAX_NAME_LENGTH) {
			name = name.substring(0, MAX_NAME_LENGTH);
		}
		entity.setName(name);
		entity.setDistance(request.getDistance() != null ? request.getDistance() : 300);
		entity.setOpenOnly(Boolean.TRUE.equals(request.getOpenOnly()));
		try {
			List<String> types = request.getFoodTypes() != null ? request.getFoodTypes() : Collections.emptyList();
			entity.setFoodTypesJson(objectMapper.writeValueAsString(types));
		} catch (Exception e) {
			throw new IllegalArgumentException("음식 종류 데이터 처리 실패", e);
		}
		SavedFilterEntity saved = savedFilterRepository.save(entity);
		return SavedFilterResponse.fromEntity(saved);
	}

	@Transactional
	public void delete(String token, Long id) {
		Long memberId = getMemberId(token);
		SavedFilterEntity entity = savedFilterRepository.findById(id).orElse(null);
		if (entity != null && entity.getMemberId().equals(memberId)) {
			savedFilterRepository.delete(entity);
		}
	}
}
