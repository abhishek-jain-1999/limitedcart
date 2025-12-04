package com.abhishek.limitedcart.product.controller

import com.abhishek.limitedcart.product.search.ProductSearchService
import com.abhishek.limitedcart.product.service.ProductService
import com.abhishek.limitedcart.product.service.dto.CreateProductRequest
import com.abhishek.limitedcart.product.service.dto.ProductListResponse
import com.abhishek.limitedcart.product.service.dto.ProductResponse
import com.abhishek.limitedcart.product.service.dto.UpdateProductRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/products")
class ProductController(
    private val productService: ProductService,
    private val productSearchService: ProductSearchService
) {

    @PostMapping
    fun createProduct(
        @Valid @RequestBody request: CreateProductRequest,
        @RequestHeader(name = "X-Admin-Role", required = false) adminRole: String?
    ): ResponseEntity<ProductResponse> {
        if (adminRole != "ADMIN") {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required")
        }
        val result = productService.createProduct(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse(result))
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: UUID): ResponseEntity<ProductResponse> {
        val product = productService.getProduct(id)
        return ResponseEntity.ok(ProductResponse(product))
    }

    @GetMapping
    fun listProducts(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ProductListResponse> {
        val page = productService.listProducts(pageable)
        val response = ProductListResponse(
            items = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/search")
    fun searchProducts(@RequestParam("q") query: String): ResponseEntity<List<ProductResponse>> {
        val results = productSearchService.search(query)
            .map {
                ProductResponse(
                    product = com.abhishek.limitedcart.product.entity.ProductView(
                        id = UUID.fromString(it.id),
                        name = it.name,
                        description = it.description,
                        price = it.price,
                        maxQuantityPerSale = 0,
                        active = it.active,
                        inStock = it.inStock,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                )
            }
        return ResponseEntity.ok(results)
    }

    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProductRequest,
        @RequestHeader(name = "X-Admin-Role", required = false) adminRole: String?
    ): ResponseEntity<ProductResponse> {
        if (adminRole != "ADMIN") {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required")
        }
        val updated = productService.updateProduct(id, request)
        return ResponseEntity.ok(ProductResponse(updated))
    }
}
