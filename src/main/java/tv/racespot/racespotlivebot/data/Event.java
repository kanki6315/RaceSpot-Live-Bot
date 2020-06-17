/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.data;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    private String id;

    private String youtubeLink;
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    public Event() {
    }

    public Event(String url) {
        this.youtubeLink = url;
        this.status = EventStatus.SCHEDULED;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getYoutubeLink() {
        return youtubeLink;
    }

    public void setYoutubeLink(final String youtubeLink) {
        this.youtubeLink = youtubeLink;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(final EventStatus status) {
        this.status = status;
    }
}