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
package org.spongepowered.vanilla.mixin.world;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.interfaces.IMixinWorldType;

@Mixin(WorldProvider.class)
public abstract class MixinWorldProvider {
    @Shadow protected World worldObj;
    @Shadow public WorldType terrainType;
    @Shadow abstract boolean getHasNoSky();

    public BlockPos getRandomizedSpawnPoint() {
        BlockPos ret = this.worldObj.getSpawnPoint();

        boolean isAdventure = this.worldObj.getWorldInfo().getGameType() == WorldSettings.GameType.ADVENTURE;
        int spawnFuzz = ((IMixinWorldType) this.terrainType).getSpawnFuzz();
        int border = MathHelper.floor_double(this.worldObj.getWorldBorder().getClosestDistance(ret.getX(), ret.getZ()));
        if (border < spawnFuzz) {
            spawnFuzz = border;
        }
        if (spawnFuzz < 1) {
            spawnFuzz = 1;
        }
        int spawnFuzzHalf = spawnFuzz / 2;

        if (!getHasNoSky() && !isAdventure) {
            ret = this.worldObj.getTopSolidOrLiquidBlock(
                    ret.add(this.worldObj.rand.nextInt(spawnFuzzHalf) - spawnFuzz, 0, this.worldObj.rand.nextInt(spawnFuzzHalf) - spawnFuzz));
        }

        return ret;
    }

    public int getRespawnDimension(EntityPlayerMP player) {
        return 0;
    }
}
