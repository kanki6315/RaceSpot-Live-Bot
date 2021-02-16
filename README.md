# RaceSpotTV Live Bot

A Discord bot built for RaceSpot TV to power Live YouTube notifications across any channel as well as automated broadcast schedule management with an integration with Google Sheets. This is powered by Spring Boot, Javacord3, SDCF4J and the Google Client library. Backed by a PSQL db that is updated using commands powered by SDCF4J. Uses Spring Boot Scheduling to run a task at a fixed rate interval to then check YT Event status's using the Client Library before posting to Discord.

###  Commands
These commands can only be issued by discord admins

Used for YouTube Notifications
* `!addYTevent <YouTube Video ID>`
* `!clear`

Used for Adding Servers to YouTube Notifications
* `!addServer <Server Id> <Channel Id>`
* `!listServers`

Used for Schedule Posting
* `!postSchedule`
* `!updateSchedule`
* `!clearSchedule`

Used for Adding Talent to Schedule Notifications
* `!addTalent <Talent Name> <Talent Discord ID>`
* `!removeTalent <Talent Name>`
* `!listTalent `

Used for Adding Series Logos to Schedule
* `!addSeries <Series Name> <Thumnbail URL>`
* `!removeSeries <Series Name>`
* `!listSeries`