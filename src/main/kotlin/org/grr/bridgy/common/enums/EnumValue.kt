package org.grr.bridgy.common.enums

import jakarta.persistence.*

@Entity
@Table(
    name = "enum_values",
    uniqueConstraints = [UniqueConstraint(columnNames = ["enum_type", "enum_key"])]
)
class EnumValue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "enum_type", nullable = false)
    val enumType: String,

    @Column(name = "enum_key", nullable = false)
    val enumKey: String,

    @Column(nullable = false)
    var description: String = "",

    @Column(nullable = false)
    var ordinal: Int = 0,

    @Column(nullable = false)
    var active: Boolean = true
)
