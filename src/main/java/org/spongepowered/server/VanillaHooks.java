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
package org.spongepowered.server;

import net.minecraft.item.ItemStack;
import org.spongepowered.api.data.manipulator.DataManipulator;

import java.util.Collection;

public final class VanillaHooks {

    private VanillaHooks() {
    }

    // This method may be un-needed now
    private static void copyStack(ItemStack source, ItemStack target) {
        target.stackSize = source.stackSize;
        // TODO May need to clear manipulators from source stack sans durability
        final Collection<DataManipulator<?, ?>> manipulators = ((org.spongepowered.api.item.inventory.ItemStack) source).getContainers();
        for (DataManipulator<?, ?> manipulator : manipulators) {
            ((org.spongepowered.api.item.inventory.ItemStack) target).offer((DataManipulator) manipulator);
        }
    }
}
