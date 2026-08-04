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

package me.lucko.luckperms.fidorial.context;

import fr.fidorial.entity.GameMode;
import fr.fidorial.entity.Player;
import fr.fidorial.world.World;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.fidorial.LPFidorialPlugin;
import me.lucko.luckperms.fidorial.util.FidorialCompat;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.Set;

public class FidorialPlayerCalculator implements ContextCalculator<Player> {
    private final LPFidorialPlugin plugin;

    private final boolean gamemode;
    private final boolean world;

    public FidorialPlayerCalculator(final LPFidorialPlugin plugin, final Set<String> disabled) {
        this.plugin = plugin;
        this.gamemode = !disabled.contains(DefaultContextKeys.GAMEMODE_KEY);
        this.world = !disabled.contains(DefaultContextKeys.WORLD_KEY);
    }

    @Override
    public void calculate(@NonNull final Player subject, @NonNull final ContextConsumer consumer) {
        if (this.gamemode) {
            consumer.accept(DefaultContextKeys.GAMEMODE_KEY, getGamemodeName(subject.gameMode()));
        }

        if (this.world) {
            final String worldName = FidorialCompat.worldKey(subject.world());
            this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).rewriteAndSubmit(worldName, consumer);
        }
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        final ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        if (this.gamemode) {
            for (final GameMode mode : GameMode.values()) {
                builder.add(DefaultContextKeys.GAMEMODE_KEY, getGamemodeName(mode));
            }
        }
        if (this.world) {
            for (final World world : this.plugin.getServer().worlds()) {
                final String worldName = FidorialCompat.worldKey(world);
                if (Context.isValidValue(worldName)) {
                    builder.add(DefaultContextKeys.WORLD_KEY, worldName);
                }
            }
        }
        return builder.build();
    }

    private static String getGamemodeName(final GameMode mode) {
        return mode.name().toLowerCase(Locale.ROOT);
    }
}
