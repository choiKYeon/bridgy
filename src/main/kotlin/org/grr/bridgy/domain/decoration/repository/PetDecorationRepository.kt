package org.grr.bridgy.domain.decoration.repository

import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.grr.bridgy.domain.decoration.entity.PetDecoration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface PetDecorationRepository : JpaRepository<PetDecoration, Long> {
    fun findByPetId(petId: Long): List<PetDecoration>
    fun findByPetIdAndDecorationType(petId: Long, decorationType: DecorationType): Optional<PetDecoration>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PetDecoration pd WHERE pd.pet.id = :petId AND pd.decorationType = :decorationType")
    fun deleteByPetIdAndDecorationType(petId: Long, decorationType: DecorationType)
}
