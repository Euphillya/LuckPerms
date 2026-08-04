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

package me.lucko.luckperms.fidorial.loader;

import fr.fidorial.plugin.Plugin;
import fr.fidorial.plugin.PluginContext;
import me.lucko.luckperms.common.loader.JarInJarClassLoader;
import me.lucko.luckperms.common.loader.LoaderBootstrap;

/**
 * The plugin entrypoint declared in {@code fidorial.json}.
 *
 * <p>Fidorial gives each plugin its own {@link java.net.URLClassLoader}, so we can nest the real
 * plugin (and its relocated dependencies) inside this jar and load it from an isolated child
 * classloader, exactly as we do on the other platforms.</p>
 */
public class FidorialLoaderPlugin implements Plugin {
    private static final String JAR_NAME = "luckperms-fidorial.jarinjar";
    private static final String BOOTSTRAP_CLASS = "me.lucko.luckperms.fidorial.LPFidorialBootstrap";

    private JarInJarClassLoader loader;
    private LoaderBootstrap plugin;

    @Override
    public void onLoad(final PluginContext context) {
        this.loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME);
        this.plugin = this.loader.instantiatePlugin(BOOTSTRAP_CLASS, PluginContext.class, context);
        this.plugin.onLoad();
    }

    @Override
    public void onEnable() {
        this.plugin.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            this.plugin.onDisable();
        } finally {
            try {
                this.loader.close();
            } catch (final Exception e) {
                // ignore
            }
        }
    }
}
