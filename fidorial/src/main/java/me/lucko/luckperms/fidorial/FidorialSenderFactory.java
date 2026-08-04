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

package me.lucko.luckperms.fidorial;

import fr.fidorial.entity.Player;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.fidorial.sender.FidorialPlayerSender;
import me.lucko.luckperms.fidorial.sender.FidorialSender;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

import java.util.UUID;

public class FidorialSenderFactory extends SenderFactory<LPFidorialPlugin, FidorialSender> {

    public FidorialSenderFactory(LPFidorialPlugin plugin) {
        super(plugin);
    }

    /**
     * Wraps a platform player in the sender abstraction.
     *
     * @param player the player
     * @return the sender
     */
    public FidorialSender player(Player player) {
        return new FidorialPlayerSender(getPlugin(), player);
    }

    @Override
    protected String getName(FidorialSender sender) {
        return sender.getName();
    }

    @Override
    protected UUID getUniqueId(FidorialSender sender) {
        return sender.getUniqueId();
    }

    @Override
    protected void sendMessage(FidorialSender sender, Component message) {
        sender.sendMessage(message);
    }

    @Override
    protected Tristate getPermissionValue(FidorialSender sender, String node) {
        return sender.getPermissionValue(node);
    }

    @Override
    protected boolean hasPermission(FidorialSender sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    protected void performCommand(FidorialSender sender, String command) {
        sender.performCommand(command);
    }

    @Override
    protected boolean isConsole(FidorialSender sender) {
        return sender.isConsole();
    }
}
