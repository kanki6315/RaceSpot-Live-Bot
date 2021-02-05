/**
 * Copyright (C) 2021 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.data;

import java.util.HashSet;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMappingRepository extends JpaRepository<UserMapping, String> {

    UserMapping findBydUserId(Long DUserId);

    UserMapping findByTalentNameIgnoreCase(String talentName);

    List<UserMapping> findByTalentNameIn(HashSet<String> talentNames);
}
