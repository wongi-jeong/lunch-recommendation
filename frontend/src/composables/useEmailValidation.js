import { computed } from 'vue'
import { DISPOSABLE_EMAIL_DOMAINS } from '@/data/disposableEmailDomains'

/**
 * 대기업 벤치마크 기반 이메일 유효성 검사
 *
 * 1. 구글/네이버/카카오: RFC 5322 문법 검증
 * 2. MX/DNS 검증: 도메인 존재 여부 (백엔드 API 호출 시)
 * 3. 일회용 이메일 차단: 10minutemail, guerrillamail 등
 * 4. 도메인 오타 제안: gmail.con → gmail.com
 */
const EMAIL_LOCAL_MAX = 64   // RFC 5321
const EMAIL_TOTAL_MAX = 254 // RFC 5321

// RFC 5322 실용적 문법: 로컬@도메인.TLD (대소문자, 숫자, ._-+ 허용, @ 단일)
const EMAIL_SYNTAX_REGEX = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\.[a-zA-Z]{2,}$/

// 한국/글로벌 대기업 도메인 오타 교정 맵 (자주 틀리는 패턴)
const DOMAIN_TYPO_MAP = {
  // Gmail
  'gmial.com': 'gmail.com',
  'gmai.com': 'gmail.com',
  'gmail.con': 'gmail.com',
  'gmail.co': 'gmail.com',
  'gmail.ocm': 'gmail.com',
  'gnail.com': 'gmail.com',
  'gmal.com': 'gmail.com',
  // 네이버
  'naver.con': 'naver.com',
  'naver.comm': 'naver.com',
  'naver.coom': 'naver.com',
  'nvaer.com': 'naver.com',
  // 다음
  'daum.ent': 'daum.net',
  'daum.nte': 'daum.net',
  'daum.com': 'daum.net',
  'daum.con': 'daum.net',
  // 한메일
  'hanmail.ent': 'hanmail.net',
  'hanmail.nte': 'hanmail.net',
  'hanmail.com': 'hanmail.net',
  'hanmail.con': 'hanmail.net',
  // 카카오
  'kakao.con': 'kakao.com',
  'kakao.co': 'kakao.com',
  'kaka.com': 'kakao.com',
  'kako.com': 'kakao.com',
  'cacao.com': 'kakao.com',
  // 네이트
  'nate.con': 'nate.com',
  'nate.co': 'nate.com',
  'nate.co.kr': 'nate.com',
  'nte.com': 'nate.com',
  // 야후
  'yahooo.com': 'yahoo.com',
  'yaho.com': 'yahoo.com',
  // 엠파스
  'empas.con': 'empas.com',
  'empas.co': 'empas.com',
  'empass.com': 'empas.com',
  // 파란
  'paran.con': 'paran.com',
  'paran.co': 'paran.com',
  'parann.com': 'paran.com',
  // 드림위즈
  'dreamwiz.con': 'dreamwiz.com',
  'dreamwiz.co': 'dreamwiz.com',
  'dreamwis.com': 'dreamwiz.com',
  // 한미르
  'hanmir.con': 'hanmir.com',
  'hanmir.co': 'hanmir.com',
  'hanmil.com': 'hanmir.com',
  // Outlook / Hotmail / 기타 글로벌
  'outlok.com': 'outlook.com',
  'outlook.con': 'outlook.com',
  'outlook.co': 'outlook.com',
  'hotmial.com': 'hotmail.com',
  'hotmail.con': 'hotmail.com',
  'icloud.con': 'icloud.com',
  'iclod.com': 'icloud.com',
}

/**
 * @param {string} email
 * @returns {{ valid: boolean, error: string|null, suggestion: string|null }}
 */
export function validateEmail(email) {
  const trimmed = (email || '').trim()
  if (!trimmed) {
    return { valid: false, error: '이메일을 입력해주세요', suggestion: null }
  }

  // 1. 길이 검증 (RFC 5321)
  const [localPart, domainPart] = trimmed.split('@')
  if (localPart?.length > EMAIL_LOCAL_MAX || trimmed.length > EMAIL_TOTAL_MAX) {
    return { valid: false, error: '이메일 주소가 너무 깁니다', suggestion: null }
  }

  // 2. 문법 검증 (RFC 5322 스타일)
  if (!EMAIL_SYNTAX_REGEX.test(trimmed)) {
    return { valid: false, error: '올바른 이메일 형식을 입력해주세요', suggestion: null }
  }

  // 3. 일회용/임시 이메일 차단 (대기업 표준)
  const domain = (domainPart || '').toLowerCase()
  if (DISPOSABLE_EMAIL_DOMAINS.has(domain)) {
    return {
      valid: false,
      error: '일회용 이메일은 사용할 수 없습니다',
      suggestion: '실제 사용 중인 이메일 주소를 입력해주세요'
    }
  }

  // 4. 도메인 오타 제안
  const suggestion = DOMAIN_TYPO_MAP[domain]
  if (suggestion) {
    const suggestedEmail = localPart + '@' + suggestion
    return {
      valid: false,
      error: `'${domain}' 대신 '${suggestion}'을 사용하시나요?`,
      suggestion: suggestedEmail
    }
  }

  return { valid: true, error: null, suggestion: null }
}

/**
 * Vue Composable: 이메일 유효성 검사
 * @param {import('vue').Ref<string>} emailRef - 이메일 입력 ref
 * @returns {{ isValid: import('vue').ComputedRef<boolean>, error: import('vue').ComputedRef<string|null>, suggestion: import('vue').ComputedRef<string|null>, validate: (email?: string) => { valid: boolean, error: string|null, suggestion: string|null } }}
 */
export function useEmailValidation(emailRef) {
  const result = computed(() => {
    const email = emailRef?.value ?? ''
    if (!email) return { valid: false, error: null, suggestion: null }
    return validateEmail(email)
  })

  const isValid = computed(() => result.value.valid)
  const error = computed(() => result.value.error)
  const suggestion = computed(() => result.value.suggestion)

  return {
    isValid,
    error,
    suggestion,
    validate: validateEmail
  }
}
