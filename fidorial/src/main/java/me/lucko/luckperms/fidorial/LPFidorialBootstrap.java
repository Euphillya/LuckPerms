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

import fr.fidorial.Server;
import fr.fidorial.entity.OfflinePlayer;
import fr.fidorial.entity.Player;
import fr.fidorial.plugin.PluginContext;
import me.lucko.luckperms.common.loader.LoaderBootstrap;
import me.lucko.luckperms.common.plugin.bootstrap.BootstrappedWithLoader;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.classpath.JarInJarClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import net.luckperms.api.platform.Platform;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Bootstrap plugin for LuckPerms running on Fidorial.
 */
public class LPFidorialBootstrap implements LuckPermsBootstrap, LoaderBootstrap, BootstrappedWithLoader {

    /**
     * The context handed to us by the platform's plugin loader
     */
    private final PluginContext context;

    /**
     * The plugin logger
     */
    private final PluginLogger logger;

    /**
     * A scheduler adapter for the platform
     */
    private final FidorialSchedulerAdapter schedulerAdapter;

    /**
     * The plugin class path appender
     */
    private final JarInJarClassPathAppender classPathAppender;

    /**
     * The plugin instance
     */
    private final LPFidorialPlugin plugin;

    /**
     * The time when the plugin was enabled
     */
    private Instant startTime;

    // load/enable latches
    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    private static final String PLUGIN_ID = "luckperms";
    private Object loaderPlugin;

    public LPFidorialBootstrap(final PluginContext context) {
        this.context = context;

        this.logger = new FidorialPluginLogger(LoggerFactory.getLogger("LuckPerms"));
        this.schedulerAdapter = new FidorialSchedulerAdapter(this);
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.plugin = new LPFidorialPlugin(this);
    }

    // provide adapters

    @Override
    public Object getLoader() {
        if (this.loaderPlugin == null) {
            this.loaderPlugin = this.context.server().plugins()
                    .plugin(PLUGIN_ID)
                    .map(Object.class::cast)
                    .orElse(this);
        }
        return this.loaderPlugin;
    }

    public PluginContext getContext() {
        return this.context;
    }

    public Server getServer() {
        return this.context.server();
    }

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public FidorialSchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    @Override
    public InputStream getResourceStream(final String path) {
        // avoid picking up resources belonging to other plugins
        final URL url = this.classPathAppender.getClassLoader().findResource(path);
        try {
            return url != null ? url.openStream() : null;
        } catch (final IOException e) {
            return null;
        }
    }

    // lifecycle

    @Override
    public void onLoad() {
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }
    }

    @Override
    public void onEnable() {
        this.startTime = Instant.now();
        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Override
    public void onDisable() {
        this.plugin.disable();
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    // provide information about the plugin

    @Override
    public String getVersion() {
        return this.context.meta().version();
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    // provide information about the platform

    @Override
    public Platform.Type getType() {
        return Platform.Type.FIDORIAL;
    }

    @Override
    public String getServerBrand() {
        return getServer().getName();
    }

    @Override
    public String getServerVersion() {
        final Server server = getServer();
        return server.minecraftVersion() + " (protocol " + server.protocolVersion() + ")";
    }

    @Override
    public Path getDataDirectory() {
        return this.context.dataFolder().toAbsolutePath();
    }

    @Override
    public Optional<Player> getPlayer(final UUID uniqueId) {
        return getServer().player(uniqueId).map(p -> (Player) p);
    }

    @Override
    public Optional<UUID> lookupUniqueId(final String username) {
        return getServer().offlinePlayers().cached(username).map(OfflinePlayer::uuid);
    }

    @Override
    public Optional<String> lookupUsername(final UUID uniqueId) {
        return getServer().offlinePlayers().cached(uniqueId).flatMap(OfflinePlayer::name);
    }

    @Override
    public int getPlayerCount() {
        return getServer().playerCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        final Collection<? extends Player> players = getServer().onlinePlayers();
        final List<String> list = new ArrayList<>(players.size());
        for (final Player player : players) {
            list.add(player.name());
        }
        return list;
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        final Collection<? extends Player> players = getServer().onlinePlayers();
        final List<UUID> list = new ArrayList<>(players.size());
        for (final Player player : players) {
            list.add(player.uuid());
        }
        return list;
    }

    @Override
    public boolean isPlayerOnline(final UUID uniqueId) {
        return getServer().player(uniqueId).isPresent();
    }

    @Override
    public @Nullable String identifyClassLoader(final ClassLoader classLoader) {
        // JavaPluginManager names its loaders "fidorial-plugin:<file name>"
        if (classLoader instanceof URLClassLoader) {
            final String name = classLoader.getName();
            if (name != null && name.startsWith("fidorial-plugin:")) {
                return name.substring("fidorial-plugin:".length());
            }
        }
        return null;
    }
}
