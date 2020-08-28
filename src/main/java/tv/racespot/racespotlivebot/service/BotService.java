/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.service;

import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import tv.racespot.racespotlivebot.service.executor.EventExecutor;
import tv.racespot.racespotlivebot.service.executor.ServerExecutor;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.activity.ActivityType;

public class BotService {

    private final DiscordApi api;
    private CommandHandler handler;

    private final EventExecutor eventExecutor;
    private final ServerExecutor serverExecutor;

    public BotService(
        final DiscordApi api,
        final EventExecutor eventExecutor,
        final ServerExecutor serverExecutor) {
        this.api = api;
        this.eventExecutor = eventExecutor;
        this.serverExecutor = serverExecutor;
    }

    public Boolean startBot() {

        api.updateActivity(ActivityType.WATCHING, "Sim Racing Action!");

        handler = new JavacordHandler(api);
        handler.registerCommand(eventExecutor);
        handler.registerCommand(serverExecutor);

        api.addReconnectListener(event -> event.getApi().updateActivity(ActivityType.WATCHING, "Getting ready for live coverage!"));

        return true;
    }
}