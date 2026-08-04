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

package me.lucko.luckperms.fidorial.service;

import fr.fidorial.entity.Player;
import fr.fidorial.permission.PermissionHolder;
import fr.fidorial.permission.PermissionNode;
import fr.fidorial.permission.PermissionResolver;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.fidorial.LPFidorialPlugin;
import me.lucko.luckperms.fidorial.sender.PermissionCompat;
import net.kyori.adventure.util.TriState;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;

public class LuckPermsPermissionResolver implements PermissionResolver {
    private final LPFidorialPlugin plugin;

    public LuckPermsPermissionResolver(final LPFidorialPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TriState resolve(final PermissionHolder holder, final PermissionNode node) {
        if (!(holder instanceof final Player player)) {
            return TriState.NOT_SET;
        }

        final User user = this.plugin.getUserManager().getIfLoaded(player.uuid());
        if (user == null) {
            return TriState.NOT_SET;
        }

        final QueryOptions queryOptions = this.plugin.getContextManager().getQueryOptions(player);
        final Tristate result = user.getCachedData()
                .getPermissionData(queryOptions)
                .checkPermission(node.path(), CheckOrigin.PLATFORM_API_HAS_PERMISSION)
                .result();

        return PermissionCompat.toPlatform(result);
    }

    @Override
    public int weight() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean cacheable() {
        return false;
    }
}
