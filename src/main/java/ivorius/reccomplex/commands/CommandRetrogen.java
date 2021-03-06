/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands;

import ivorius.ivtoolkit.util.IvStreams;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.files.RCFiles;
import ivorius.reccomplex.files.loading.FileSuffixFilter;
import ivorius.reccomplex.utils.ServerTranslations;
import ivorius.reccomplex.world.gen.feature.WorldGenStructures;
import ivorius.reccomplex.world.gen.feature.structure.StructureInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.RegionFileCache;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by lukas on 25.05.14.
 */
public class CommandRetrogen extends CommandBase
{
    public static Stream<Pair<Integer, Integer>> existingRegions(File worldDir)
    {
        File regionsDirectory = RCFiles.getValidatedFolder(new File(worldDir, "region"), false);
        if (regionsDirectory == null) return Stream.empty();

        String[] mcas = regionsDirectory.list(new FileSuffixFilter("mca"));
        if (mcas == null) throw new IllegalStateException();

        return Arrays.stream(mcas).map(s -> s.split("\\."))
                .filter(p -> p.length == 4 && p[0].equals("r")) // Is region file
                .map(p -> Pair.of(Integer.parseInt(p[1]), Integer.parseInt(p[2])))
                .filter(rfc -> rfc.getLeft() != null && rfc.getRight() != null); // Has coords
    }

    public static Stream<ChunkPos> existingChunks(World world)
    {
        // Each region is 32x32 chunks
        File worldDirectory = world.getSaveHandler().getWorldDirectory();
        return existingRegions(worldDirectory)
                .map(rfc -> new ChunkPos(rfc.getLeft() << 5, rfc.getRight() << 5))
                .map(rflc -> Pair.of(RegionFileCache.createOrLoadRegionFile(worldDirectory, rflc.chunkXPos, rflc.chunkZPos), rflc))
                .flatMap(r -> IvStreams.flatMapToObj(IntStream.range(0, 32), x -> IntStream.range(0, 32).mapToObj(z -> Pair.of(r.getLeft(), add(r.getRight(), x, z)))))
                .filter(p -> p.getLeft().chunkExists(p.getRight().chunkXPos & 31, p.getRight().chunkZPos & 31)) // Region has chunk
                .map(Pair::getRight);
    }

    @Nonnull
    protected static ChunkPos add(ChunkPos pos, int x, int z)
    {
        return new ChunkPos(x + pos.chunkXPos, z + pos.chunkZPos);
    }

    public static Random getRandom(WorldServer world, ChunkPos pos)
    {
        return world.setRandomSeed(pos.chunkXPos, pos.chunkZPos, 0xDEADBEEF);
    }

    public static long retrogen(WorldServer world, Predicate<StructureInfo> structurePredicate)
    {
        return existingChunks(world)
                .filter(pos -> world.getChunkFromChunkCoords(pos.chunkXPos, pos.chunkZPos).isTerrainPopulated())
                .filter(pos -> WorldGenStructures.decorate(world, getRandom(world, pos), pos, structurePredicate))
                .count();
    }

    @Override
    public String getName()
    {
        return RCConfig.commandPrefix + "retro";
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender var1)
    {
        return ServerTranslations.usage("commands.rcretro.usage");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException
    {
        long count = retrogen(RCCommands.tryParseDimension(commandSender, args, 0), RCCommands.tryParseStructurePredicate(args, 4, () -> null));

        commandSender.sendMessage(ServerTranslations.format("commands.rcretro.count", String.valueOf(count)));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        if (args.length == 1)
            return RCCommands.completeDimension(args);
        else if (args.length >= 2)
            return RCCommands.completeResourceMatcher(args);

        return Collections.emptyList();
    }

}
