package org.grr.bridgy.common.enums

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Enum V0", description = "Enum 값 조회 공개 API (인증 불필요)")
@RestController
@RequestMapping("/api/v0/enums")
class EnumControllerV0(
    private val enumValueRepository: EnumValueRepository
) {

    @Operation(
        summary = "전체 enum 타입 목록 조회",
        description = "시스템에 등록된 모든 enum 타입 목록을 조회합니다.",
        tags = ["Enum"]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공")
        ]
    )
    @GetMapping
    fun getEnumTypes(): ResponseEntity<List<String>> {
        val types = enumValueRepository.findAll()
            .map { it.enumType }
            .distinct()
            .sorted()
        return ResponseEntity.ok(types)
    }

    @Operation(
        summary = "특정 enum 타입의 값 목록 조회",
        description = "지정한 enum 타입의 활성화된 값 목록을 조회합니다. (예: USER_ROLE, DECORATION_TYPE, DECORATION_TIER)",
        tags = ["Enum"]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공")
        ]
    )
    @GetMapping("/{enumType}")
    fun getEnumValues(
        @Parameter(
            description = "조회할 enum 타입 (예: USER_ROLE, DECORATION_TYPE, DECORATION_TIER)",
            example = "USER_ROLE",
            required = true
        )
        @PathVariable enumType: String
    ): ResponseEntity<List<EnumValueResponse>> {
        val values = enumValueRepository.findByEnumTypeAndActive(enumType.uppercase(), true)
            .sortedBy { it.ordinal }
            .map { EnumValueResponse.from(it) }
        return ResponseEntity.ok(values)
    }

    @Operation(
        summary = "전체 enum 값 조회 (타입별 그룹핑)",
        description = "시스템에 등록된 모든 활성 enum 값을 타입별로 그룹핑하여 조회합니다.",
        tags = ["Enum"]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공")
        ]
    )
    @GetMapping("/all")
    fun getAllEnumValues(): ResponseEntity<Map<String, List<EnumValueResponse>>> {
        val grouped = enumValueRepository.findAll()
            .filter { it.active }
            .groupBy { it.enumType }
            .mapValues { (_, values) ->
                values.sortedBy { it.ordinal }.map { EnumValueResponse.from(it) }
            }
        return ResponseEntity.ok(grouped)
    }
}

data class EnumValueResponse(
    val key: String,
    val description: String,
    val ordinal: Int
) {
    companion object {
        fun from(entity: EnumValue) = EnumValueResponse(
            key = entity.enumKey,
            description = entity.description,
            ordinal = entity.ordinal
        )
    }
}
