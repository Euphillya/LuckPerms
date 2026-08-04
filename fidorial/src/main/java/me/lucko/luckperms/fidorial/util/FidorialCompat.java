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

package me.lucko.luckperms.fidorial.util;

import fr.fidorial.entity.Player;
import fr.fidorial.event.player.PlayerLoginAttemptEvent;
import fr.fidorial.scheduler.RegionizedScheduler;
import fr.fidorial.world.ChunkPos;
import fr.fidorial.world.World;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.lang.reflect.Method;

/**
 * Converts between platform and native adventure objects.
 *
 * <p>We shade + relocate adventure in LuckPerms, because we use a slightly modified version.
 * Fidorial also exposes adventure throughout its API, and at a different major version, so the two
 * copies cannot be merged. This class bridges between "our" adventure objects and Fidorial's by
 * round-tripping through json.</p>
 *
 * <p>The same problem applies to any Fidorial method whose signature mentions an adventure type:
 * the descriptor would be rewritten by the relocator and no longer match at runtime. Those calls
 * are therefore made reflectively here rather than directly.</p>
 *
 * <p>{@code net.kyori.adventure.util.TriState} is the one deliberate exception - it is excluded
 * from relocation in the build script, because it appears in the signature of
 * {@code fr.fidorial.permission.PermissionResolver}, which we implement.</p>
 */
public final class FidorialCompat {
    private FidorialCompat() {}

    private static final Object PLATFORM_SERIALIZER_INSTANCE;
    private static final Method PLATFORM_SERIALIZER_DESERIALIZE;
    private static final Method PLATFORM_SEND_MESSAGE;
    private static final Method PLATFORM_PLAYER_KICK;
    private static final Method PLATFORM_LOGIN_ATTEMPT_REFUSE;
    private static final Method PLATFORM_WORLD_KEY;
    private static final Method PLATFORM_KEY_AS_STRING;
    private static final Method PLATFORM_SCHEDULER_EXECUTE;

    static {
        // built by concatenation so the relocator doesn't rewrite the literal
        String adventurePkg = "net.kyo".concat("ri.adventure.");
        try {
            Class<?> audienceClass = Class.forName(adventurePkg + "audience.Audience");
            Class<?> componentClass = Class.forName(adventurePkg + "text.Component");
            Class<?> keyClass = Class.forName(adventurePkg + "key.Key");
            Class<?> serializerClass = Class.forName(adventurePkg + "text.serializer.gson.GsonComponentSerializer");

            PLATFORM_SERIALIZER_INSTANCE = serializerClass.getMethod("gson").invoke(null);
            PLATFORM_SERIALIZER_DESERIALIZE = serializerClass.getMethod("deserialize", Object.class);
            PLATFORM_SEND_MESSAGE = audienceClass.getMethod("sendMessage", componentClass);
            PLATFORM_PLAYER_KICK = Player.class.getMethod("kick", componentClass);
            PLATFORM_LOGIN_ATTEMPT_REFUSE = PlayerLoginAttemptEvent.class.getMethod("refuse", componentClass);
            PLATFORM_WORLD_KEY = World.class.getMethod("key");
            PLATFORM_KEY_AS_STRING = keyClass.getMethod("asString");
            PLATFORM_SCHEDULER_EXECUTE = RegionizedScheduler.class.getMethod("execute", keyClass, ChunkPos.class, Runnable.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Converts one of our components into the platform's equivalent.
     *
     * @param component our component
     * @return the platform component
     */
    public static Object toPlatformComponent(Component component) {
        String json = GsonComponentSerializer.gson().serialize(component);
        try {
            return PLATFORM_SERIALIZER_DESERIALIZE.invoke(PLATFORM_SERIALIZER_INSTANCE, json);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a message to a Fidorial audience.
     *
     * @param audience the audience, e.g. a player
     * @param message the message
     */
    public static void sendMessage(Object audience, Component message) {
        try {
            PLATFORM_SEND_MESSAGE.invoke(audience, toPlatformComponent(message));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Disconnects a player.
     *
     * @param player the player
     * @param reason the kick reason
     */
    public static void kick(Player player, Component reason) {
        try {
            PLATFORM_PLAYER_KICK.invoke(player, toPlatformComponent(reason));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Refuses a connection, with a reason.
     *
     * @param event the login attempt event
     * @param reason the message shown to the client
     */
    public static void refuse(PlayerLoginAttemptEvent event, Component reason) {
        try {
            PLATFORM_LOGIN_ATTEMPT_REFUSE.invoke(event, toPlatformComponent(reason));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the string form of a world key, e.g. {@code minecraft:overworld}.
     *
     * @param world the world
     * @return the key, as a string
     */
    public static String worldKey(World world) {
        try {
            Object key = PLATFORM_WORLD_KEY.invoke(world);
            return (String) PLATFORM_KEY_AS_STRING.invoke(key);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Schedules a task on the region thread which owns the given chunk.
     *
     * @param scheduler the regionized scheduler
     * @param world the world owning the region
     * @param pos the chunk position
     * @param task the task to run
     */
    public static void executeOnRegion(RegionizedScheduler scheduler, World world, ChunkPos pos, Runnable task) {
        try {
            Object key = PLATFORM_WORLD_KEY.invoke(world);
            PLATFORM_SCHEDULER_EXECUTE.invoke(scheduler, key, pos, task);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
