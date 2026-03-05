<script setup>
const props = defineProps({
  winnerOption: {
    type: Object,
    required: true
  },
  percentage: {
    type: Number,
    required: true
  },
  totalVoters: {
    type: Number,
    required: true
  },
  getPhotoUrl: {
    type: Function,
    required: true
  },
  handleImageError: {
    type: Function,
    required: true
  },
  getBusinessStatus: {
    type: Function,
    required: true
  }
})
</script>

<template>
  <div class="result-card">
    <div class="result-card-header">
      <span class="result-badge">TOP PICK</span>
      <div class="result-card-title">{{ percentage }}%의 선택, 오늘의 점심</div>
      <div class="result-card-subtitle">{{ totalVoters }}명이 투표에 참여했어요</div>
    </div>
    <div class="result-card-content">
      <div class="result-card-img">
        <img
          :src="getPhotoUrl(winnerOption)"
          :alt="winnerOption.name"
          class="result-card-photo"
          @error="handleImageError"
        />
      </div>
      <div class="result-card-info">
        <div class="result-store-header">
          <span class="result-store-name">{{ winnerOption.name }}</span>
          <span class="result-store-category">{{ (winnerOption.categories || [])[0] || '식당' }}</span>
        </div>
        <div class="result-store-details">
          <div class="result-detail-row">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <mask
                id="loc_mask"
                style="mask-type:alpha"
                maskUnits="userSpaceOnUse"
                x="0"
                y="0"
                width="20"
                height="20"
              >
                <rect width="20" height="20" fill="#D9D9D9" />
              </mask>
              <g mask="url(#loc_mask)">
                <path
                  d="M10 10C10.4583 10 10.8507 9.83681 11.1771 9.51042C11.5035 9.18403 11.6667 8.79167 11.6667 8.33333C11.6667 7.875 11.5035 7.48264 11.1771 7.15625C10.8507 6.82986 10.4583 6.66667 10 6.66667C9.54167 6.66667 9.14931 6.82986 8.82292 7.15625C8.49653 7.48264 8.33333 7.875 8.33333 8.33333C8.33333 8.79167 8.49653 9.18403 8.82292 9.51042C9.14931 9.83681 9.54167 10 10 10ZM10 18.3333C7.76389 16.4306 6.09375 14.6632 4.98958 13.0312C3.88542 11.3993 3.33333 9.88889 3.33333 8.5C3.33333 6.41667 4.00347 4.75694 5.34375 3.52083C6.68403 2.28472 8.23611 1.66667 10 1.66667C11.7639 1.66667 13.316 2.28472 14.6563 3.52083C15.9965 4.75694 16.6667 6.41667 16.6667 8.5C16.6667 9.88889 16.1146 11.3993 15.0104 13.0312C13.9062 14.6632 12.2361 16.4306 10 18.3333Z"
                  fill="#5F6368"
                />
              </g>
            </svg>
            <span>거리: {{ winnerOption.distance || '정보 없음' }}</span>
          </div>
          <div class="result-detail-row">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <mask
                id="star_mask"
                style="mask-type:alpha"
                maskUnits="userSpaceOnUse"
                x="0"
                y="0"
                width="20"
                height="20"
              >
                <rect width="20" height="20" fill="#D9D9D9" />
              </mask>
              <g mask="url(#star_mask)">
                <path
                  d="M4.85417 17.5L6.20833 11.6458L1.66667 7.70833L7.66667 7.1875L10 1.66667L12.3333 7.1875L18.3333 7.70833L13.7917 11.6458L15.1458 17.5L10 14.3958L4.85417 17.5Z"
                  fill="#5F6368"
                />
              </g>
            </svg>
            <span>평점: {{ winnerOption.rating || '정보 없음' }}</span>
          </div>
          <div class="result-detail-row">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <mask
                id="schedule_mask"
                style="mask-type:alpha"
                maskUnits="userSpaceOnUse"
                x="0"
                y="0"
                width="20"
                height="20"
              >
                <rect width="20" height="20" fill="#D9D9D9" />
              </mask>
              <g mask="url(#schedule_mask)">
                <path
                  d="M12.75 13.9167L13.9167 12.75L10.8333 9.66667V5.83333H9.16667V10.3333L12.75 13.9167ZM10 18.3333C8.84722 18.3333 7.76389 18.1146 6.75 17.6771C5.73611 17.2396 4.85417 16.6458 4.10417 15.8958C3.35417 15.1458 2.76042 14.2639 2.32292 13.25C1.88542 12.2361 1.66667 11.1528 1.66667 10C1.66667 8.84722 1.88542 7.76389 2.32292 6.75C2.76042 5.73611 3.35417 4.85417 4.10417 4.10417C4.85417 3.35417 5.73611 2.76042 6.75 2.32292C7.76389 1.88542 8.84722 1.66667 10 1.66667C11.1528 1.66667 12.2361 1.88542 13.25 2.32292C14.2639 2.76042 15.1458 3.35417 15.8958 4.10417C16.6458 4.85417 17.2396 5.73611 17.6771 6.75C18.1146 7.76389 18.3333 8.84722 18.3333 10C18.3333 11.1528 18.1146 12.2361 17.6771 13.25C17.2396 14.2639 16.6458 15.1458 15.8958 15.8958C15.1458 16.6458 14.2639 17.2396 13.25 17.6771C12.2361 18.1146 11.1528 18.3333 10 18.3333Z"
                  fill="#5F6368"
                />
              </g>
            </svg>
            <span>
              영업여부:
              <span
                :class="{
                  'status-open': winnerOption.openNow === true,
                  'status-closed': winnerOption.openNow === false
                }"
              >
                {{ getBusinessStatus(winnerOption) }}
              </span>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
