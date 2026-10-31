package com.breakinblocks.neosync.api.shell;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An object that allows registering custom {@link ShellStateComponent}.
 */
public final class ShellStateComponentFactoryRegistry {
    private static final ShellStateComponentFactoryRegistry INSTANCE = new ShellStateComponentFactoryRegistry();

    private final Set<ShellStateComponentFactory> factories = new CopyOnWriteArraySet<>();

    private ShellStateComponentFactoryRegistry() {
    }

    /**
     * @return The public-facing {@link ShellStateComponentFactoryRegistry} instance.
     */
    public static ShellStateComponentFactoryRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a new factory.
     * @param factory The factory.
     * @return The registered factory.
     */
    public ShellStateComponentFactory register(ShellStateComponentFactory factory) {
        this.factories.add(factory);
        return factory;
    }

    /**
     * @return All the registered factories.
     */
    public Collection<ShellStateComponentFactory> getValues() {
        return Collections.unmodifiableSet(this.factories);
    }

    /**
     * Registers a new factory.
     * @param factory The factory.
     * @return The registered factory.
     */
    public ShellStateComponentFactory register(Function<@Nullable ServerPlayer, ShellStateComponent> factory) {
        return register(new ShellStateComponentFactory() {
            @Override
            public ShellStateComponent empty() {
                return factory.apply(null);
            }

            @Override
            public ShellStateComponent of(ServerPlayer player) {
                return factory.apply(player);
            }
        });
    }

    /**
     * Registers a new factory.
     * @param emptyFactory Creates a new instance of {@linkplain ShellStateComponent} that has no player data.
     * @param factory Creates a new instance of {@linkplain ShellStateComponent} that is synced with the player's state.
     * @return The registered factory.
     */
    public ShellStateComponentFactory register(Supplier<ShellStateComponent> emptyFactory, Function<ServerPlayer, ShellStateComponent> factory) {
        return register(new ShellStateComponentFactory() {
            @Override
            public ShellStateComponent empty() {
                return emptyFactory.get();
            }

            @Override
            public ShellStateComponent of(ServerPlayer player) {
                return factory.apply(player);
            }
        });
    }

    /**
     * {@link ShellStateComponent}'s factory.
     */
    public interface ShellStateComponentFactory {
        /**
         * Creates a new instance of {@link ShellStateComponent} that has no player data.
         * @return The {@linkplain ShellStateComponent}.
         */
        ShellStateComponent empty();

        /**
         * Creates a new instance of {@link ShellStateComponent} that is synced with the player's state.
         * @param player The player.
         * @return The {@linkplain ShellStateComponent}.
         */
        ShellStateComponent of(ServerPlayer player);
    }
}
