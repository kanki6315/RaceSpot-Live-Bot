/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.config;

import java.sql.SQLException;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import tv.racespot.racespotlivebot.service.BotService;
import tv.racespot.racespotlivebot.service.executor.EventExecutor;

import org.h2.tools.Server;
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

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server inMemoryH2DatabaseaServer() throws SQLException {
        return Server.createTcpServer(
            "-tcp", "-tcpAllowOthers", "-tcpPort", "9090");
    }

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}