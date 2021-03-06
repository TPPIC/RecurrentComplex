/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.gen.feature.structure;

import ivorius.reccomplex.utils.RCStructureBoundingBoxes;
import ivorius.reccomplex.world.gen.feature.structure.generic.gentypes.GenerationInfo;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by lukas on 08.09.16.
 */
public class Environment
{
    @Nonnull
    public final WorldServer world;
    @Nonnull
    public final Biome biome;
    @Nullable
    public final Integer villageType;
    @Nullable
    public final GenerationInfo generationInfo;

    public Environment(@Nonnull WorldServer world, @Nonnull Biome biome, @Nullable Integer villageType, @Nullable GenerationInfo generationInfo)
    {
        this.world = world;
        this.biome = biome;
        this.villageType = villageType;
        this.generationInfo = generationInfo;
    }

    @Nonnull
    public static Environment inNature(@Nonnull WorldServer world, @Nonnull StructureBoundingBox boundingBox, GenerationInfo generationInfo)
    {
        return new Environment(world, getBiome(world, boundingBox), null, generationInfo);
    }

    @Nonnull
    public static Environment inNature(@Nonnull WorldServer world, @Nonnull StructureBoundingBox boundingBox)
    {
        return inNature(world, boundingBox, null);
    }

    public static Biome getBiome(World world, StructureBoundingBox boundingBox)
    {
        return world.getBiome(RCStructureBoundingBoxes.getCenter(boundingBox));
    }

    public Environment withGeneration(GenerationInfo generation)
    {
        return new Environment(world, biome, villageType, generationInfo);
    }
}
