package ph.mramos.booking.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class Application {

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
						r -> r.path("/actuator/refresh")
							.and().header("X-Hub-Signature-256")
							.and().predicate(exchange -> {
								String hashedBody = exchange.getRequest().getHeaders().getFirst("X-Hub-Signature-256");
								System.out.println("Hashed Body: " + hashedBody);
								return true;
							})
						.filters(
							f -> f.modifyRequestBody(String.class, String.class, (exchange, s) -> Mono.empty()))
						.uri("http://localhost:9080"))
				.build();
	}

}
