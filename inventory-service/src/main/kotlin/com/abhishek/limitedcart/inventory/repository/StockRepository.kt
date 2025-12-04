package com.abhishek.limitedcart.inventory.repository

import com.abhishek.limitedcart.inventory.entity.Stock
import org.springframework.data.jpa.repository.JpaRepository

interface StockRepository : JpaRepository<Stock, String>

