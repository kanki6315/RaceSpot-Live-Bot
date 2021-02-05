/**
 * Copyright (C) 2021 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.service.executor;

import static tv.racespot.racespotlivebot.util.MessageUtil.hasAdminPermission;
import static tv.racespot.racespotlivebot.util.MessageUtil.notifyChecked;
import static tv.racespot.racespotlivebot.util.MessageUtil.notifyUnallowed;
import static tv.racespot.racespotlivebot.util.TableFormatter.printTalent;

import java.util.List;
import java.util.Optional;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import tv.racespot.racespotlivebot.data.UserMapping;
import tv.racespot.racespotlivebot.data.UserMappingRepository;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMappingExecutor implements CommandExecutor {

    private final String adminChannelId;

    private final Logger logger;

    private final UserMappingRepository userRepository;

    public UserMappingExecutor(
        final UserMappingRepository userMappingRepository,
        final String adminChannelId) {
        this.userRepository = userMappingRepository;
        this.adminChannelId = adminChannelId;

        this.logger = LoggerFactory.getLogger(UserMappingExecutor.class);
    }

    @Command(aliases = "!addTalent")
    public void addTalent(String[] args, Message message, Server server, User user, TextChannel channel) {
        if (!hasAdminPermission(server, user) || !channel.getIdAsString().equalsIgnoreCase(adminChannelId)) {
            return;
        }

        if(args.length != 2) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append("Unable to find user without correct input")
                .send(channel);
            return;
        }

        UserMapping existingUser = userRepository.findByTalentNameIgnoreCase(args[0]);
        if (existingUser != null) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append(String.format("Talent already exists with name %s", args[0]))
                .send(channel);
            return;
        }

        if (!StringUtils.isNumeric(args[1])) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append(String.format("2nd input must be a discord numeric id instead of: %s", args[1]))
                .send(channel);
            return;
        }

        Optional<User> dUser = server.getMemberById(args[1]);
        if(!dUser.isPresent()) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append(String.format("Unable to find user with id %s in discord server", args[1]))
                .send(channel);
            return;
        }

        UserMapping mapping = new UserMapping();
        mapping.setTalentName(args[0]);
        mapping.setdUserId(dUser.get().getId());
        userRepository.save(mapping);
        notifyChecked(message);
    }

    @Command(aliases = "!listTalent")
    public void listTalent(Server server, User user, TextChannel channel) {
        if (!hasAdminPermission(server, user) || !channel.getIdAsString().equalsIgnoreCase(adminChannelId)) {
            return;
        }
        List<UserMapping> mappings = userRepository.findAll();
        printTalent(mappings, channel);
    }

    @Command(aliases = "!removeTalent")
    public void removeTalent(String[] args, Server server, User user, TextChannel channel, Message message) {
        if (!hasAdminPermission(server, user) || !channel.getIdAsString().equalsIgnoreCase(adminChannelId)) {
            return;
        }

        if(args.length != 1) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append("Unable to find user without correct input")
                .send(channel);
            return;
        }

        UserMapping mapping = userRepository.findByTalentNameIgnoreCase(args[0]);
        if(mapping == null) {
            notifyUnallowed(message);
            new MessageBuilder()
                .append(String.format("Talent does not exist with name %s", args[0]))
                .send(channel);
            return;
        }

        userRepository.delete(mapping);
        notifyChecked(message);
    }
}