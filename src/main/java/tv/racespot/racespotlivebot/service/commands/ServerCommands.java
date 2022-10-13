package tv.racespot.racespotlivebot.service.commands;

import me.s3ns3iw00.jcommands.Command;
import me.s3ns3iw00.jcommands.argument.ArgumentResult;
import me.s3ns3iw00.jcommands.argument.type.ValueArgument;
import me.s3ns3iw00.jcommands.type.SlashCommand;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import tv.racespot.racespotlivebot.data.DServer;
import tv.racespot.racespotlivebot.data.DServerRepository;

import java.util.List;
import java.util.Optional;

import static tv.racespot.racespotlivebot.util.TableFormatter.printServers;

public class ServerCommands {

    @Value("${discord.notification.admin_channel_id}")
    private String adminChannelId;

    private final DServerRepository serverRepository;

    private final Logger logger;

    private DiscordApi api;

    public ServerCommands(
            final DServerRepository serverRepository,
            final DiscordApi api) {
        this.serverRepository = serverRepository;
        this.api = api;

        this.logger = LoggerFactory.getLogger(ServerCommands.class);
    }

    public Command addServer() {

        SlashCommand addServerCommand = new SlashCommand("addserver", "Add new server to cache.");

        ValueArgument serverIdArgument = new ValueArgument("serverId", "Server Id", SlashCommandOptionType.LONG);
        ValueArgument channelIdArgument = new ValueArgument("channelId", "Channel Id", SlashCommandOptionType.LONG);

        addServerCommand.addArgument(serverIdArgument, channelIdArgument);
        addServerCommand.setOnAction(event -> {
            // Get the arguments from the event
            ArgumentResult[] args = event.getArguments();

            String serverId = args[0].get();
            String channelId = args[1].get();

            List<DServer> dServers = serverRepository.findBydServerId(serverId);
            if(dServers.size() > 0) {
                event.getResponder().respondNow()
                        .setContent(String.format("Server with id %s already exists", serverId))
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            }

            Optional<Server> optionalServer = api.getServerById(serverId);
            if(!optionalServer.isPresent()) {
                event.getResponder().respondNow()
                        .setContent(String.format("Bot is not in server with id %s", serverId))
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            }

            Server newServer = optionalServer.get();
            if(!newServer.getChannelById(channelId).isPresent()) {
                event.getResponder().respondNow()
                        .setContent(String.format("Server does not contain channel with id %s", channelId))
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            }

            DServer dServer = new DServer();
            dServer.setDServerId(serverId);
            dServer.setDChannelId(channelId);
            dServer.setDName(optionalServer.get().getName());

            serverRepository.save(dServer);

            event.getResponder().respondNow()
                    .setContent("Server added")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        return addServerCommand;
    }

    public Command listServer() {
        SlashCommand listServerCommand = new SlashCommand("listservers", "List servers in cache.");

        listServerCommand.setOnAction(event -> {

            List<DServer> dServers = serverRepository.findAll();
            printServers(dServers, event.getChannel().get());

            event.getResponder().respondNow()
                    .setContent("Servers printed")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        return listServerCommand;
    }

}
