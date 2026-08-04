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

import fr.fidorial.command.CommandSender;
import fr.fidorial.command.CommandSource;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.fidorial.LPFidorialPlugin;
import me.lucko.luckperms.fidorial.util.FidorialCompat;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

import java.util.UUID;

public class FidorialCommandSourceSender implements FidorialSender {
    private final LPFidorialPlugin plugin;
    private final CommandSource source;
    private final CommandSender sender;

    public FidorialCommandSourceSender(final LPFidorialPlugin plugin, final CommandSource source) {
        this.plugin = plugin;
        this.source = source;
        this.sender = source.sender();
    }

    @Override
    public UUID getUniqueId() {
        return Sender.CONSOLE_UUID;
    }

    @Override
    public String getName() {
        return Sender.CONSOLE_NAME;
    }

    @Override
    public void sendMessage(final Component message) {
        FidorialCompat.sendMessage(this.sender, TranslationManager.render(message));
    }

    @Override
    public Tristate getPermissionValue(final String node) {
        return PermissionCompat.query(this.sender, node);
    }

    @Override
    public boolean hasPermission(final String node) {
        return getPermissionValue(node).asBoolean();
    }

    @Override
    public void performCommand(final String command) {
        this.plugin.getServer().commands().dispatchAsync(this.source, command).join();
    }

    @Override
    public boolean isConsole() {
        return true;
    }
}
