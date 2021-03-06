/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.commands;

import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.utils.ServerTranslations;
import ivorius.reccomplex.world.gen.feature.StructureGenerationData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lukas on 25.05.14.
 */
public class CommandWhatIsThis extends CommandBase
{
    @Nonnull
    public static ITextComponent entryTextComponent(StructureGenerationData.Entry entry)
    {
        TextComponentString forget = new TextComponentString("X");
        String uuidString = entry.getUuid().toString();
        forget.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                String.format("/%s %s", RCCommands.forget.getName(), uuidString)));
        forget.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                ServerTranslations.format("commands.rcforget.forget", uuidString)));
        forget.getStyle().setColor(TextFormatting.RED);

        TextComponentString structure = entry instanceof StructureGenerationData.StructureEntry
                ? CommandSearchStructure.structureTextComponent(entry.description())
                : new TextComponentString(entry.description());

        return new TextComponentTranslation("%s (%s)", structure, forget);
    }

    @Override
    public String getName()
    {
        return RCConfig.commandPrefix + "whatisthis";
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender var1)
    {
        return ServerTranslations.usage("commands.whatisthis.usage");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException
    {
        World world = commandSender.getEntityWorld();

        BlockPos pos = RCCommands.tryParseBlockPos(commandSender, args, 0, false);

        List<StructureGenerationData.Entry> entries = StructureGenerationData.get(world).entriesAt(pos).collect(Collectors.toCollection(ArrayList::new));
        if (entries.size() > 0)
            commandSender.sendMessage(ServerTranslations.format(new ArrayList<ITextComponent>().size() > 1 ? "commands.whatisthis.many" : "commands.whatisthis.one",
                    ServerTranslations.join(entries.stream().map(CommandWhatIsThis::entryTextComponent).collect(Collectors.toList()))));
        else
            commandSender.sendMessage(ServerTranslations.format("commands.whatisthis.none"));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        if (args.length == 1 || args.length == 2 || args.length == 3)
            return getTabCompletionCoordinate(args, 0, pos);

        return Collections.emptyList();
    }
}
