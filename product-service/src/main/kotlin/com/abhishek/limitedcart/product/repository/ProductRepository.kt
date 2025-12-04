package com.abhishek.limitedcart.product.repository

import com.abhishek.limitedcart.product.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductRepository : JpaRepository<ProductEntity, UUID>

