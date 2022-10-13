package tv.racespot.racespotlivebot.service.commands;

import me.s3ns3iw00.jcommands.Command;
import me.s3ns3iw00.jcommands.argument.ArgumentResult;
import me.s3ns3iw00.jcommands.argument.type.ValueArgument;
import me.s3ns3iw00.jcommands.type.SlashCommand;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import tv.racespot.racespotlivebot.data.SeriesLogo;
import tv.racespot.racespotlivebot.data.SeriesLogoRepository;

import java.util.List;

import static tv.racespot.racespotlivebot.util.TableFormatter.printSeries;

public class SeriesLogoCommands {

    @Value("${discord.notification.admin_channel_id}")
    private String adminChannelId;

    private final Logger logger;

    private final SeriesLogoRepository seriesLogoRepository;

    public SeriesLogoCommands(
            final SeriesLogoRepository seriesLogoRepository) {
        this.seriesLogoRepository = seriesLogoRepository;

        this.logger = LoggerFactory.getLogger(SeriesLogoCommands.class);
    }

    public Command addSeries() {

        SlashCommand addSeriesCommand = new SlashCommand("addseries", "Add new series to cache.");

        ValueArgument seriesNameArgument = new ValueArgument("seriesName", "Series Name", SlashCommandOptionType.STRING);
        ValueArgument imageUrlArgument = new ValueArgument("imageUrl", "Image URL", SlashCommandOptionType.STRING);

        addSeriesCommand.addArgument(seriesNameArgument, imageUrlArgument);
        addSeriesCommand.setOnAction(event -> {
            ArgumentResult[] args = event.getArguments();

            String seriesName = args[0].get();
            String imageUrl = args[1].get();

            SeriesLogo existingLogo = seriesLogoRepository.findBySeriesNameIgnoreCase(seriesName);
            if (existingLogo != null) {
                event.getResponder().respondNow()
                        .setContent(String.format("Series already exists with name %s", seriesName))
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            }

            SeriesLogo mapping = new SeriesLogo();
            mapping.setSeriesName(seriesName);
            mapping.setThumbnailUrl(imageUrl);
            seriesLogoRepository.save(mapping);

            event.getResponder().respondNow()
                    .setContent("Series added")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        return addSeriesCommand;
    }

    public Command listSeries() {
        SlashCommand listSeriesCommand = new SlashCommand("listseries", "List series in cache.");

        listSeriesCommand.setOnAction(event -> {
            TextChannel channel = event.getChannel().get();

            List<SeriesLogo> logos = seriesLogoRepository.findAll();
            printSeries(logos, channel);
            event.getResponder().respondNow()
                    .setContent("Series posted")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        return listSeriesCommand;
    }

    public Command removeSeries() {

        SlashCommand removeSeriesCommand = new SlashCommand("removeseries", "Remove series from cache.");

        ValueArgument seriesNameArgument = new ValueArgument("seriesName", "Series Name", SlashCommandOptionType.STRING);

        removeSeriesCommand.addArgument(seriesNameArgument);
        removeSeriesCommand.setOnAction(event -> {
            ArgumentResult[] args = event.getArguments();

            String seriesName = args[0].get();

            SeriesLogo existingLogo = seriesLogoRepository.findBySeriesNameIgnoreCase(seriesName);
            if(existingLogo == null) {
                event.getResponder().respondNow()
                        .setContent(String.format("Series does not exist with name %s", seriesName))
                        .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                        .respond();
                return;
            }

            seriesLogoRepository.delete(existingLogo);

            event.getResponder().respondNow()
                    .setContent("Series deleted")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        return removeSeriesCommand;
    }
}
