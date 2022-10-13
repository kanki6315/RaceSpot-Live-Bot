package tv.racespot.racespotlivebot.service.commands;

import me.s3ns3iw00.jcommands.Command;
import me.s3ns3iw00.jcommands.argument.ArgumentResult;
import me.s3ns3iw00.jcommands.argument.type.MentionArgument;
import me.s3ns3iw00.jcommands.argument.type.ValueArgument;
import me.s3ns3iw00.jcommands.type.SlashCommand;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import tv.racespot.racespotlivebot.data.UserMapping;
import tv.racespot.racespotlivebot.data.UserMappingRepository;

import java.util.List;

import static tv.racespot.racespotlivebot.util.TableFormatter.printTalent;

public class UserMappingCommands {

    @Value("${discord.notification.admin_channel_id}")
    private String adminChannelId;

    private final Logger logger;

    private final UserMappingRepository userRepository;

    public UserMappingCommands(
            final UserMappingRepository userMappingRepository) {
        this.userRepository = userMappingRepository;

        this.logger = LoggerFactory.getLogger(UserMappingCommands.class);
    }

    public Command addTalent() {
        SlashCommand addTalent = new SlashCommand("addtalent", "Add new broadcast talent to cache.");

        ValueArgument nameArgument = new ValueArgument("name", "First and last name separated with space", SlashCommandOptionType.STRING);
        // This argument only accepts two word separated with comma and each word started with capitalized letter.
        nameArgument.validate("(?<first>[A-Z][a-z]+) (?<last>[A-Z][a-z]+)");
        // Send back a message to the user when the user's input is not valid for the pattern
        // EPHEMERAL flag means that the response will only be visible for the user
        nameArgument.setOnMismatch(event -> {
            event.getResponder().respondNow()
                    .setContent("The name is not valid for the pattern. :face_with_raised_eyebrow:")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        MentionArgument mentionArgument = new MentionArgument("discorduser", "user to add");

        addTalent.addArgument(nameArgument, mentionArgument);
        addTalent.setOnAction(event -> {
            // Get the arguments from the event
            ArgumentResult[] args = event.getArguments();

            String name = args[0].get();
            User discordUser = args[1].get();

            UserMapping existingUser = userRepository.findByTalentNameIgnoreCase(name);
            if (existingUser != null) {
                event.getResponder().respondNow()
                        .setContent(String.format("Talent already exists with name %s", args[0]))
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            }

            existingUser = userRepository.findBydUserId(discordUser.getId());
            if (existingUser != null) {
                event.getResponder().respondNow()
                        .setContent(String.format("Talent already exists with username %s", discordUser.getDiscriminatedName()))
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            }

            UserMapping mapping = new UserMapping();
            mapping.setTalentName(name);
            mapping.setdUserId(discordUser.getId());
            userRepository.save(mapping);

            event.getResponder().respondNow()
                    .setContent("Talent added")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        return addTalent;
    }

    public Command listTalent() {
        SlashCommand listTalent = new SlashCommand("listtalent", "List broadcast talent in cache.");

        listTalent.setOnAction(event -> {

            List<UserMapping> mappings = userRepository.findAll();
            printTalent(mappings, event.getChannel().get());

            event.getResponder().respondNow()
                    .setContent("Talent printed")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        return listTalent;
    }

    public Command removeTalent() {
        SlashCommand removeTalent = new SlashCommand("removetalent", "Remove broadcast talent from cache.");

        ValueArgument nameArgument = new ValueArgument("name", "First and last name separated with space", SlashCommandOptionType.STRING);
        // This argument only accepts two word separated with comma and each word started with capitalized letter.
        nameArgument.validate("(?<first>[A-Z][a-z]+) (?<last>[A-Z][a-z]+)");
        // Send back a message to the user when the user's input is not valid for the pattern
        // EPHEMERAL flag means that the response will only be visible for the user
        nameArgument.setOnMismatch(event -> {
            event.getResponder().respondNow()
                    .setContent("The name is not valid for the pattern. :face_with_raised_eyebrow:")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        removeTalent.addArgument(nameArgument);
        removeTalent.setOnAction(event -> {
            // Get the arguments from the event
            ArgumentResult[] args = event.getArguments();

            String name = args[0].get();

            UserMapping mapping = userRepository.findByTalentNameIgnoreCase(name);
            if (mapping == null) {
                event.getResponder().respondNow()
                        .setContent(String.format("Talent does not exist with name %s", args[0]))
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            }
            userRepository.delete(mapping);

            event.getResponder().respondNow()
                    .setContent("Talent removed")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        return removeTalent;
    }

}
