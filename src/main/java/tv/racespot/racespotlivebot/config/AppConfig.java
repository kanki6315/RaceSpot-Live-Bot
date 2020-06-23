/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.config;

import java.util.TimeZone;

import javax.annotation.PostConstruct;

import tv.racespot.racespotlivebot.service.BotService;
import tv.racespot.racespotlivebot.service.executor.EventExecutor;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ExecutorConfig.class})
public class AppConfig {

    @Value("${discord.api.token}")
    private String apiToken;

    @Bean
    public DiscordApi api(){

        return new DiscordApiBuilder().setToken(apiToken).login().join();
    }

    @Bean
    public BotService botService(
        DiscordApi api,
        EventExecutor eventExecutor) {

        return new BotService(
            api,
            eventExecutor);
    }

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}