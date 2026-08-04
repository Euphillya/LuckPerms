/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.fidorial.listeners;

import fr.fidorial.entity.Player;
import fr.fidorial.entity.PlayerProfile;
import fr.fidorial.event.EventBus;
import fr.fidorial.event.EventPriority;
import fr.fidorial.event.player.PlayerLoginAttemptEvent;
import fr.fidorial.event.player.PlayerJoinEvent;
import fr.fidorial.event.player.PlayerQuitEvent;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.fidorial.LPFidorialPlugin;
import me.lucko.luckperms.fidorial.util.FidorialCompat;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Loads and unloads permissions data as players come and go.
 */
public class FidorialConnectionListener extends AbstractConnectionListener {
    private final LPFidorialPlugin plugin;

    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    public FidorialConnectionListener(final LPFidorialPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register(final EventBus events) {
        events.subscribe(PlayerLoginAttemptEvent.class, EventPriority.LOW, this::onLoginAttempt);
        events.subscribe(PlayerLoginAttemptEvent.class, EventPriority.MONITOR, this::onLoginAttemptMonitor);
        events.subscribe(PlayerJoinEvent.class, EventPriority.NORMAL, this::onPlayerJoin);
        events.subscribe(PlayerQuitEvent.class, EventPriority.MONITOR, this::onPlayerQuit);
    }

    private void onLoginAttempt(final PlayerLoginAttemptEvent e) {

        final PlayerProfile profile = e.profile();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login attempt for " + profile.uuid() + " - " + profile.name());
        }

        if (e.isCancelled()) {
            this.plugin.getLogger().info("Another plugin has refused the connection for " + profile.uuid() + " - " +
                    profile.name() + ". No permissions data will be loaded.");
            this.deniedLogin.add(profile.uuid());
            return;
        }
        try {
            final User user = loadUser(profile.uuid(), profile.name());
            recordConnection(profile.uuid());
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(profile.uuid(), profile.name(), user);
        } catch (final Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " +
                    profile.uuid() + " - " + profile.name(), ex);

            this.deniedLogin.add(profile.uuid());
            FidorialCompat.refuse(e, TranslationManager.render(Message.LOADING_DATABASE_ERROR.build()));
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(profile.uuid(), profile.name(), null);
        }
    }

    private void onLoginAttemptMonitor(final PlayerLoginAttemptEvent e) {
        final UUID uniqueId = e.profile().uuid();
        if (this.deniedLogin.remove(uniqueId) && !e.isCancelled()) {
            this.plugin.getLogger().severe("Player connection was re-allowed for " + uniqueId);
            e.setCancelled(true);
        }
    }

    private void onPlayerJoin(final PlayerJoinEvent e) {
        final Player player = e.player();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing join for " + player.uuid() + " - " + player.name());
        }

        final User user = this.plugin.getUserManager().getIfLoaded(player.uuid());
        if (user != null) {
            // drop anything the platform resolved before we were consulted
            player.invalidatePermissions();
            return;
        }

        if (!getUniqueConnections().contains(player.uuid())) {
            this.plugin.getLogger().warn("User " + player.uuid() + " - " + player.name() +
                    " doesn't have data pre-loaded, they have never been processed during a login attempt in this session." +
                    " - denying login.");
        } else {
            this.plugin.getLogger().warn("User " + player.uuid() + " - " + player.name() +
                    " doesn't currently have data pre-loaded, but they have been processed before in this session." +
                    " - denying login.");
        }

        final Component reason = TranslationManager.render(Message.LOADING_STATE_ERROR.build());
        FidorialCompat.kick(player, reason);
    }

    private void onPlayerQuit(final PlayerQuitEvent e) {
        handleDisconnect(e.player().uuid());
    }
}
