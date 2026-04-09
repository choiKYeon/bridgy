package org.grr.bridgy.domain.decoration.repository

import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.grr.bridgy.domain.decoration.entity.PetDecoration
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PetDecorationRepository : JpaRepository<PetDecoration, Long> {
    fun findByPetId(petId: Long): List<PetDecoration>
    fun findByPetIdAndDecorationType(petId: Long, decorationType: DecorationType): Optional<PetDecoration>
    fun deleteByPetIdAndDecorationType(petId: Long, decorationType: DecorationType)
}
