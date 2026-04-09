package org.grr.bridgy.domain.decoration.repository

import org.grr.bridgy.domain.decoration.entity.Decoration
import org.grr.bridgy.domain.decoration.entity.DecorationTier
import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.springframework.data.jpa.repository.JpaRepository

interface DecorationRepository : JpaRepository<Decoration, Long> {
    fun findByType(type: DecorationType): List<Decoration>
    fun findByTier(tier: DecorationTier): List<Decoration>
    fun findByTypeAndTier(type: DecorationType, tier: DecorationTier): List<Decoration>
    fun findByIsDefaultTrue(): List<Decoration>
}
