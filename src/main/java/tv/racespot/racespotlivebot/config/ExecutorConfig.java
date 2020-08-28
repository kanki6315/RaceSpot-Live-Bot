/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.config;

import tv.racespot.racespotlivebot.data.DServerRepository;
import tv.racespot.racespotlivebot.data.EventRepository;
import tv.racespot.racespotlivebot.service.executor.EventExecutor;
import tv.racespot.racespotlivebot.service.executor.ServerExecutor;

import org.javacord.api.DiscordApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class ExecutorConfig {

    @Value("${google.api_key}")
    private String googleApiKey;


    @Value("${discord.notification.admin_channel_id}")
    private String adminChannelId;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DServerRepository serverRepository;


    @Bean
    EventExecutor eventExecutor(
        final DiscordApi api) {
        return new EventExecutor(
            api,
            googleApiKey,
            eventRepository,
            serverRepository,
            adminChannelId);
    }


    @Bean ServerExecutor serverExecutor(
        final DiscordApi api) {
        return new ServerExecutor(
            adminChannelId,
            serverRepository,
            api);
    }
}