package org.luwrain.io.api.ya360;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable client configuration loaded from a JSON file or provided directly.
 */
public final class Ya360Config
{
    public static final String DEFAULT_PATH_PROPERTY = "yamb.config";

    private final String cookie;
    private final String myUid;
    private final String myGuid;
    private final Map<String, String> cookies;

    /**
     * Creates a configuration from raw values.
     *
     * @param cookie full Cookie header value
     * @param myUid  numeric Passport uid as a decimal string
     * @param myGuid own messenger guid
     * @throws Ya360ConfigException if a required value is missing or cookie misses a required part
     */
    public Ya360Config(String cookie, String myUid, String myGuid) throws Ya360ConfigException
    {
        if (cookie == null || cookie.isBlank())
        {
            throw new Ya360ConfigException("cookie is required");
        }
        if (myUid == null || myUid.isBlank())
        {
            throw new Ya360ConfigException("my_uid is required");
        }
        if (myGuid == null || myGuid.isBlank())
        {
            throw new Ya360ConfigException("my_guid is required");
        }
        this.cookie = cookie;
        this.myUid = myUid.trim();
        this.myGuid = myGuid.trim();
        this.cookies = parseCookies(cookie);
        if (!cookies.containsKey("Session_id"))
        {
            throw new Ya360ConfigException("cookie must contain Session_id");
        }
        if (!cookies.containsKey("yandexuid"))
        {
            throw new Ya360ConfigException("cookie must contain yandexuid");
        }
    }

    /**
     * @return raw Cookie header value
     */
    public String cookie()
    {
        return cookie;
    }

    /**
     * @return numeric Passport uid as a decimal string
     */
    public String myUid()
    {
        return myUid;
    }

    /**
     * @return own messenger guid
     */
    public String myGuid()
    {
        return myGuid;
    }

    /**
     * @return yandexuid cookie value parsed from the cookie header
     */
    public String yandexUid()
    {
        return cookies.get("yandexuid");
    }

    /**
     * @param name cookie name, case sensitive
     * @return cookie value or null when absent
     */
    public String cookieValue(String name)
    {
        if (name == null)
        {
            return null;
        }
        return cookies.get(name);
    }

    /**
     * Loads configuration from the default path, which can be overridden with
     * the {@link #DEFAULT_PATH_PROPERTY} system property.
     *
     * @return loaded configuration
     * @throws Ya360ConfigException on IO error or invalid configuration
     */
    public static Ya360Config load() throws Ya360ConfigException
    {
        return load(defaultPath());
    }

    /**
     * Loads configuration from an explicit path.
     *
     * @param path config file path
     * @return loaded configuration
     * @throws Ya360ConfigException on IO error or invalid configuration
     */
    public static Ya360Config load(Path path) throws Ya360ConfigException
    {
        if (path == null)
        {
            throw new Ya360ConfigException("config path is null");
        }
        try
        {
            final String text = Files.readString(path);
            final JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            final String cookie = requiredString(root, "cookie");
            final String myUid = requiredString(root, "my_uid");
            final String myGuid = requiredString(root, "my_guid");
            return new Ya360Config(cookie, myUid, myGuid);
        }
        catch (Ya360ConfigException e)
        {
            throw e;
        }
        catch (IOException | RuntimeException e)
        {
            throw new Ya360ConfigException("Failed to load config from " + path, e);
        }
    }

    /**
     * @return default config file path, honoring the {@code yamb.config} system property
     */
    public static Path defaultPath()
    {
        final String override = System.getProperty(DEFAULT_PATH_PROPERTY);
        if (override != null && !override.isBlank())
        {
            return Path.of(override);
        }
        final String home = System.getProperty("user.home");
        return Path.of(home, ".config", "yandex-messenger", "config.json");
    }

    private static String requiredString(JsonObject object, String name) throws Ya360ConfigException
    {
        if (!object.has(name) || object.get(name).isJsonNull())
        {
            throw new Ya360ConfigException("Missing required config field: " + name);
        }
        return object.get(name).getAsString();
    }

    private static Map<String, String> parseCookies(String cookieHeader)
    {
        final Map<String, String> result = new LinkedHashMap<>();
        for (final String part : cookieHeader.split(";"))
        {
            final String trimmed = part.trim();
            if (trimmed.isEmpty())
            {
                continue;
            }
            final int equals = trimmed.indexOf('=');
            if (equals < 0)
            {
                result.put(trimmed, "");
            }
            else
            {
                result.put(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
            }
        }
        return result;
    }
}

