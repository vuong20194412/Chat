package vuong20194412.chat.authentication_api_gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthenticationApiGatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthenticationApiGatewayServiceApplication.class, args);
	}

}
