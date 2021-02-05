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
import tv.racespot.racespotlivebot.service.executor.ScheduleExecutor;
import tv.racespot.racespotlivebot.service.executor.SeriesLogoExecutor;
import tv.racespot.racespotlivebot.service.executor.ServerExecutor;
import tv.racespot.racespotlivebot.service.executor.UserMappingExecutor;
import tv.racespot.racespotlivebot.service.rest.SheetsManager;

import org.h2.tools.Server;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@Configuration
@Import({ExecutorConfig.class})
public class AppConfig {

    @Value("${discord.api.token}")
    private String apiToken;

    @Value("${discord.sheets.id}")
    private String sheetId;

    @Value("${discord.sheets.gid}")
    private int gid;

    @Value("${discord.sheets.range}")
    private String sheetRange;

    @Bean
    public DiscordApi api(){

        return new DiscordApiBuilder()
            .setToken(apiToken)
            .setAllIntents()
            .login().join();
    }

    @Bean
    public BotService botService(
        DiscordApi api,
        EventExecutor eventExecutor,
        ServerExecutor serverExecutor,
        ScheduleExecutor scheduleExecutor,
        UserMappingExecutor userMappingExecutor,
        SeriesLogoExecutor seriesLogoExecutor) {

        return new BotService(
            api,
            eventExecutor,
            serverExecutor,
            scheduleExecutor,
            userMappingExecutor,
            seriesLogoExecutor);
    }

    @Bean
    @Scope("singleton")
    public SheetsManager sheetsManager() {
        return new SheetsManager(
            sheetId,
            gid,
            sheetRange);
    }

    /* used for local dev
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server inMemoryH2DatabaseaServer() throws SQLException {
        return Server.createTcpServer(
            "-tcp", "-tcpAllowOthers", "-tcpPort", "9090");
    }*/

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}