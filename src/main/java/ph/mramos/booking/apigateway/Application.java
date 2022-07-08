package ph.mramos.booking.apigateway;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.ServerWebExchange;

import com.google.common.hash.Hashing;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class Application {

	@Value("${app.github.secret-key}")
	private String githubSecretKey;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public RouteLocator routLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("booking-app-route",
						r -> r.path("/booking/**")
							.and().method("GET", "POST", "PUT", "DELETE")
						.filters(
							f -> f.addRequestHeader("X-Request-Color", "red")
							.addResponseHeader("X-Response-Color", "blue"))
						.uri("http://localhost:9080/"))
				.route("book-app-actuator-refresh-route",
						r -> r.path("/actuator/refresh") // Don't forget to run ngrok.exe first otherwise GitHub web hook will not be able to reach the /actuator/refresh endpoint.
							.and().header("X-Hub-Signature-256")
							.and().predicate(this::isValidGithubWebhookRequest)
						.filters(
							f -> f.modifyRequestBody(String.class, String.class, (exchange, s) -> Mono.empty()))
						.uri("http://localhost:9080"))
				.build();
	}

	private boolean isValidGithubWebhookRequest(ServerWebExchange exchange) {
		String githubSignedBody = exchange.getRequest().getHeaders().getFirst("X-Hub-Signature-256");
		githubSignedBody = githubSignedBody.substring(7); // Drop the 'sha256=' prefix.

		String rawBody = exchange.getAttribute("cachedRequestBodyObject");
		String hashedBody = Hashing.hmacSha256(githubSecretKey.getBytes(StandardCharsets.UTF_8))
				.hashString(rawBody, StandardCharsets.UTF_8)
				.toString();

		return githubSignedBody.equals(hashedBody);
	}

}
