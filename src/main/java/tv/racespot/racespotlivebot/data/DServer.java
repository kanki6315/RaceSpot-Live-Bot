/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "server")
public class DServer {

    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    private String id;

    private String dServerId;

    private String dName;
    private String dChannelId;

    public DServer() {
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getDServerId() {
        return dServerId;
    }

    public void setDServerId(final String dServerId) {
        this.dServerId = dServerId;
    }

    public String getDChannelId() {
        return dChannelId;
    }

    public void setDChannelId(final String dChannelId) {
        this.dChannelId = dChannelId;
    }

    public String getDName() {
        return dName;
    }

    public void setDName(final String dName) {
        this.dName = dName;
    }
}