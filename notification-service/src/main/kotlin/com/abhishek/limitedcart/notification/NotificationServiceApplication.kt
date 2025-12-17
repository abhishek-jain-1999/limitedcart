package com.abhishek.limitedcart.notification

import com.abhishek.limitedcart.common.config.JpaConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@SpringBootApplication(
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        JpaRepositoriesAutoConfiguration::class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration::class
    ]
)
@ComponentScan(
    basePackages = [
        "com.abhishek.limitedcart.notification",
        "com.abhishek.limitedcart.common",
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [JpaConfig::class]
        )
    ]
)
class NotificationServiceApplication {
    companion object {
        
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<NotificationServiceApplication>(*args)
        }
    }
}
