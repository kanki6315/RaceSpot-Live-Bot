package tv.racespot.racespotlivebot.service.commands;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import me.s3ns3iw00.jcommands.Command;
import me.s3ns3iw00.jcommands.argument.ArgumentResult;
import me.s3ns3iw00.jcommands.argument.type.ValueArgument;
import me.s3ns3iw00.jcommands.type.SlashCommand;
import org.apache.commons.lang3.StringUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionCallbackDataFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import tv.racespot.racespotlivebot.data.*;
import tv.racespot.racespotlivebot.data.Event;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class EventCommands {

    @Value("${google.api_key}")
    private String googleApiKey;
    @Value("${discord.notification.admin_channel_id}")
    private String adminChannelId;

    private final EventRepository eventRepository;
    private final DServerRepository serverRepository;

    private YouTube youtubeClient;

    private final Logger logger;

    private DiscordApi api;

    public EventCommands(
            final DiscordApi api,
            final EventRepository eventRepository,
            final DServerRepository serverRepository) {
        this.api = api;
        this.eventRepository = eventRepository;
        this.serverRepository = serverRepository;

        this.youtubeClient = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("racespot-tv-live-bot").build();

        this.logger = LoggerFactory.getLogger(EventCommands.class);
    }

    @Scheduled(fixedRate = 300000, initialDelay = 10000)
    public void checkScheduledEvents() {
        logger.info("beginning scheduled check");
        List<Event> events = eventRepository.findByStatus(EventStatus.SCHEDULED);
        if(events.size() == 0) {
            logger.info("No events scheduled!");
            return;
        }
        logger.info(String.format("%d events to check", events.size()));

        try {
            YouTube.Videos.List search = youtubeClient.videos().list(Arrays.asList("id", "snippet"));
            search.setKey(googleApiKey);
            search.setId(events.stream().map(Event::getYoutubeLink).collect(Collectors.toList()));
            //search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url,snippet/liveBroadcastContent)");

            int counter = 0;
            VideoListResponse searchResponse = search.execute();
            List<Video> videos = searchResponse.getItems();
            Map<Event, Video> eventToVideoMap = new HashMap<>();
            List<String> channelIds = new ArrayList<>();
            if (videos != null) {
                for (Video video : videos) {
                    if (!"upcoming".equalsIgnoreCase(video.getSnippet().getLiveBroadcastContent())) {
                        Event event =
                                events.stream().filter(e -> e.getYoutubeLink().equals(video.getId()))
                                        .findFirst().get();
                        eventToVideoMap.put(event, video);
                        if(!channelIds.contains(video.getSnippet().getChannelId())) {
                            channelIds.add(video.getSnippet().getChannelId());
                        }
                        counter++;
                    }
                }
            }

            if(!eventToVideoMap.isEmpty()) {
                Map<String, String> channelIdToAvatarMap = getChannelAvatarMap(channelIds);

                List<DServer> servers = serverRepository.findAll();

                for (Event event : eventToVideoMap.keySet()) {
                    event.setStatus(EventStatus.LIVE);
                    eventRepository.save(event);

                    Video video = eventToVideoMap.get(event);

                    for (DServer server : servers) {
                        try {
                            ServerTextChannel
                                    channel = api.getServerTextChannelById(server.getDChannelId()).get();
                            new MessageBuilder()
                                    .setEmbed(constructEmbed(video, channelIdToAvatarMap))
                                    .send(channel);
                        } catch (Exception ex) {
                            logger.error(ex.getMessage());
                            ServerTextChannel
                                    channel = api.getServerTextChannelById(adminChannelId).get();
                            new MessageBuilder()
                                    .append(String.format("Error while sending live announcement for server %s: %s",
                                            server.getDName(), ex.getMessage()))
                                    .send(channel);
                        }
                    }
                    counter++;
                    logger.info(String.format("Event is live: %S", event.getYoutubeLink()));
                }
            }
            logger.info(String.format("finished checking events: %d events live", counter));
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            ServerTextChannel
                    channel = api.getServerTextChannelById(adminChannelId).get();
            new MessageBuilder()
                    .append(String.format("Error while syncing event status: %s", ex.getMessage()))
                    .send(channel);
        }
        logger.info("finished scheduled task");
    }

    private Map<String, String> getChannelAvatarMap(final List<String> channelIds) throws IOException {
        YouTube.Channels.List search = youtubeClient.channels().list(Arrays.asList("id","snippet"));
        search.setKey(googleApiKey);
        search.setId(channelIds);

        ChannelListResponse searchResponse = search.execute();
        List<Channel> channels = searchResponse.getItems();
        Map<String, String> idToUrlMap = new HashMap<>();
        for(Channel channel : channels) {
            if(StringUtils.isNotEmpty(channel.getSnippet().getThumbnails().getDefault().getUrl())) {
                idToUrlMap.put(channel.getId(), channel.getSnippet().getThumbnails().getDefault().getUrl());
            } else {
                idToUrlMap.put(channel.getId(), "https://cdn.discordapp.com/icons/291983345133289476/4737f24e20bbe4e78104819daffe0bb1.png?size=256");
            }
        }
        return idToUrlMap;
    }

    public Command addYoutubeEvent() {
        SlashCommand addYTEventCommand = new SlashCommand("addytevent", "Add new YT event for notification");
        ValueArgument urlArgument = new ValueArgument("youtubeURL", "YouTube Event URL", SlashCommandOptionType.STRING);
        urlArgument.validate("^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch?v=)([^#&?]*).*$");
        urlArgument.setOnMismatch(event -> {
            event.getResponder().respondNow()
                    .setContent("This does not appear to be a valid YouTube URL. :face_with_raised_eyebrow:")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });

        addYTEventCommand.addArgument(urlArgument);
        addYTEventCommand.setOnAction(event -> {
            event.getResponder().respondLater().thenAccept(updater -> {
                updater.setContent("Adding event").update();

                ArgumentResult[] args = event.getArguments();

                Matcher regexMatcher = args[0].get();
                String youtubeId = regexMatcher.group(1);

                try {
                    YouTube.Videos.List search = youtubeClient.videos().list(Arrays.asList("id","snippet"));
                    search.setKey(googleApiKey);
                    search.setId(Collections.singletonList(youtubeId));
                    //search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url,snippet/liveBroadcastContent)");

                    VideoListResponse searchResponse = search.execute();
                    List<Video> searchResultList = searchResponse.getItems();
                    if (searchResultList != null && searchResultList.size() == 1) {
                        logger.info("found video");
                        Video stream = searchResultList.get(0);
                        if("upcoming".equalsIgnoreCase(stream.getSnippet().getLiveBroadcastContent())) {
                            Event ytEvent = new Event(youtubeId);
                            eventRepository.save(ytEvent);
                        } else {
                            event.getResponder().followUp()
                                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                                    .setContent(String.format("Event is not in upcoming state. State = %s", stream.getSnippet().getLiveBroadcastContent()))
                                    .send();
                            return;
                        }
                    } else {
                        event.getResponder().followUp()
                                .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                                .setContent("Unable to find video with submitted id")
                                .send();
                        return;
                    }
                } catch(IOException ioEx) {
                    event.getResponder().followUp()
                            .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                            .setContent("Adding Event Failed + " + ioEx.getMessage())
                            .send();
                    return;
                }
            });

        });

        return addYTEventCommand;
    }

    public Command clear() {
        SlashCommand clearCommand = new SlashCommand("clearevents", "Clear Completed Events");

        clearCommand.setOnAction(event -> {
            List<Event> events = eventRepository.findByStatus(EventStatus.LIVE);
            eventRepository.deleteAll(events);

            event.getResponder().respondNow()
                    .setContent("Events cleared")
                    .setFlags(InteractionCallbackDataFlag.EPHEMERAL)
                    .respond();
        });
        return clearCommand;
    }

    private EmbedBuilder constructEmbed(Video video, Map<String, String> channelIdToAvatarMap) {
        return new EmbedBuilder()
                .setAuthor(video.getSnippet().getChannelTitle())
                .setTitle(video.getSnippet().getTitle())
                .setDescription("Stream has just gone live!")
                .setColor(Color.YELLOW)
                .setThumbnail(channelIdToAvatarMap.get(video.getSnippet().getChannelId()))
                .setImage(video.getSnippet().getThumbnails().getMaxres().getUrl())
                .setUrl(String.format("https://www.youtube.com/watch?v=%s", video.getId()));
    }
}
