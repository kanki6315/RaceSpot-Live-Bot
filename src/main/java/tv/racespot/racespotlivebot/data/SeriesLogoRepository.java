/**
 * Copyright (C) 2021 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeriesLogoRepository extends JpaRepository<SeriesLogo, String> {

    SeriesLogo findBySeriesNameIgnoreCase(String seriesName);
}