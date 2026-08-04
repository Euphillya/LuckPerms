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

import fr.fidorial.permission.PermissionHolder;
import fr.fidorial.permission.PermissionNode;
import net.kyori.adventure.util.TriState;
import net.luckperms.api.util.Tristate;

public final class PermissionCompat {
    private PermissionCompat() {}

    /**
     * Asks the platform to resolve a permission for a holder.
     *
     * @param holder the holder
     * @param node the permission
     * @return the resolved state
     */
    public static Tristate query(final PermissionHolder holder, final String node) {
        final PermissionNode parsed = parse(node);
        if (parsed == null) {
            return Tristate.UNDEFINED;
        }
        return fromPlatform(holder.permissionState(parsed));
    }

    /**
     * Parses a permission node, returning null rather than throwing when the string cannot be
     * represented as a {@link PermissionNode}.
     *
     * @param node the permission string
     * @return the node, or null
     */
    public static PermissionNode parse(final String node) {
        try {
            return PermissionNode.of(node);
        } catch (final IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public static Tristate fromPlatform(final TriState state) {
        return switch (state) {
            case TRUE -> Tristate.TRUE;
            case FALSE -> Tristate.FALSE;
            case NOT_SET -> Tristate.UNDEFINED;
        };
    }

    public static TriState toPlatform(final Tristate state) {
        return switch (state) {
            case TRUE -> TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case UNDEFINED -> TriState.NOT_SET;
        };
    }
}
