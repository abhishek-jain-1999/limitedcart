package com.abhishek.limitedcart.product.search

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

import org.springframework.data.elasticsearch.annotations.Query

interface ProductSearchRepository : ElasticsearchRepository<ProductDocument, String> {
    /**
     * Performs a robust search using a Boolean query with "should" clauses (OR logic):
     * 1. multi_match: Searches 'name' and 'description' for the full token.
     *    - fuzziness="AUTO": Handles typos (e.g., "aple" -> "apple").
     * 2. wildcard: Searches 'name' for the substring *query*.
     *    - Handles partial matches (e.g., "ulta" -> "ultrabook").
     * 3. wildcard: Searches 'description' for the substring *query*.
     */
    @Query("{\"bool\": {\"should\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name\", \"description\"], \"fuzziness\": \"AUTO\"}}, {\"wildcard\": {\"name\": {\"value\": \"*?0*\"}}}, {\"wildcard\": {\"description\": {\"value\": \"*?0*\"}}}]}}")
    fun searchByNameOrDescription(query: String): List<ProductDocument>
}
