package org.grr.bridgy.common.enums

import org.springframework.data.jpa.repository.JpaRepository

interface EnumValueRepository : JpaRepository<EnumValue, Long> {

    fun findByEnumType(enumType: String): List<EnumValue>

    fun findByEnumTypeAndActive(enumType: String, active: Boolean): List<EnumValue>

    fun findByEnumTypeAndEnumKey(enumType: String, enumKey: String): EnumValue?

    fun existsByEnumTypeAndEnumKey(enumType: String, enumKey: String): Boolean
}
