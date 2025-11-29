package com.quantlabs.QuantTester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableAutoConfiguration
@EntityScan(basePackages = "com.quantlabs.QuantTester.model")
@EnableJpaRepositories(basePackages = "com.quantlabs.QuantTester.repository")
public class QuantLabsApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuantLabsApplication.class, args);
	}

}
