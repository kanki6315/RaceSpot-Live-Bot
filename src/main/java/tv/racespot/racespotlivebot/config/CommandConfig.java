package tv.racespot.racespotlivebot.config;

import org.javacord.api.DiscordApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tv.racespot.racespotlivebot.data.*;
import tv.racespot.racespotlivebot.service.commands.*;
import tv.racespot.racespotlivebot.service.rest.SheetsManager;

@Configuration
public class CommandConfig {

    @Autowired
    UserMappingRepository userMappingRepository;

    @Autowired
    private DServerRepository serverRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ScheduledEventRepository scheduledEventRepository;

    @Autowired
    private SeriesLogoRepository seriesLogoRepository;

    @Bean
    public EventCommands eventCommands(DiscordApi api) {
        return new EventCommands(api, eventRepository, serverRepository);
    }

    @Bean
    public ScheduleCommands scheduleCommands(DiscordApi api, SheetsManager sheetsManager) {
        return new ScheduleCommands(api, sheetsManager, scheduledEventRepository, userMappingRepository, seriesLogoRepository);
    }

    @Bean
    public SeriesLogoCommands seriesLogoCommands() {
        return new SeriesLogoCommands(seriesLogoRepository);
    }

    @Bean
    public ServerCommands serverCommands(DiscordApi api) {
        return new ServerCommands(serverRepository, api);
    }

    @Bean
    public UserMappingCommands userMappingCommands() {
        return new UserMappingCommands(userMappingRepository);
    }
}
