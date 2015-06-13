/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.vanilla;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.inject.Guice;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.state.ConstructionEvent;
import org.spongepowered.api.event.state.InitializationEvent;
import org.spongepowered.api.event.state.LoadCompleteEvent;
import org.spongepowered.api.event.state.PostInitializationEvent;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.event.state.ServerAboutToStartEvent;
import org.spongepowered.api.event.state.ServerStartingEvent;
import org.spongepowered.api.event.state.ServerStoppedEvent;
import org.spongepowered.api.event.state.StateEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.ProviderExistsException;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.persistence.SerializationService;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.SpongeBootstrap;
import org.spongepowered.common.interfaces.IMixinServerCommandManager;
import org.spongepowered.common.service.permission.SpongeContextCalculator;
import org.spongepowered.common.service.permission.SpongePermissionService;
import org.spongepowered.common.service.persistence.SpongeSerializationService;
import org.spongepowered.common.service.sql.SqlServiceImpl;
import org.spongepowered.common.util.SpongeHooks;
import org.spongepowered.vanilla.guice.VanillaGuiceModule;
import org.spongepowered.vanilla.plugin.VanillaPluginManager;

import java.io.File;
import java.io.IOException;

public final class SpongeVanilla implements PluginContainer {

    public static final SpongeVanilla INSTANCE = new SpongeVanilla();

    private final Game game;

    private SpongeVanilla() {
        Guice.createInjector(new VanillaGuiceModule(this, LogManager.getLogger("Sponge"))).getInstance(Sponge.class);

        this.game = Sponge.getGame();
    }

    public static void main(String[] args) {
        MinecraftServer.main(args);
    }

    public void preInitialize() {
        try {
            Sponge.getLogger().info("Loading Sponge...");

            File gameDir = Sponge.getGameDirectory();
            File pluginsDir = Sponge.getPluginsDirectory();

            if (!gameDir.isDirectory() || !pluginsDir.isDirectory()) {
                if (!pluginsDir.mkdirs()) {
                    throw new IOException("Failed to create plugins folder");
                }
            }

            SpongeBootstrap.initializeServices();
            SpongeBootstrap.preInitializeRegistry();

            this.game.getEventManager().register(this, this);
            this.game.getEventManager().register(this, this.game.getRegistry());

            Sponge.getLogger().info("Loading plugins...");
            ((VanillaPluginManager) this.game.getPluginManager()).loadPlugins();
            postState(ConstructionEvent.class);
            Sponge.getLogger().info("Initializing plugins...");
            postState(PreInitializationEvent.class);

            this.game.getServiceManager().potentiallyProvide(PermissionService.class).executeWhenPresent(new Predicate<PermissionService>() {

                @Override
                public boolean apply(PermissionService input) {
                    input.registerContextCalculator(new SpongeContextCalculator());
                    return true;
                }
            });

            SpongeHooks.enableThreadContentionMonitoring();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void initialize() {
        SpongeBootstrap.initializeRegistry();
        postState(InitializationEvent.class);

        if (!this.game.getServiceManager().provide(PermissionService.class).isPresent()) {
            try {
                SpongePermissionService service = new SpongePermissionService();
                // Setup default permissions
                service.getGroupForOpLevel(2).getSubjectData().setPermission(SubjectData.GLOBAL_CONTEXT, "minecraft.commandblock", Tristate.TRUE);
                this.game.getServiceManager().setProvider(this, PermissionService.class, service);
            } catch (ProviderExistsException e) {
                // It's a fallback, ignore
            }
        }

        SpongeBootstrap.postIniitalizeRegistry();
        SerializationService service = this.game.getServiceManager().provide(SerializationService.class).get();
        ((SpongeSerializationService) service).completeRegistration();

        postState(PostInitializationEvent.class);

        Sponge.getLogger().info("Successfully loaded and initialized plugins.");

        postState(LoadCompleteEvent.class);
    }

    @Subscribe(order = Order.PRE)
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        ((IMixinServerCommandManager) MinecraftServer.getServer().getCommandManager()).registerEarlyCommands(this.game);
    }

    @Subscribe(order = Order.PRE)
    public void onServerStarted(ServerStartingEvent event) {
        ((IMixinServerCommandManager) MinecraftServer.getServer().getCommandManager()).registerLowPriorityCommands(this.game);
    }

    @Subscribe(order = Order.PRE)
    public void onServerStopped(ServerStoppedEvent event) throws IOException {
        ((SqlServiceImpl) this.game.getServiceManager().provideUnchecked(SqlService.class)).close();
    }

    @Override
    public String getId() {
        return "sponge";
    }

    @Override
    public String getName() {
        return "Sponge";
    }

    @Override
    public String getVersion() {
        return this.game.getPlatform().getVersion();
    }

    @Override
    public Object getInstance() {
        return this;
    }

    public void postState(Class<? extends StateEvent> type) {
        this.game.getEventManager().post(SpongeEventFactory.createState(type, this.game));
    }
}
