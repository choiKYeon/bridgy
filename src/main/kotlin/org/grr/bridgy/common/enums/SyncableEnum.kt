package org.grr.bridgy.common.enums

/**
 * DB에 자동 동기화할 enum에 이 인터페이스를 구현하세요.
 * 새 enum 값을 추가하면 다음 앱 실행 시 자동으로 enum_values 테이블에 반영됩니다.
 *
 * 사용법:
 * enum class MyType(override val description: String) : SyncableEnum {
 *     SOME_VALUE("설명"),
 *     ANOTHER("다른 설명")
 * }
 */
interface SyncableEnum {
    val description: String
    val name: String        // Kotlin enum에 기본 제공
    val ordinal: Int        // Kotlin enum에 기본 제공
}
