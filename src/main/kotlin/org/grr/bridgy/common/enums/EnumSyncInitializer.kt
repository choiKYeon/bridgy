package org.grr.bridgy.common.enums

import org.grr.bridgy.domain.decoration.entity.DecorationTier
import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.grr.bridgy.domain.user.entity.UserRole
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 앱 실행 시 SyncableEnum을 구현한 모든 enum을 enum_values 테이블에 자동 동기화합니다.
 *
 * - 새 enum 값 → INSERT
 * - 기존 값의 description/ordinal 변경 → UPDATE
 * - 코드에서 삭제된 enum 값 → active = false (데이터 보존)
 *
 * 새로운 enum을 추가하려면 SYNC_TARGETS 리스트에 등록하세요.
 */
@Component
@Profile("!test")
@Order(1) // 다른 initializer보다 먼저 실행
class EnumSyncInitializer(
    private val enumValueRepository: EnumValueRepository
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(EnumSyncInitializer::class.java)

    /**
     * 동기화 대상 enum 목록.
     * 새 enum 타입을 만들면 여기에 추가하세요.
     */
    private val syncTargets: List<Pair<String, Array<out SyncableEnum>>> = listOf(
        "USER_ROLE" to UserRole.values(),
        "DECORATION_TYPE" to DecorationType.values(),
        "DECORATION_TIER" to DecorationTier.values()
    )

    @Transactional
    override fun run(vararg args: String?) {
        var totalInserted = 0
        var totalUpdated = 0
        var totalDeactivated = 0

        for ((enumType, values) in syncTargets) {
            val result = syncEnum(enumType, values)
            totalInserted += result.inserted
            totalUpdated += result.updated
            totalDeactivated += result.deactivated
        }

        log.info("============================================")
        log.info("  Enum 동기화 완료")
        log.info("  대상: ${syncTargets.size}개 enum 타입")
        log.info("  신규: ${totalInserted}개 | 수정: ${totalUpdated}개 | 비활성화: ${totalDeactivated}개")
        log.info("============================================")
    }

    private fun syncEnum(enumType: String, values: Array<out SyncableEnum>): SyncResult {
        var inserted = 0
        var updated = 0
        var deactivated = 0

        val currentKeys = values.map { it.name }.toSet()

        // 1) 새 값 추가 또는 기존 값 업데이트
        for (value in values) {
            val existing = enumValueRepository.findByEnumTypeAndEnumKey(enumType, value.name)

            if (existing == null) {
                enumValueRepository.save(
                    EnumValue(
                        enumType = enumType,
                        enumKey = value.name,
                        description = value.description,
                        ordinal = value.ordinal,
                        active = true
                    )
                )
                log.info("  [+] $enumType.${value.name} 추가됨")
                inserted++
            } else {
                var changed = false
                if (existing.description != value.description) {
                    existing.description = value.description
                    changed = true
                }
                if (existing.ordinal != value.ordinal) {
                    existing.ordinal = value.ordinal
                    changed = true
                }
                if (!existing.active) {
                    existing.active = true
                    changed = true
                }
                if (changed) {
                    enumValueRepository.save(existing)
                    log.info("  [~] $enumType.${value.name} 업데이트됨")
                    updated++
                }
            }
        }

        // 2) 코드에서 삭제된 enum 값은 비활성화
        val dbValues = enumValueRepository.findByEnumTypeAndActive(enumType, true)
        for (dbValue in dbValues) {
            if (dbValue.enumKey !in currentKeys) {
                dbValue.active = false
                enumValueRepository.save(dbValue)
                log.info("  [-] $enumType.${dbValue.enumKey} 비활성화됨 (코드에서 제거됨)")
                deactivated++
            }
        }

        return SyncResult(inserted, updated, deactivated)
    }

    private data class SyncResult(val inserted: Int, val updated: Int, val deactivated: Int)
}
