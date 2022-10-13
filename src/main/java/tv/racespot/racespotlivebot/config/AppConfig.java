/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.config;

import java.util.TimeZone;

import javax.annotation.PostConstruct;

import tv.racespot.racespotlivebot.service.BotService;
import tv.racespot.racespotlivebot.service.commands.*;
import tv.racespot.racespotlivebot.service.rest.SheetsManager;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@Configuration
@Import({CommandConfig.class})
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
            EventCommands eventCommands,
            ScheduleCommands scheduleCommands,
            SeriesLogoCommands seriesLogoCommands,
            ServerCommands serverCommands,
            UserMappingCommands userMappingCommands) {

        return new BotService(
                api,
                eventCommands,
                scheduleCommands,
                seriesLogoCommands,
                serverCommands,
                userMappingCommands);
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