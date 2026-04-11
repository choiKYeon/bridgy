package org.grr.bridgy.common.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.net.InetAddress

@Component
class StartupLogger(
    private val applicationContext: ApplicationContext,
    private val environment: Environment
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val port = environment.getProperty("server.port", "8080")
        val ip = InetAddress.getLocalHost().hostAddress
        val profile = environment.activeProfiles.joinToString().ifBlank { "default" }

        val handlerMapping = applicationContext.getBean(RequestMappingHandlerMapping::class.java)
        val endpoints = handlerMapping.handlerMethods.entries
            .flatMap { (info, _) ->
                val methods = info.methodsCondition.methods.map { it.name }.ifEmpty { listOf("GET") }
                val patterns = info.patternValues
                methods.flatMap { method -> patterns.map { pattern -> method to pattern } }
            }
            .filter { (_, path) -> path.startsWith("/api/") }
            .sortedWith(compareBy({ it.second }, { it.first }))

        val v0 = endpoints.filter { (_, p) -> p.contains("/v0/") }
        val v1 = endpoints.filter { (_, p) -> p.contains("/v1/") }

        val separator = "═".repeat(60)

        log.info("\n\n" +
            "  ╔$separator╗\n" +
            "  ║              🐾  BRIDGY SERVER STARTED  🐾              ║\n" +
            "  ╠$separator╣\n" +
            "  ║  Profile   : ${"%-46s".format(profile)}║\n" +
            "  ║  Local     : ${"%-46s".format("http://localhost:$port")}║\n" +
            "  ║  Network   : ${"%-46s".format("http://$ip:$port")}║\n" +
            "  ║  Swagger   : ${"%-46s".format("http://localhost:$port/swagger-ui/index.html")}║\n" +
            "  ╠$separator╣\n" +
            "  ║  📂 PUBLIC API (v0) — 인증 불필요                        ║\n" +
            v0.joinToString("") { (method, path) ->
                "  ║    %-8s %s\n".format(method, path).let { line ->
                    if (line.length > 63) "  ║    %-8s %s\n".format(method, path.take(47)) else line
                } .let { "  ║    %-8s %-47s║\n".format(method, path) }
            } +
            "  ╠$separator╣\n" +
            "  ║  🔐 AUTH API (v1) — JWT 필요                             ║\n" +
            v1.joinToString("") { (method, path) ->
                "  ║    %-8s %-47s║\n".format(method, path)
            } +
            "  ╚$separator╝\n"
        )
    }
}