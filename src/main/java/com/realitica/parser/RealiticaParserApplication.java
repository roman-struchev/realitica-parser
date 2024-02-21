package com.realitica.parser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class RealiticaParserApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealiticaParserApplication.class, args);
	}

}
