package org.grr.bridgy.common.config

/**
 * 무료 사용자 제한 상수.
 * 향후 구독제 도입 시 프리미엄 사용자는 이 제한을 해제/완화합니다.
 */
object FreeTierLimits {

    // ─── 반려동물 ───
    const val MAX_PETS_PER_USER = 3

    // ─── 갤러리 ───
    const val MAX_PHOTOS_PER_PET = 20

    // ─── 댓글 ───
    const val MAX_COMMENTS_PER_USER_PER_DAY = 30
    const val MAX_COMMENT_LENGTH = 300

    // ─── 피드 / 검색 ───
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 20
    const val MAX_SEARCH_RESULTS = 20

    // ─── 데코레이션 ───
    const val MAX_DECORATIONS_PER_PET = 2  // 테두리 1개 + 뱃지 1개
}
