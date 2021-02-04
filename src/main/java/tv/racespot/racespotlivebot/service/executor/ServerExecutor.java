/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.service.executor;

import static tv.racespot.racespotlivebot.util.MessageUtil.hasAdminPermission;
import static tv.racespot.racespotlivebot.util.MessageUtil.notifyChecked;
import static tv.racespot.racespotlivebot.util.MessageUtil.notifyFailed;
import static tv.racespot.racespotlivebot.util.MessageUtil.notifyUnallowed;
import static tv.racespot.racespotlivebot.util.TableFormatter.printServers;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import tv.racespot.racespotlivebot.data.DServer;
import tv.racespot.racespotlivebot.data.DServerRepository;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerExecutor implements CommandExecutor {

    private final String adminChannelId;

    private final DServerRepository serverRepository;

    private final Logger logger;

    private DiscordApi api;

    public ServerExecutor(
        final String adminChannelId,
        final DServerRepository serverRepository,
        final DiscordApi api) {
        this.adminChannelId = adminChannelId;
        this.serverRepository = serverRepository;
        this.api = api;

        this.logger = LoggerFactory.getLogger(ServerExecutor.class);;

    }

    @Command(aliases = "!addServer")
    public void addServer(String[] args, Message message, Server server, User user, TextChannel channel) {

        if(!adminChannelId.equals(Long.toString(channel.getId()))) {
            return;
        }
        if(!hasAdminPermission(server, user) || args.length != 2) {
            notifyUnallowed(message);
            return;
        }

        String serverId = args[0];
        String channelId = args[1];

        List<DServer> dServers = serverRepository.findBydServerId(serverId);
        if(dServers.size() > 0) {
            new MessageBuilder()
                .append(String.format("Server with id %s already exists", serverId))
                .send(channel);
            notifyFailed(message);
        }

        Optional<Server> optionalServer = api.getServerById(serverId);
        if(!optionalServer.isPresent()) {
            new MessageBuilder()
                .append(String.format("Bot is not in server with id %s", serverId))
                .send(channel);
            notifyFailed(message);
            return;
        }
        Server newServer = optionalServer.get();
        if(!newServer.getChannelById(channelId).isPresent()) {
            new MessageBuilder()
                .append(String.format("Server does not contain channel with id %s", channelId))
                .send(channel);
            notifyFailed(message);
            return;
        }

        DServer dServer = new DServer();
        dServer.setDServerId(serverId);
        dServer.setDChannelId(channelId);
        dServer.setDName(optionalServer.get().getName());

        serverRepository.save(dServer);
        notifyChecked(message);
    }

    @Command(aliases = "!listServers")
    public void listServers(Message message, Server server, User user, TextChannel channel) {

        if(!adminChannelId.equals(Long.toString(channel.getId()))) {
            return;
        }
        if(!hasAdminPermission(server, user)) {
            notifyUnallowed(message);
            return;
        }

        List<DServer> dServers = serverRepository.findAll();
        printServers(dServers, channel);
        notifyChecked(message);
    }
}