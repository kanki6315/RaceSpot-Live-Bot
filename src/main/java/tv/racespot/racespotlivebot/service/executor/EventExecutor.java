/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.service.executor;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import tv.racespot.racespotlivebot.data.Event;
import tv.racespot.racespotlivebot.data.EventRepository;
import tv.racespot.racespotlivebot.data.EventStatus;

import org.apache.commons.lang3.StringUtils;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

public class EventExecutor implements CommandExecutor {

    private final String googleApiKey;
    private final String announcementChannelId;
    private final String adminChannelId;

    private final EventRepository eventRepository;

    private YouTube youtubeClient;

    private final Logger logger;

    private DiscordApi api;

    public EventExecutor(
        final DiscordApi api,
        final String googleApiKey,
        final EventRepository eventRepository,
        final String announcementChannelId,
        final String adminChannelId) {
        this.api = api;
        this.googleApiKey = googleApiKey;
        this.eventRepository = eventRepository;
        this.announcementChannelId = announcementChannelId;
        this.adminChannelId = adminChannelId;

        this.youtubeClient = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("racespot-tv-live-bot").build();

        this.logger = LoggerFactory.getLogger(EventExecutor.class);
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

                for (Event event : eventToVideoMap.keySet()) {
                    event.setStatus(EventStatus.LIVE);
                    eventRepository.save(event);

                    Video video = eventToVideoMap.get(event);

                    ServerTextChannel
                        channel = api.getServerTextChannelById(announcementChannelId).get();
                    new MessageBuilder()
                        .setEmbed(constructEmbed(video, channelIdToAvatarMap))
                        .send(channel);
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

    @Command(aliases = "!testEmbed")
    public void testEmbed(String[] args, Message message, Server server, User user, TextChannel channel) {
        if(!hasAdminPermission(server, user) && args.length != 1) {
            return;
        }

        try {
            YouTube.Videos.List search = youtubeClient.videos().list(Arrays.asList("id","snippet"));
            search.setKey(googleApiKey);
            search.setId(Collections.singletonList(args[0]));
            //search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url,snippet/liveBroadcastContent)");

            VideoListResponse searchResponse = search.execute();
            List<Video> searchResultList = searchResponse.getItems();
            if (searchResultList != null && searchResultList.size() == 1) {
                logger.info("found video");
                Video stream = searchResultList.get(0);
                Map<String, String> channelAvatar = getChannelAvatarMap(Collections.singletonList(stream.getSnippet().getChannelId()));
                new MessageBuilder()
                    .setEmbed(constructEmbed(stream, channelAvatar))
                    .send(channel);
            } else {
                new MessageBuilder()
                    .append("Unable to find video with submitted id")
                    .send(channel);
                notifyFailed(message);
            }
        } catch(IOException ioEx) {
            notifyFailed(message);
        }
    }

    @Command(aliases = "!addYTevent", description = "Add Event to DB", usage = "!event [YouTube URL]")
    public void addYoutubeEvent(String[] args, Message message, Server server, User user, TextChannel channel)
        throws IOException {

        if(!adminChannelId.equals(Long.toString(channel.getId()))) {
            return;
        }
        if(!hasAdminPermission(server, user) && args.length != 1) {
            notifyUnallowed(message);
            return;
        }

        try {
            YouTube.Videos.List search = youtubeClient.videos().list(Arrays.asList("id","snippet"));
            search.setKey(googleApiKey);
            search.setId(Collections.singletonList(args[0]));
            //search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url,snippet/liveBroadcastContent)");

            VideoListResponse searchResponse = search.execute();
            List<Video> searchResultList = searchResponse.getItems();
            if (searchResultList != null && searchResultList.size() == 1) {
                logger.info("found video");
                Video stream = searchResultList.get(0);
                if("upcoming".equalsIgnoreCase(stream.getSnippet().getLiveBroadcastContent())) {
                    Event event = new Event(args[0]);
                    eventRepository.save(event);
                    notifyChecked(message);
                } else {
                    new MessageBuilder()
                        .append(String.format("Event is not in upcoming state. State = %s", stream.getSnippet().getLiveBroadcastContent()))
                        .send(channel);
                    notifyFailed(message);
                }
            } else {
                new MessageBuilder()
                    .append("Unable to find video with submitted id")
                    .send(channel);
                notifyFailed(message);
            }
        } catch(IOException ioEx) {
            notifyFailed(message);
        }
    }

    private boolean hasAdminPermission(Server server, User user) {

        List<Role> roles = user.getRoles(server);
        return roles.stream()
            .map(Role::getAllowedPermissions)
            .flatMap(Collection::stream)
            .anyMatch(role -> role.equals(PermissionType.ADMINISTRATOR));
    }

    private void notifyChecked(Message message) {
        message.addReaction("👍");
    }

    private void notifyFailed(Message message) {
        message.addReaction("👎");
    }

    private void notifyUnallowed(Message message) {
        message.addReaction("❌");
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