package tv.racespot.discordlivebot;

import tv.racespot.discordlivebot.service.BotService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RaceSpotLiveBotApplication {

	@Autowired
	private BotService botService;

	public static void main(String[] args) {
		SpringApplication.run(RaceSpotLiveBotApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			botService.startBot();
		};
	}
}
