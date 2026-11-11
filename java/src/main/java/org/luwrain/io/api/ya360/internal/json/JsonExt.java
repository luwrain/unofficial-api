package org.luwrain.io.api.ya360.internal.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers to read Gson elements without spreading null checks everywhere.
 */
public final class JsonExt
{
    private JsonExt()
    {
    }

    public static String string(JsonObject object, String name)
    {
        if (object == null || !object.has(name) || object.get(name).isJsonNull())
        {
            return null;
        }
        return object.get(name).getAsString();
    }

    public static Integer integer(JsonObject object, String name)
    {
        if (object == null || !object.has(name) || object.get(name).isJsonNull())
        {
            return null;
        }
        return object.get(name).getAsInt();
    }

    public static Long longValue(JsonObject object, String name)
    {
        if (object == null || !object.has(name) || object.get(name).isJsonNull())
        {
            return null;
        }
        return object.get(name).getAsLong();
    }

    public static Boolean bool(JsonObject object, String name)
    {
        if (object == null || !object.has(name) || object.get(name).isJsonNull())
        {
            return null;
        }
        return object.get(name).getAsBoolean();
    }

    public static JsonObject object(JsonObject object, String name)
    {
        if (object == null || !object.has(name) || object.get(name).isJsonNull())
        {
            return null;
        }
        return object.get(name).getAsJsonObject();
    }

    public static JsonObject firstObject(JsonObject object, String... names)
    {
        for (final String name : names)
        {
            final JsonObject value = object(object, name);
            if (value != null)
            {
                return value;
            }
        }
        return null;
    }

    public static List<JsonObject> objectList(JsonObject object, String name)
    {
        final List<JsonObject> result = new ArrayList<>();
        if (object == null || !object.has(name) || object.get(name).isJsonNull())
        {
            return result;
        }
        for (final JsonElement element : object.getAsJsonArray(name))
        {
            if (element != null && element.isJsonObject())
            {
                result.add(element.getAsJsonObject());
            }
        }
        return result;
    }
}

