/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.discordlivebot.service;

import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import tv.racespot.discordlivebot.service.executor.EventExecutor;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.activity.ActivityType;

public class BotService {

    private final DiscordApi api;
    private CommandHandler handler;

    private final EventExecutor eventExecutor;

    public BotService(
        final DiscordApi api,
        final EventExecutor eventExecutor) {
        this.api = api;
        this.eventExecutor = eventExecutor;
    }

    public Boolean startBot() {

        api.updateActivity(ActivityType.WATCHING, "Sim Racing Action!");

        handler = new JavacordHandler(api);
        handler.registerCommand(eventExecutor);

        api.addReconnectListener(event -> event.getApi().updateActivity(ActivityType.WATCHING, "Getting ready for live coverage!"));

        return true;
    }
}