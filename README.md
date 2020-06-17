# RaceSpotTV Live Bot

A Discord bot built using Spring Boot, Javacord3, SDCF4J and the Google Client library. Uses a H2 In-Memory to cache an event list that is updated using commands powered by SDCF4J. Uses Spring Boot Scheduling to run a task at a fixed rate interval to then check YT Event status's using the Client Library before posting to Discord.

###  Commands
These commands can only be issued by discord admins
* `!addYTevent`
