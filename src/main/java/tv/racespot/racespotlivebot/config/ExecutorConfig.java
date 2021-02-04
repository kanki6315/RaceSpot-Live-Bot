/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.config;

import tv.racespot.racespotlivebot.data.DServerRepository;
import tv.racespot.racespotlivebot.data.EventRepository;
import tv.racespot.racespotlivebot.data.ScheduledEventRepository;
import tv.racespot.racespotlivebot.data.SeriesLogoRepository;
import tv.racespot.racespotlivebot.data.UserMappingRepository;
import tv.racespot.racespotlivebot.service.executor.EventExecutor;
import tv.racespot.racespotlivebot.service.executor.ScheduleExecutor;
import tv.racespot.racespotlivebot.service.executor.ServerExecutor;
import tv.racespot.racespotlivebot.service.executor.UserMappingExecutor;
import tv.racespot.racespotlivebot.service.rest.SheetsManager;

import org.javacord.api.DiscordApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class ExecutorConfig {

    @Value("${google.api_key}")
    private String googleApiKey;

    @Value("${discord.notification.admin_channel_id}")
    private String adminChannelId;

    @Value("${discord.notification.schedule_channel_id}")
    private String scheduleChannelId;

    @Value("${discord.notification.error_channel_id}")
    private String errorChannelId;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DServerRepository serverRepository;


    @Bean
    public EventExecutor eventExecutor(
        final DiscordApi api) {
        return new EventExecutor(
            api,
            googleApiKey,
            eventRepository,
            serverRepository,
            adminChannelId);
    }


    @Bean
    public ServerExecutor serverExecutor(
        final DiscordApi api) {
        return new ServerExecutor(
            adminChannelId,
            serverRepository,
            api);
    }

    @Bean
    public ScheduleExecutor scheduleExecutor(
        final DiscordApi api,
        SheetsManager sheetsManager,
        ScheduledEventRepository scheduledEventRepository,
        UserMappingRepository userMappingRepository,
        SeriesLogoRepository seriesLogoRepository) {
        return new ScheduleExecutor(
            api,
            sheetsManager,
            scheduledEventRepository,
            userMappingRepository,
            seriesLogoRepository,
            scheduleChannelId,
            adminChannelId,
            errorChannelId);
    }

    @Bean
    public UserMappingExecutor userMappingExecutor(
        UserMappingRepository userMappingRepository) {
        return new UserMappingExecutor(
            userMappingRepository,
            adminChannelId);
    }
}