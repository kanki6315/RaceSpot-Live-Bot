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
@Table(name = "user_mapping")
public class UserMapping {

    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    private String id;

    private String talentName;

    private long dUserId;

    public UserMapping() {
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTalentName() {
        return talentName;
    }

    public void setTalentName(final String talentName) {
        this.talentName = talentName;
    }

    public long getdUserId() {
        return dUserId;
    }

    public void setdUserId(final long dUserId) {
        this.dUserId = dUserId;
    }
}