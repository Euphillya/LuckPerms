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
import fr.fidorial.permission.PermissionResolver;
import fr.fidorial.plugin.PluginContext;
import fr.fidorial.service.ServicePriority;
import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.fidorial.calculator.FidorialCalculatorFactory;
import me.lucko.luckperms.fidorial.context.FidorialContextManager;
import me.lucko.luckperms.fidorial.context.FidorialPlayerCalculator;
import me.lucko.luckperms.fidorial.listeners.FidorialCommandListUpdater;
import me.lucko.luckperms.fidorial.listeners.FidorialConnectionListener;
import me.lucko.luckperms.fidorial.listeners.FidorialPlatformListener;
import me.lucko.luckperms.fidorial.sender.FidorialConsoleSender;
import me.lucko.luckperms.fidorial.sender.FidorialSender;
import me.lucko.luckperms.fidorial.service.LuckPermsPermissionResolver;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * LuckPerms implementation for Fidorial.
 */
public class LPFidorialPlugin extends AbstractLuckPermsPlugin {
    private final LPFidorialBootstrap bootstrap;

    private FidorialSenderFactory senderFactory;
    private FidorialConnectionListener connectionListener;
    private FidorialCommandExecutor commandManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private FidorialContextManager contextManager;

    private FidorialSender consoleSender;
    private LuckPermsPermissionResolver permissionResolver;
    private Object serviceOwner;

    public LPFidorialPlugin(final LPFidorialBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public LPFidorialBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public PluginContext getContext() {
        return this.bootstrap.getLoader();
    }

    public Server getServer() {
        return this.bootstrap.getServer();
    }

    public Object getServiceOwner() {
        if (this.serviceOwner == null) {
            this.serviceOwner = getServer().plugins().plugin("luckperms").map(Object.class::cast).orElse(this);
        }
        return this.serviceOwner;
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new FidorialSenderFactory(this);
        this.consoleSender = new FidorialConsoleSender(LoggerFactory.getLogger("LuckPerms"));
    }

    @Override
    protected Set<Dependency> getGlobalDependencies() {
        final Set<Dependency> dependencies = super.getGlobalDependencies();
        // required for loading the LP config
        dependencies.add(Dependency.CONFIGURATE_CORE);
        dependencies.add(Dependency.CONFIGURATE_YAML);
        dependencies.add(Dependency.SNAKEYAML);
        return dependencies;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new FidorialConfigAdapter(this, resolveConfig("config.yml"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new FidorialConnectionListener(this);
        this.connectionListener.register(getContext().events());
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new MessagingFactory<>(this);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new FidorialCommandExecutor(this);
        this.commandManager.register();
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new FidorialCalculatorFactory(this);
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new FidorialContextManager(this);

        final FidorialPlayerCalculator playerCalculator = new FidorialPlayerCalculator(this, getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS));
        this.contextManager.registerCalculator(playerCalculator);
    }

    @Override
    protected void setupPlatformHooks() {
        this.permissionResolver = new LuckPermsPermissionResolver(this);
        getServer().services().register(
                PermissionResolver.class,
                this.permissionResolver,
                getServiceOwner(),
                ServicePriority.HIGHEST
        );

        if (getConfiguration().get(ConfigKeys.CHAT_FORMATTER_ENABLED)) {
            getLogger().warn("The built-in LuckPerms chat formatter has been removed. Please delete the 'CHAT SETTINGS' " +
                    "section from your LuckPerms config.yml.");
        }

        final FidorialPlatformListener platformListener = new FidorialPlatformListener(this);
        platformListener.register(getContext().events());
        platformListener.insertKnownPermissions();
    }

    @Override
    protected void removePlatformHooks() {
        getServer().services().unregisterAll(getServiceOwner());
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(final LuckPermsApiProvider apiProvider) {
        return new FidorialEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(final LuckPerms api) {
        // expose the api to other Fidorial plugins via the service registry
        getServer().services().register(LuckPerms.class, api, getServiceOwner(), ServicePriority.HIGHEST);
    }

    @Override
    protected void performFinalSetup() {
        if (getConfiguration().get(ConfigKeys.UPDATE_CLIENT_COMMAND_LIST)) {
            getApiProvider().getEventBus().subscribe(new FidorialCommandListUpdater(this));
        }
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(final User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> this.contextManager.getQueryOptions(player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                getServer().onlinePlayers().stream().map(p -> getSenderFactory().wrap(this.senderFactory.player(p)))
        );
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(this.consoleSender);
    }

    public FidorialSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public FidorialCommandExecutor getCommandManager() {
        return this.commandManager;
    }

    @Override
    public StandardUserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public StandardGroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public StandardTrackManager getTrackManager() {
        return this.trackManager;
    }

    @Override
    public FidorialContextManager getContextManager() {
        return this.contextManager;
    }
}
