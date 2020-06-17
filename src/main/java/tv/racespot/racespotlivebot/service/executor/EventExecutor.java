/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.service.executor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.btobastian.sdcf4j.Command;
import de.btobastian.sdcf4j.CommandExecutor;
import tv.racespot.racespotlivebot.data.Event;
import tv.racespot.racespotlivebot.data.EventRepository;
import tv.racespot.racespotlivebot.data.EventStatus;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
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
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

public class EventExecutor implements CommandExecutor {

    private final String googleApiKey;
    private final String annoucementChannelId;
    private final String errorChannelId;

    private final EventRepository eventRepository;

    private YouTube youtubeClient;

    private final Logger logger;

    private DiscordApi api;

    public EventExecutor(
        final DiscordApi api,
        final String googleApiKey,
        final EventRepository eventRepository,
        final String annoucementChannelId,
        final String errorChannelId) {
        this.googleApiKey = googleApiKey;
        this.eventRepository = eventRepository;
        this.annoucementChannelId = annoucementChannelId;
        this.errorChannelId = errorChannelId;

        this.youtubeClient = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("racespot-tv-live-bot").build();

        this.logger = LoggerFactory.getLogger(EventExecutor.class);
    }

    @Scheduled(fixedRate = 1500000)
    public void checkScheduledEvents() {
        List<Event> events = eventRepository.findByStatus(EventStatus.SCHEDULED);
        if(events.size() == 0) {
            return;
        }

        try {
            YouTube.Videos.List search = youtubeClient.videos().list(Arrays.asList("id", "snippet"));
            search.setKey(googleApiKey);
            search.setId(events.stream().map(Event::getYoutubeLink).collect(Collectors.toList()));
            //search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url,snippet/liveBroadcastContent)");

            VideoListResponse searchResponse = search.execute();
            List<Video> searchResultList = searchResponse.getItems();
            if (searchResultList != null) {
                for (Video searchResult : searchResultList) {
                    if (!"upcoming".equalsIgnoreCase(searchResult.getSnippet().getLiveBroadcastContent())) {
                        Event event =
                            events.stream().filter(e -> e.getYoutubeLink().equals(searchResult.getId()))
                                .findFirst().get();
                        event.setStatus(EventStatus.LIVE);
                        eventRepository.save(event);

                        ServerTextChannel
                            channel = api.getServerTextChannelById(annoucementChannelId).get();
                        new MessageBuilder()
                            .append(String.format("Event is live: %S", event.getYoutubeLink()))
                            .send(channel);
                    }
                }
            }
        } catch (Exception ex) {
            ServerTextChannel
                channel = api.getServerTextChannelById(errorChannelId).get();
            new MessageBuilder()
                .append(String.format("Error while syncing event status: %s", ex.getMessage()))
                .send(channel);
        }
    }

    @Command(aliases = "!addYTevent", description = "Add Event to DB", usage = "!event [YouTube URL]")
    public void addYoutubeEvent(String[] args, Message message, Server server, User user, TextChannel channel)
        throws IOException {

        if(!hasAdminPermission(server, user)) {
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

    public void notifyChecked(Message message) {
        message.addReaction("👍");
    }

    public static void notifyFailed(Message message) {
        message.addReaction("👎");
    }
}