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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import fr.fidorial.command.CommandSource;
import fr.fidorial.command.Commands;
import fr.fidorial.entity.Player;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.fidorial.sender.FidorialCommandSourceSender;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FidorialCommandExecutor extends CommandManager {
    private static final String PRIMARY_ALIAS = "luckperms";
    private static final Set<String> ALIASES = Set.of("lp", "perm", "perms", "permission", "permissions");

    private static final String ARGUMENTS = "arguments";

    private final LPFidorialPlugin plugin;

    public FidorialCommandExecutor(final LPFidorialPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register() {
        final LiteralCommandNode<CommandSource> node = Commands.literal(PRIMARY_ALIAS)
                .requires(source -> hasPermissionForAny(wrap(source)))
                .executes(context -> execute(context.getSource(), ""))
                .then(Commands.argument(ARGUMENTS, StringArgumentType.greedyString())
                        .suggests(this::suggest)
                        .executes(context -> execute(context.getSource(), StringArgumentType.getString(context, ARGUMENTS))))
                .build();

        this.plugin.getServer().commands().register(node, ALIASES);
    }

    public void unregister() {
        this.plugin.getServer().commands().unregister(PRIMARY_ALIAS);
        for (final String alias : ALIASES) {
            this.plugin.getServer().commands().unregister(alias);
        }
    }

    private int execute(final CommandSource source, final String arguments) {
        final Sender sender = wrap(source);
        final List<String> args = ArgumentTokenizer.EXECUTE.tokenizeInput(arguments);
        executeCommand(sender, "lp", args);
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggest(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
        final Sender sender = wrap(context.getSource());
        final String remaining = builder.getRemaining();

        final List<String> args = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(remaining);
        final List<String> completions = tabCompleteCommand(sender, args);

        // brigadier replaces from the offset we give it, so line it up with the token being typed
        final SuggestionsBuilder offset = builder.createOffset(builder.getStart() + remaining.lastIndexOf(' ') + 1);
        for (final String completion : completions) {
            offset.suggest(completion);
        }
        return offset.buildFuture();
    }

    /**
     * Wraps a platform command source in a LuckPerms sender.
     *
     * @param source the source
     * @return the sender
     */
    private Sender wrap(final CommandSource source) {
        if (source.sender() instanceof final Player player) {
            return this.plugin.getSenderFactory().wrap(this.plugin.getSenderFactory().player(player));
        }
        return this.plugin.getSenderFactory().wrap(new FidorialCommandSourceSender(this.plugin, source));
    }
}
