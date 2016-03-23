/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.structures.generic.maze;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.ivtoolkit.maze.components.MazeRoom;
import ivorius.ivtoolkit.maze.components.MazeRoomConnection;
import ivorius.ivtoolkit.maze.components.MazeRoomConnections;
import ivorius.ivtoolkit.tools.NBTCompoundObject;
import ivorius.ivtoolkit.tools.NBTCompoundObjects;
import ivorius.ivtoolkit.tools.NBTTagLists;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.structures.generic.Selection;
import ivorius.reccomplex.utils.NBTCompoundObjects2;
import ivorius.reccomplex.utils.NBTTagLists2;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 18.01.16.
 */
public class SavedMazeReachability implements NBTCompoundObject
{
    private static final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();

    public final List<Set<SavedMazePath>> groups = new ArrayList<>();
    public final List<ImmutablePair<SavedMazePath, SavedMazePath>> crossConnections = new ArrayList<>();

    public <T extends Map.Entry<SavedMazePath, SavedMazePath>> SavedMazeReachability(List<Set<SavedMazePath>> groups, List<T> crossConnections)
    {
        set(groups, crossConnections);
    }

    public SavedMazeReachability()
    {
    }


    public static Predicate<MazeRoomConnection> notBlocked(final Collection<Connector> blockedConnections, final Map<MazeRoomConnection, Connector> connections)
    {
        return input -> !blockedConnections.contains(connections.get(input));
    }

    public static Set<SavedMazePath> buildExpected(SavedMazeComponent savedMazeComponent)
    {
        Set<SavedMazePath> complete = Sets.newHashSet(savedMazeComponent.exitPaths.stream().map(input -> input.path).collect(Collectors.toList()));
        completeExitPaths(complete, savedMazeComponent.rooms);
        return complete;
    }

    /**
     * Analogous to WorldGenMaze.completeExitPaths
     * @param exits
     * @param rooms
     */
    public static void completeExitPaths(Set<SavedMazePath> exits, Selection rooms)
    {
        for (MazeRoom room : rooms.mazeRooms(true))
            SavedMazePaths.neighbors(room).stream().filter(connection -> !exits.contains(connection) && !(rooms.contains(connection.getSourceRoom()) && rooms.contains(connection.getDestRoom()))).forEach(exits::add);
    }

    public void set(SavedMazeReachability reachability)
    {
        set(reachability.groups, reachability.crossConnections);
    }

    public <T extends Map.Entry<SavedMazePath, SavedMazePath>> void set(List<Set<SavedMazePath>> groups, List<T> crossConnections)
    {
        groups.clear();
        for (Set<SavedMazePath> group : groups)
            groups.add(Sets.newHashSet(group.stream().map(SavedMazePath::copy).collect(Collectors.toList())));

        crossConnections.clear();
        for (Map.Entry<SavedMazePath, SavedMazePath> entry : crossConnections)
        {
            this.crossConnections.add(ImmutablePair.of(entry.getKey().copy(), entry.getValue().copy()));
        }
    }

    public ImmutableSet<Pair<MazeRoomConnection, MazeRoomConnection>> build(final AxisAlignedTransform2D transform, final int[] size, Predicate<MazeRoomConnection> filter, Set<MazeRoomConnection> connections)
    {
        filter = Predicates.and(Predicates.in(connections), filter);

        ImmutableSet.Builder<Pair<MazeRoomConnection, MazeRoomConnection>> builder = ImmutableSet.builder();
        Set<MazeRoomConnection> defaultGroup = Sets.newHashSet(connections);

        for (Set<SavedMazePath> group : groups)
        {
            FluentIterable<MazeRoomConnection> existing = FluentIterable.from(group).transform(savedMazePath -> MazeRoomConnections.rotated(savedMazePath.toRoomConnection(), transform, size)).filter(filter);

            for (MazeRoomConnection left : existing)
                defaultGroup.remove(left);

            addInterconnections(builder, existing);
        }

        addInterconnections(builder, defaultGroup);

        for (Map.Entry<SavedMazePath, SavedMazePath> entry : crossConnections)
        {
            MazeRoomConnection key = MazeRoomConnections.rotated(entry.getKey().toRoomConnection(), transform, size);
            MazeRoomConnection val = MazeRoomConnections.rotated(entry.getValue().toRoomConnection(), transform, size);

            if (filter.apply(key) && filter.apply(val))
                builder.add(Pair.of(key, val));
        }

        return builder.build();
    }

    protected void addInterconnections(ImmutableSet.Builder<Pair<MazeRoomConnection, MazeRoomConnection>> builder, Iterable<MazeRoomConnection> existing)
    {
        MazeRoomConnection last = null;
        for (MazeRoomConnection current : existing)
        {
            if (last != null) // It's enough to make a transitive connection in both directions
            {
                builder.add(Pair.of(last, current));
                builder.add(Pair.of(current, last));
            }

            last = current;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        groups.clear();
        groups.addAll(Lists.transform(NBTTagLists2.listsFrom(compound, "groups"), input -> Sets.newHashSet(NBTCompoundObjects.readList(input, SavedMazePath.class))));

        crossConnections.clear();
        crossConnections.addAll(Lists.transform(NBTTagLists.compoundsFrom(compound, "crossConnections"), input -> ImmutablePair.of(NBTCompoundObjects2.readFrom(input, "key", SavedMazePath.class), NBTCompoundObjects2.readFrom(input, "val", SavedMazePath.class))));
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        NBTTagLists2.writeNbt(compound, "groups", Lists.transform(groups, NBTCompoundObjects::writeList));

        NBTTagLists.writeCompoundsTo(compound, "crossConnections", Lists.transform(crossConnections, input -> {
            NBTTagCompound compound1 = new NBTTagCompound();
            NBTCompoundObjects2.writeTo(compound1, "key", input.getKey());
            NBTCompoundObjects2.writeTo(compound1, "val", input.getValue());
            return compound1;
        }));
    }

    public static class Serializer implements JsonSerializer<SavedMazeReachability>, JsonDeserializer<SavedMazeReachability>
    {
        @Override
        public SavedMazeReachability deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = JsonUtils.getJsonElementAsJsonObject(json, "MazeRoom");

            List<Set<SavedMazePath>> groups = context.deserialize(jsonObject.get("groups"), new TypeToken<List<Set<SavedMazePath>>>(){}.getType());
            if (groups == null)
                groups = Collections.emptyList();

            List<ImmutablePair<SavedMazePath, SavedMazePath>> crossConnections = gson.fromJson(jsonObject.get("crossConnections"), new TypeToken<List<ImmutablePair<SavedMazePath, SavedMazePath>>>(){}.getType());
            if (crossConnections == null)
                crossConnections = Collections.emptyList();

            return new SavedMazeReachability(groups, crossConnections);
        }

        @Override
        public JsonElement serialize(SavedMazeReachability src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.add("source", context.serialize(src.groups));
            jsonObject.addProperty("pathDimension", gson.toJson(src.crossConnections));

            return jsonObject;
        }
    }
}
