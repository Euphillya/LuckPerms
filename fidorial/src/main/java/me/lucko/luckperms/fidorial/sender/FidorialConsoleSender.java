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

import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.util.Tristate;
import org.slf4j.Logger;

import java.util.UUID;

public class FidorialConsoleSender implements FidorialSender {
    private final Logger logger;

    public FidorialConsoleSender(final Logger logger) {
        this.logger = logger;
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
        final Component rendered = TranslationManager.render(message);
        this.logger.info(PlainTextComponentSerializer.plainText().serialize(rendered));
    }

    @Override
    public Tristate getPermissionValue(final String node) {
        return Tristate.TRUE;
    }

    @Override
    public boolean hasPermission(final String node) {
        return true;
    }

    @Override
    public void performCommand(final String command) {
        this.logger.warn("Unable to execute '{}': Fidorial does not expose a console command source to plugins.", command);
    }

    @Override
    public boolean isConsole() {
        return true;
    }
}
