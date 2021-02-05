/**
 * Copyright (C) 2021 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.service.executor;

import static tv.racespot.racespotlivebot.util.MessageUtil.hasAdminPermission;
import static tv.racespot.racespotlivebot.util.MessageUtil.notifyChecked;
import static tv.racespot.racespotlivebot.util.MessageUtil.notifyUnallowed;
import static tv.racespot.racespotlivebot.util.TableFormatter.printSeries;

import java.util.List;
import java.util.Optional;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import tv.racespot.racespotlivebot.data.SeriesLogo;
import tv.racespot.racespotlivebot.data.SeriesLogoRepository;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeriesLogoExecutor implements CommandExecutor {

    private final String adminChannelId;

    private final Logger logger;

    private final SeriesLogoRepository seriesLogoRepository;

    public SeriesLogoExecutor(
        final SeriesLogoRepository seriesLogoRepository,
        final String adminChannelId) {
        this.seriesLogoRepository = seriesLogoRepository;
        this.adminChannelId = adminChannelId;

        this.logger = LoggerFactory.getLogger(SeriesLogoExecutor.class);
    }

    @Command(aliases = "!addSeries")
    public void addSeries(String[] args, Message message, Server server, User user, TextChannel channel) {
        if (!hasAdminPermission(server, user) || !channel.getIdAsString().equalsIgnoreCase(adminChannelId)) {
            return;
        }

        if(args.length != 2) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append("Unable to create series without correct input")
                .send(channel);
            return;
        }

        SeriesLogo existingLogo = seriesLogoRepository.findBySeriesNameIgnoreCase(args[0]);
        if (existingLogo != null) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append(String.format("Series already exists with name %s", args[0]))
                .send(channel);
            return;
        }

        if (StringUtils.isNumeric(args[1])) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append(String.format("2nd input must be a thumbnail url instead of: %s", args[1]))
                .send(channel);
            return;
        }

        SeriesLogo mapping = new SeriesLogo();
        mapping.setSeriesName(args[0]);
        mapping.setThumbnailUrl(args[1]);
        seriesLogoRepository.save(mapping);
        notifyChecked(message);
    }

    @Command(aliases = "!listSeries")
    public void listSeries(Server server, User user, TextChannel channel) {
        if (!hasAdminPermission(server, user) || !channel.getIdAsString().equalsIgnoreCase(adminChannelId)) {
            return;
        }
        List<SeriesLogo> logos = seriesLogoRepository.findAll();
        printSeries(logos, channel);
    }

    @Command(aliases = "!removeSeries")
    public void removeSeries(String[] args, Server server, User user, TextChannel channel, Message message) {
        if (!hasAdminPermission(server, user) || !channel.getIdAsString().equalsIgnoreCase(adminChannelId)) {
            return;
        }

        if(args.length != 1) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append("Unable to find series without correct input")
                .send(channel);
            return;
        }

        SeriesLogo logo = seriesLogoRepository.findBySeriesNameIgnoreCase(args[0]);
        if(logo == null) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append(String.format("Series does not exist with name %s", args[0]))
                .send(channel);
            return;
        }

        seriesLogoRepository.delete(logo);
        notifyChecked(message);
    }
}