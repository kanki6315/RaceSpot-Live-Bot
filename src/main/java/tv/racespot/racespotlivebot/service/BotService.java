/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.service;

import me.s3ns3iw00.jcommands.CommandHandler;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.activity.ActivityType;
import tv.racespot.racespotlivebot.service.commands.*;

public class BotService {

    private final DiscordApi api;

    private final EventCommands eventCommands;
    private final ScheduleCommands scheduleCommands;
    private final SeriesLogoCommands seriesLogoCommands;
    private final ServerCommands serverCommands;
    private final UserMappingCommands userMappingCommands;

    public BotService(
            final DiscordApi api,
            final EventCommands eventCommands,
            final ScheduleCommands scheduleCommands,
            final SeriesLogoCommands seriesLogoCommands,
            final ServerCommands serverCommands,
            final UserMappingCommands userMappingCommands) {
        this.api = api;
        this.eventCommands = eventCommands;
        this.scheduleCommands = scheduleCommands;
        this.seriesLogoCommands = seriesLogoCommands;
        this.serverCommands = serverCommands;
        this.userMappingCommands = userMappingCommands;
    }

    public Boolean startBot() {

        api.updateActivity(ActivityType.WATCHING, "Sim Racing Action!");
        CommandHandler.setApi(api);
        CommandHandler.registerCommand(eventCommands.addYoutubeEvent());
        CommandHandler.registerCommand(eventCommands.clear());
        CommandHandler.registerCommand(scheduleCommands.postSchedule());
        CommandHandler.registerCommand(scheduleCommands.updateSchedule());
        CommandHandler.registerCommand(scheduleCommands.clearSchedule());
        CommandHandler.registerCommand(seriesLogoCommands.addSeries());
        CommandHandler.registerCommand(seriesLogoCommands.listSeries());
        CommandHandler.registerCommand(seriesLogoCommands.removeSeries());
        CommandHandler.registerCommand(serverCommands.addServer());
        CommandHandler.registerCommand(serverCommands.listServer());
        CommandHandler.registerCommand(userMappingCommands.addTalent());
        CommandHandler.registerCommand(userMappingCommands.listTalent());
        CommandHandler.registerCommand(userMappingCommands.removeTalent());


        api.addReconnectListener(event -> event.getApi().updateActivity(ActivityType.WATCHING, "Getting ready for live coverage!"));

        return true;
    }
}