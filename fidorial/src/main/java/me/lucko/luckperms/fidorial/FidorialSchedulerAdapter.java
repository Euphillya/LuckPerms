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
import me.lucko.luckperms.common.plugin.scheduler.JavaSchedulerAdapter;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.sender.AbstractSender;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.fidorial.sender.FidorialPlayerSender;
import me.lucko.luckperms.fidorial.sender.FidorialSender;
import me.lucko.luckperms.fidorial.util.FidorialCompat;

public class FidorialSchedulerAdapter extends JavaSchedulerAdapter implements SchedulerAdapter {
    private final LPFidorialBootstrap bootstrap;

    public FidorialSchedulerAdapter(final LPFidorialBootstrap bootstrap) {
        super(bootstrap);
        this.bootstrap = bootstrap;
    }

    @Override
    public void executeSync(final Sender ctx, final Runnable task) {
        final FidorialSender sender = unwrapSender(ctx);
        if (sender instanceof final FidorialPlayerSender playerSender) {
            final Player player = playerSender.getPlayer();
            try {
                FidorialCompat.executeOnRegion(
                        this.bootstrap.getServer().scheduler(),
                        player.world(),
                        player.location().chunk(),
                        task
                );
                return;
            } catch (final RuntimeException e) {
                this.bootstrap.getPluginLogger().warn("Unable to schedule a task on the region owning " + player.name(), e);
            }
        }
        executeAsync(task);
    }

    @SuppressWarnings("unchecked")
    private static FidorialSender unwrapSender(final Sender sender) {
        if (sender instanceof AbstractSender) {
            return ((AbstractSender<FidorialSender>) sender).getSender();
        } else {
            throw new IllegalArgumentException("unknown sender type: " + sender.getClass());
        }
    }
}
