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

package me.lucko.luckperms.fidorial.sender;

import fr.fidorial.entity.Player;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.fidorial.LPFidorialPlugin;
import me.lucko.luckperms.fidorial.util.FidorialCompat;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

import java.util.Objects;
import java.util.UUID;

public class FidorialPlayerSender implements FidorialSender {
    private final LPFidorialPlugin plugin;
    private final Player player;

    public FidorialPlayerSender(final LPFidorialPlugin plugin, final Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    @Override
    public UUID getUniqueId() {
        return this.player.uuid();
    }

    @Override
    public String getName() {
        return this.player.name();
    }

    @Override
    public void sendMessage(final Component message) {
        FidorialCompat.sendMessage(this.player, TranslationManager.render(message));
    }

    @Override
    public Tristate getPermissionValue(final String node) {
        return PermissionCompat.query(this.player, node);
    }

    @Override
    public boolean hasPermission(final String node) {
        return getPermissionValue(node).asBoolean();
    }

    @Override
    public void performCommand(final String command) {
        this.plugin.getServer().commands().dispatchAsync(this.player, command).join();
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof final FidorialPlayerSender other && this.player.uuid().equals(other.player.uuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.player.uuid());
    }
}
