/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.json;

import com.google.gson.*;
import net.minecraft.nbt.NBTTagByteArray;

import java.lang.reflect.Type;

/**
 * Created by lukas on 25.05.14.
 */
public class NBTTagByteArraySerializer implements JsonSerializer<NBTTagByteArray>, JsonDeserializer<NBTTagByteArray>
{
    @Override
    public JsonElement serialize(NBTTagByteArray src, Type typeOfSrc, JsonSerializationContext context)
    {
        return context.serialize(src.getByteArray(), byte[].class);
    }

    @Override
    public NBTTagByteArray deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        return new NBTTagByteArray(context.deserialize(json, byte[].class));
    }
}
