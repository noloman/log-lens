package me.manulorenzo.api.configuration

import me.manulorenzo.api.observability.CorrelationIdFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfig {
    @Bean
    fun correlationIdFilter(): FilterRegistrationBean<CorrelationIdFilter> {
        val registrationBean = FilterRegistrationBean<CorrelationIdFilter>()
        registrationBean.filter = CorrelationIdFilter()
        registrationBean.addUrlPatterns("/api/*")
        return registrationBean
    }
}
