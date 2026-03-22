package org.grr.bridgy.domain.customer.repository

import org.grr.bridgy.domain.customer.entity.Customer
import org.springframework.data.jpa.repository.JpaRepository

interface CustomerRepository : JpaRepository<Customer, Long> {
    fun findByKakaoUserId(kakaoUserId: String): Customer?
}
