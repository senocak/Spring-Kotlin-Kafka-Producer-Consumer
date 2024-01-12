package com.github.senocak.config

import com.github.senocak.security.AuthorizationInterceptor
import com.github.senocak.util.AppConstants
import com.github.senocak.util.AppConstants.corePoolSize
import com.github.senocak.util.logger
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityScheme
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@EnableAsync
@Configuration
class AppConfig(
    private val authorizationInterceptor: AuthorizationInterceptor
): WebMvcConfigurer {
    private val log: Logger by logger()

    override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = corePoolSize
        executor.maxPoolSize = corePoolSize * 1_000
        executor.setThreadFactory(ThreadFactoryBuilder().setNameFormat("async-thread-%d").build())
        executor.queueCapacity = -1
        executor.initialize()
        configurer
            .setDefaultTimeout(120_000)
            .setTaskExecutor(executor)
        log.info("Core pool size: ${executor.corePoolSize}, max pool size: ${executor.maxPoolSize}," +
                "keepAliveSeconds: ${executor.keepAliveSeconds}, queueCapacity: ${executor.queueCapacity}," +
                "queueSize: ${executor.queueSize}")
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry
            .addRedirectViewController("/", "/index.html")
        registry
            .addRedirectViewController("/swagger", "/swagger-ui/index.html")
    }

    /**
     * Marking the files as resource
     * @param registry -- Stores registrations of resource handlers for serving static resources
     */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("//**")
            .addResourceLocations("classpath:/static/")
    }

    /**
     * Add Spring MVC lifecycle interceptors for pre- and post-processing of controller method invocations
     * and resource handler requests.
     * @param registry -- List of mapped interceptors.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(authorizationInterceptor)
            .addPathPatterns("/api/v1/**")
    }

    /**
     * Configure "global" cross origin request processing.
     * @param registry -- CorsRegistry assists with the registration of CorsConfiguration mapped to a path pattern.
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("HEAD", "OPTIONS", "GET", "POST", "PUT", "PATCH", "DELETE")
            .maxAge(3600)
    }

    @Bean
    fun customOpenAPI(@Value("\${springdoc.version}") appVersion: String?): OpenAPI =
            OpenAPI().components(
                    Components().addSecuritySchemes(AppConstants.securitySchemeName,
                            SecurityScheme()
                                    .name(AppConstants.securitySchemeName)
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                    )
            ).info(Info()
                    .title("Chat Rest Api - Kotlin")
                    .version(appVersion)
                    .description("Fully completed chat project written with Spring Boot")
                    .termsOfService("https://github.com/senocak")
                    .license(License().name("Apache 2.0").url("https://springdoc.org"))
            )

    /**
     * Returns a new instance of the RestTemplate class.
     * The RestTemplate class is a convenient class for making RESTful web service calls.
     * This method returns a new instance of the RestTemplate class, allowing clients to make multiple independent
     * web service calls using different RestTemplate instances.
     * @return a new instance of the RestTemplate class.
     */
    @Bean
    @Primary
    fun restTemplate(): RestTemplate = RestTemplate()

    /**
     * Returns a new instance of the RestTemplate class by passing SSL
     * The RestTemplate class is a convenient class for making RESTful web service calls.
     * This method returns a new instance of the RestTemplate class, allowing clients to make multiple independent
     * web service calls using different RestTemplate instances.
     * @return a new instance of the RestTemplate class.

    @Bean(name = ["restTemplateByPassSSL"])
    fun restTemplateByPassSSL(): RestTemplate {
        val acceptingTrustStrategy = TrustStrategy { chain: Array<X509Certificate>, authType: String -> true }
        val sslContext: SSLContext = SSLContexts.custom()
            .loadTrustMaterial(null, acceptingTrustStrategy)
            .build()
        val csf = SSLConnectionSocketFactory(sslContext)
        val httpClient: CloseableHttpClient = HttpClients.custom()
            .setSSLSocketFactory(csf)
            .build()
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        requestFactory.httpClient = httpClient
        return RestTemplate(requestFactory)
    }
     */


    /**
     * We use the PasswordEncoder that is defined in the Spring Security configuration to encode the password.
     * @return -- singleton instance of PasswordEncoder
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
