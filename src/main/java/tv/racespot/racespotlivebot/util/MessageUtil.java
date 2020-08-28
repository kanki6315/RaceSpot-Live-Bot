/**
 * Copyright (C) 2020 by Amobee Inc.
 * All Rights Reserved.
 */
package tv.racespot.racespotlivebot.util;

import java.util.Collection;
import java.util.List;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class MessageUtil {

    public static boolean hasAdminPermission(Server server, User user) {

        List<Role> roles = user.getRoles(server);
        return roles.stream()
            .map(Role::getAllowedPermissions)
            .flatMap(Collection::stream)
            .anyMatch(role -> role.equals(PermissionType.ADMINISTRATOR));
    }

    public static void notifyChecked(Message message) {
        message.addReaction("👍");
    }

    public static void notifyFailed(Message message) {
        message.addReaction("👎");
    }

    public static void notifyUnallowed(Message message) {
        message.addReaction("❌");
    }
}