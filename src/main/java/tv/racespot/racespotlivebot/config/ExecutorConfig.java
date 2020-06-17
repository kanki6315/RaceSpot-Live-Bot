/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.config;

import tv.racespot.racespotlivebot.data.EventRepository;
import tv.racespot.racespotlivebot.service.executor.EventExecutor;

import org.javacord.api.DiscordApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class ExecutorConfig {

    @Value("${google.api_key}")
    private String googleApiKey;

    @Value("${discord.notification.announcement_channel_id}")
    private String announcementChannelId;

    @Value("${discord.notification.admin_channel_id}")
    private String adminChannelId;

    @Autowired
    private EventRepository eventRepository;

    @Bean EventExecutor eventExecutor(
        final DiscordApi api) {
        return new EventExecutor(
            api,
            googleApiKey,
            eventRepository,
            announcementChannelId,
            adminChannelId);
    }
}