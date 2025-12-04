package com.abhishek.limitedcart.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Base auditable entity shared by modules.
 * Fields are nullable so JPA can populate them after persistence.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,

    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime? = null
)

