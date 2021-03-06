/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.commands;

import ivorius.ivtoolkit.blocks.BlockSurfacePos;
import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.capability.SelectionOwner;
import ivorius.reccomplex.capability.StructureEntityInfo;
import ivorius.reccomplex.utils.ServerTranslations;
import ivorius.reccomplex.utils.algebra.ExpressionCache;
import ivorius.reccomplex.utils.expression.ResourceMatcher;
import ivorius.reccomplex.world.gen.feature.structure.StructureInfo;
import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import net.minecraft.command.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by lukas on 18.01.15.
 */
public class RCCommands
{
    @Nullable
    public static ICommand confirm;
    @Nullable
    public static ICommand cancel;

    public static ICommand lookup;
    public static ICommand list;

    public static ICommand reopen;

    public static ICommand forget;

    public static void onServerStart(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandWrite());
        event.registerServerCommand(new CommandDelete());

        if (RCConfig.asCommandPermissionLevel >= 0)
            event.registerServerCommand(new CommandAs());
        event.registerServerCommand(new CommandSelecting());

        if (!RecurrentComplex.isLite())
        {
            event.registerServerCommand(new CommandExportStructure());
            event.registerServerCommand(new CommandEditStructure());
        }
        event.registerServerCommand(new CommandGenerateStructure());
        event.registerServerCommand(new CommandImportStructure());

        event.registerServerCommand(new CommandReload());

        event.registerServerCommand(new CommandSelect());
        event.registerServerCommand(new CommandSelectShift());
        event.registerServerCommand(new CommandSelectCrop());
        event.registerServerCommand(new CommandSelectWand());
        event.registerServerCommand(new CommandSelectShrink());
        event.registerServerCommand(new CommandSelectExpand());

        if (!RecurrentComplex.isLite())
        {
            event.registerServerCommand(new CommandPreview());
            event.registerServerCommand(confirm = new CommandConfirm());
            event.registerServerCommand(cancel = new CommandCancel());
        }

        event.registerServerCommand(reopen = new CommandReopen());

        if (!RecurrentComplex.isLite())
            event.registerServerCommand(new CommandVisual());

        event.registerServerCommand(new CommandSelectFill());
        event.registerServerCommand(new CommandSelectReplace());
        event.registerServerCommand(new CommandSetProperty());
        event.registerServerCommand(new CommandSelectFillSphere());
        if (!RecurrentComplex.isLite())
        {
            event.registerServerCommand(new CommandSelectFloor());
            event.registerServerCommand(new CommandSelectSpace());
            event.registerServerCommand(new CommandSelectNatural());
        }
        event.registerServerCommand(new CommandSelectSetBiome());

        event.registerServerCommand(new CommandSelectCopy());
        event.registerServerCommand(new CommandPaste());
        event.registerServerCommand(new CommandPasteGen());

        event.registerServerCommand(new CommandSelectMove());
        event.registerServerCommand(new CommandSelectDuplicate());

        event.registerServerCommand(new CommandBiomeDict());
        event.registerServerCommand(new CommandDimensionDict());

        event.registerServerCommand(new CommandImportSchematic());
        event.registerServerCommand(new CommandExportSchematic());

        event.registerServerCommand(new CommandWhatIsThis());
        event.registerServerCommand(forget = new CommandForget());
        event.registerServerCommand(new CommandForgetAll());
        event.registerServerCommand(new CommandSelectRemember());

        event.registerServerCommand(lookup = new CommandLookupStructure());
        event.registerServerCommand(list = new CommandListStructures());
        event.registerServerCommand(new CommandSearchStructure());

        if (event.getSide() == Side.CLIENT) // TODO Make client-side command (don't even send to server)
            event.registerServerCommand(new CommandBrowseFiles());

        event.registerServerCommand(new CommandRetrogen());
        event.registerServerCommand(new CommandDecorate());
        event.registerServerCommand(new CommandDecorateOne());

        event.registerServerCommand(new CommandEval());
    }

    @Nonnull
    public static StructureEntityInfo getStructureEntityInfo(Object object, @Nullable EnumFacing facing) throws CommandException
    {
        StructureEntityInfo info = StructureEntityInfo.get(object, facing);

        if (info == null)
            throw ServerTranslations.commandException("commands.rc.noEntityInfo");

        return info;
    }

    @Nonnull
    public static SelectionOwner getSelectionOwner(Object object, @Nullable EnumFacing facing, boolean ensureValid) throws CommandException
    {
        SelectionOwner owner = SelectionOwner.getOwner(object, facing);

        if (owner == null)
            throw ServerTranslations.commandException("commands.rc.noSelection");

        if (ensureValid && !owner.hasValidSelection())
            throw ServerTranslations.commandException("commands.selectModify.noSelection");

        return owner;
    }

    public static void assertSize(ICommandSender sender, SelectionOwner owner) throws CommandException
    {
        int[] sides = owner.getSelection().areaSize();
        long size = (long) sides[0] * (long) sides[1] * (long) sides[2];

        if (size >= (long) Integer.MAX_VALUE)
            throw ServerTranslations.commandException("commands.rc.large.error");
        else if (size >= 100 * 100 * 100)
            sender.sendMessage(ServerTranslations.get("commands.rc.large.warn"));
    }

    public static BlockPos parseBlockPos(BlockPos blockpos, String[] args, int startIndex, boolean centerBlock) throws NumberInvalidException
    {
        return new BlockPos(CommandBase.parseDouble((double) blockpos.getX(), args[startIndex], -30000000, 30000000, centerBlock), CommandBase.parseDouble((double) blockpos.getY(), args[startIndex + 1], 0, 256, false), CommandBase.parseDouble((double) blockpos.getZ(), args[startIndex + 2], -30000000, 30000000, centerBlock));
    }

    public static BlockPos tryParseBlockPos(ICommandSender sender, String[] args, int startIndex, boolean centerBlock) throws NumberInvalidException
    {
        return args.length >= startIndex + 3
                ? CommandBase.parseBlockPos(sender, args, startIndex, centerBlock)
                : sender.getPosition();
    }

    @Nonnull
    public static BlockSurfacePos tryParseSurfaceBlockPos(ICommandSender sender, String[] args, int startIndex, boolean centerBlock) throws NumberInvalidException
    {
        return args.length >= startIndex + 2
                ? parseSurfaceBlockPos(sender, args, startIndex, centerBlock)
                : BlockSurfacePos.from(sender.getPosition());
    }

    public static BlockSurfacePos parseSurfaceBlockPos(ICommandSender sender, String[] args, int startIndex, boolean centerBlock) throws NumberInvalidException
    {
        return parseSurfaceBlockPos(sender.getPosition(), args, startIndex, centerBlock);
    }

    public static BlockSurfacePos parseSurfaceBlockPos(BlockPos blockpos, String[] args, int startIndex, boolean centerBlock) throws NumberInvalidException
    {
        return BlockSurfacePos.from(new BlockPos(CommandBase.parseDouble((double) blockpos.getX(), args[startIndex], -30000000, 30000000, centerBlock), 0, CommandBase.parseDouble((double) blockpos.getZ(), args[startIndex + 1], -30000000, 30000000, centerBlock)));
    }

    public static List<String> completeRotation(String[] args)
    {
        return CommandBase.getListOfStringsMatchingLastWord(args, "0", "1", "2", "3");
    }

    public static List<String> completeMirror(String[] args)
    {
        return CommandBase.getListOfStringsMatchingLastWord(args, "false", "true");
    }

    public static List<String> completeTransform(String[] args, int index)
    {
        return index == 0 ? completeRotation(args)
                : index == 1 ? completeMirror(args)
                : Collections.emptyList();
    }

    public static AxisAlignedTransform2D tryParseTransform(String[] args, int index) throws CommandException
    {
        return AxisAlignedTransform2D.from(tryParseRotation(args, index), tryParseMirror(args, index + 1));
    }

    public static int tryParseRotation(String[] args, int index) throws NumberInvalidException
    {
        return args.length > index ? CommandBase.parseInt(args[index]) : 0;
    }

    public static boolean tryParseMirror(String[] args, int mirrorIndex) throws CommandException
    {
        return args.length > mirrorIndex && CommandBase.parseBoolean(args[mirrorIndex]);
    }

    @Nonnull
    public static List<String> completeDimension(String[] args)
    {
        return CommandBase.getListOfStringsMatchingLastWord(args, Arrays.stream(DimensionManager.getIDs()).map(String::valueOf).collect(Collectors.toList()));
    }

    public static WorldServer tryParseDimension(ICommandSender commandSender, String[] args, int dimIndex) throws CommandException
    {
        WorldServer world = args.length <= dimIndex || args[dimIndex].equals("~") ? (WorldServer) commandSender.getEntityWorld() : DimensionManager.getWorld(CommandBase.parseInt(args[dimIndex]));
        if (world == null)
            throw ServerTranslations.commandException("commands.rc.nodimension");
        return world;
    }

    @Nonnull
    protected static ResourceMatcher tryParseResourceMatcher(String[] args, int startPos)
    {
        return ExpressionCache.of(new ResourceMatcher((s1) -> !s1.isEmpty()), args.length >= startPos ? CommandBase.buildString(args, startPos) : "");
    }

    @Nonnull
    protected static Predicate<StructureInfo> tryParseStructurePredicate(String[] args, int startPos, Supplier<Predicate<StructureInfo>> fallback)
    {
        return args.length >= startPos
                ? s -> tryParseResourceMatcher(args, startPos).test(StructureRegistry.INSTANCE.resourceLocation(s))
                : fallback.get();
    }

    @Nonnull
    protected static List<String> completeResourceMatcher(String[] args)
    {
        return CommandBase.getListOfStringsMatchingLastWord(args, StructureRegistry.INSTANCE.ids());
    }

    public static Biome parseBiome(String arg) throws CommandException
    {
        ResourceLocation biomeID = new ResourceLocation(arg);
        if (!Biome.REGISTRY.containsKey(biomeID))
            throw ServerTranslations.commandException("commands.rc.nobiome");

        return Biome.REGISTRY.getObject(biomeID);
    }

    public static List<String> completeBiome(String[] args)
    {
        return CommandBase.getListOfStringsMatchingLastWord(args, Biome.REGISTRY.getKeys());
    }

    public static void informDeleteResult(Pair<Set<Path>, Set<Path>> result, ICommandSender sender, String filetype, String id, String path)
    {
        if (result.getRight().size() > 0)
            sender.sendMessage(ServerTranslations.format("reccomplex.delete.failure", filetype, String.format("%s/%s", path, id)));
        else if (result.getLeft().size() > 0)
            sender.sendMessage(ServerTranslations.format("reccomplex.delete.success", filetype, String.format("%s/%s", path, id)));
    }

    public static boolean informSaveResult(boolean result, ICommandSender sender, String path, String filetype, String id)
    {
        if (result)
            sender.sendMessage(ServerTranslations.format("reccomplex.save.success", filetype, String.format("%s/%s", path, id)));
        else
            sender.sendMessage(ServerTranslations.format("reccomplex.save.failure", filetype, String.format("%s/%s", path, id)));

        return result;
    }

    public static int[] parseMetadatas(String arg) throws CommandException
    {
        try
        {
            String[] strings = arg.split(",");
            int[] ints = new int[strings.length];

            for (int i = 0; i < strings.length; i++)
            {
                ints[i] = Integer.valueOf(strings[i]);
            }

            return ints;
        }
        catch (Exception ex)
        {
            throw ServerTranslations.wrongUsageException("commands.selectModify.invalidMetadata", arg);
        }
    }
}
