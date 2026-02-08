package org.cthub.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChallengeTeamHubBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChallengeTeamHubBackendApplication.class, args);
	}

}
