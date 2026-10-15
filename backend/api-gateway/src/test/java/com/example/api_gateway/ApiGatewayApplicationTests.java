package com.example.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"google.oauth.client-id=test.apps.googleusercontent.com", "eureka.client.enabled=false"})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
