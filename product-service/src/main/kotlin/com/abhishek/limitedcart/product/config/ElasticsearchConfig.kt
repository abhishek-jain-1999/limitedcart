package com.abhishek.limitedcart.product.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.client.ClientConfiguration

@Configuration
@EnableElasticsearchRepositories(basePackages = ["com.abhishek.limitedcart.product.search"])
class ElasticsearchConfig : ElasticsearchConfiguration() {
    override fun clientConfiguration(): ClientConfiguration {
        val elasticsearchUrl = System.getenv("ELASTICSEARCH_URL") ?: "http://localhost:9200"
        val host = elasticsearchUrl.replace("http://", "").replace("https://", "")
        return ClientConfiguration.builder()
            .connectedTo(host)
            .build()
    }
}
