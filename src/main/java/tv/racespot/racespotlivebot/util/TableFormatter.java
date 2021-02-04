/**
 * Copyright (C) 2021 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.bretty.console.table.Alignment;
import io.bretty.console.table.ColumnFormatter;
import io.bretty.console.table.Table;
import tv.racespot.racespotlivebot.data.DServer;
import tv.racespot.racespotlivebot.data.UserMapping;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;

public class TableFormatter {

    public static void printServers(List<DServer> servers, TextChannel channel) {

        if (servers.size() == 0) {
            new MessageBuilder()
                .append("Severs\n", MessageDecoration.BOLD)
                .append("```")
                .append("No servers found")
                .append("```")
                .send(channel);
            return;
        }

        List<String> serverNames = new ArrayList<>();
        List<String> discordIds = new ArrayList<>();

        for (DServer server : servers) {
            serverNames.add(server.getDName());
            discordIds.add(server.getDServerId());
        }

        Table.Builder builder = new Table.Builder(
            "Serverss",
            serverNames.toArray(new String[0]),
            getStringFormatterWithWidth(serverNames));
        addStringColumnToTable(builder, "Discord ID", discordIds);

        Table table = builder.build();

        new MessageBuilder()
            .append("Servers\n", MessageDecoration.BOLD)
            .append("```")
            .append(table.toString())
            .append("```")
            .send(channel);
    }

    public static void printTalent(List<UserMapping> talentMappings, TextChannel channel) {

        if (talentMappings.size() == 0) {
            new MessageBuilder()
                .append("Talent\n", MessageDecoration.BOLD)
                .append("```")
                .append("No registered talent found")
                .append("```")
                .send(channel);
            return;
        }

        List<String> talentNames = new ArrayList<>();
        List<String> discordIds = new ArrayList<>();

        for (UserMapping mapping : talentMappings) {
            talentNames.add(mapping.getTalentName());
            discordIds.add(Long.toString(mapping.getdUserId()));
        }

        Table.Builder builder = new Table.Builder(
            "Talent",
            talentNames.toArray(new String[0]),
            getStringFormatterWithWidth(talentNames));
        addStringColumnToTable(builder, "Discord ID", discordIds);

        Table table = builder.build();

        new MessageBuilder()
            .append("Talent\n", MessageDecoration.BOLD)
            .append("```")
            .append(table.toString())
            .append("```")
            .send(channel);
    }

    private static ColumnFormatter<String> getStringFormatterWithWidth(List<String> entries) {
        return ColumnFormatter.text(
            Alignment.CENTER,
            entries
                .stream()
                .max(Comparator.comparingInt(String::length))
                .get()
                .length());
    }

    private static void addStringColumnToTable(Table.Builder table, String header, List<String> rows) {
        table.addColumn(
            header,
            rows.toArray(new String[0]),
            getStringFormatterWithWidth(rows));
    }

}