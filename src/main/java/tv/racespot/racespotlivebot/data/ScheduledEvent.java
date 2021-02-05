/**
 * Copyright (C) 2021 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "scheduled_event")
public class ScheduledEvent {

    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    private String id;

    private String date;

    private String time;

    private boolean isPublic;

    private String seriesName;

    private String description;

    private String producer;

    private String leadCommentator;

    private String colourOne;

    private String colourTwo;

    private String streamLocation;

    private Long dMessageId;

    private boolean isWebcam;

    private int index;

    private String notes;

    private float red;
    private float green;
    private float blue;

    public ScheduledEvent() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(final String time) {
        this.time = time;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(final boolean aPublic) {
        isPublic = aPublic;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(final String seriesName) {
        this.seriesName = seriesName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(final String producer) {
        this.producer = producer;
    }

    public String getLeadCommentator() {
        return leadCommentator;
    }

    public void setLeadCommentator(final String leadCommentator) {
        this.leadCommentator = leadCommentator;
    }

    public String getColourOne() {
        return colourOne;
    }

    public void setColourOne(final String colourOne) {
        this.colourOne = colourOne;
    }

    public String getColourTwo() {
        return colourTwo;
    }

    public void setColourTwo(final String colourTwo) {
        this.colourTwo = colourTwo;
    }

    public String getStreamLocation() {
        return streamLocation;
    }

    public void setStreamLocation(final String streamLocation) {
        this.streamLocation = streamLocation;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Long getdMessageId() {
        return dMessageId;
    }

    public void setdMessageId(final Long dMessageId) {
        this.dMessageId = dMessageId;
    }

    public boolean isWebcam() {
        return isWebcam;
    }

    public void setWebcam(final boolean webcam) {
        isWebcam = webcam;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public float getRed() {
        return red;
    }

    public void setRed(final float red) {
        this.red = red;
    }

    public float getGreen() {
        return green;
    }

    public void setGreen(final float green) {
        this.green = green;
    }

    public float getBlue() {
        return blue;
    }

    public void setBlue(final float blue) {
        this.blue = blue;
    }
}