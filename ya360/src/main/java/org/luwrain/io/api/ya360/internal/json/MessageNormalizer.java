package org.luwrain.io.api.ya360.internal.json;

import com.google.gson.JsonObject;
import org.luwrain.io.api.ya360.internal.timestamps.Timestamps;
import org.luwrain.io.api.ya360.model.AttachmentRef;
import org.luwrain.io.api.ya360.model.Message;
import org.luwrain.io.api.ya360.model.MessageRef;
import org.luwrain.io.api.ya360.model.MessageSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalizes the ServerMessage wire shape into a {@link Message} value object.
 */
public final class MessageNormalizer
{
    private MessageNormalizer()
    {
    }

    public static Message normalize(JsonObject serverMessage)
    {
        if (serverMessage == null)
        {
            return null;
        }
        final JsonObject client = JsonExt.object(serverMessage, "ClientMessage");
        if (client == null)
        {
            return null;
        }
        final JsonObject info = JsonExt.object(serverMessage, "ServerMessageInfo");

        final JsonObject content = JsonExt.firstObject(client, "Plain", "SystemMessage");
        final boolean system = JsonExt.object(client, "SystemMessage") != null;

        final String timestampMcs = info == null ? null : longString(JsonExt.longValue(info, "Timestamp"));
        final String timestampIso = timestampMcs == null ? null : Timestamps.microsToIso(timestampMcs);

        final JsonObject from = info == null ? null : JsonExt.object(info, "From");
        final MessageSender sender = from == null
            ? new MessageSender(null, null)
            : new MessageSender(JsonExt.string(from, "Guid"), JsonExt.string(from, "DisplayName"));

        String kind;
        String text = null;
        final List<AttachmentRef> attachments = new ArrayList<>();

        if (system)
        {
            kind = "system";
        }
        else if (content == null)
        {
            kind = "unknown";
        }
        else
        {
            if (JsonExt.object(content, "Text") != null)
            {
                kind = "text";
                final JsonObject textObject = JsonExt.object(content, "Text");
                text = JsonExt.string(textObject, "MessageText");
            }
            else if (JsonExt.object(content, "Sticker") != null)
            {
                kind = "sticker";
            }
            else if (JsonExt.object(content, "Image") != null)
            {
                kind = "image";
                attachments.addAll(extractImage(JsonExt.object(content, "Image")));
            }
            else if (JsonExt.object(content, "MiscFile") != null)
            {
                kind = "file";
                attachments.addAll(extractFile(JsonExt.object(content, "MiscFile")));
            }
            else if (JsonExt.object(content, "Card") != null)
            {
                kind = "card";
            }
            else if (JsonExt.object(content, "Gallery") != null)
            {
                kind = "gallery";
                attachments.addAll(extractGallery(JsonExt.object(content, "Gallery")));
                final JsonObject gallery = JsonExt.object(content, "Gallery");
                final JsonObject galleryText = JsonExt.object(gallery, "Text");
                text = galleryText == null ? null : JsonExt.string(galleryText, "MessageText");
            }
            else if (JsonExt.object(content, "Voice") != null)
            {
                kind = "voice";
                final JsonObject voice = JsonExt.object(content, "Voice");
                attachments.addAll(extractVoice(voice));
                final JsonObject voiceText = JsonExt.object(voice, "Text");
                text = voiceText == null ? null : JsonExt.string(voiceText, "MessageText");
            }
            else if (JsonExt.object(content, "Poll") != null)
            {
                kind = "poll";
            }
            else
            {
                kind = "unknown";
            }
        }

        final boolean deleted = info != null && Boolean.TRUE.equals(JsonExt.bool(info, "Deleted"));
        if (deleted)
        {
            kind = "deleted";
        }

        final Long lastEditMcs = info == null ? null : JsonExt.longValue(info, "LastEditTimestamp");
        final boolean edited = lastEditMcs != null && lastEditMcs > 0;
        final String editedAtIso = edited ? Timestamps.microsToIso(lastEditMcs) : null;

        final Integer seqNo = info == null ? null : JsonExt.integer(info, "SeqNo");

        return new Message(
            timestampMcs,
            content == null ? null : JsonExt.string(content, "ChatId"),
            timestampIso,
            timestampMcs,
            seqNo,
            sender,
            kind,
            text,
            attachments,
            null,
            edited,
            editedAtIso,
            deleted
        );
    }

    public static MessageRef normalizeRef(JsonObject ref)
    {
        if (ref == null)
        {
            return null;
        }
        final String timestampMcs = longString(JsonExt.longValue(ref, "Timestamp"));
        return new MessageRef(
            JsonExt.string(ref, "ChatId"),
            timestampMcs,
            timestampMcs == null ? null : Timestamps.microsToIso(timestampMcs)
        );
    }

    private static String longString(Long value)
    {
        return value == null ? null : Long.toString(value);
    }

    private static List<AttachmentRef> extractImage(JsonObject image)
    {
        if (image == null)
        {
            return List.of();
        }
        return List.of(attachment("image", JsonExt.object(image, "FileInfo"), image));
    }

    private static List<AttachmentRef> extractFile(JsonObject file)
    {
        if (file == null)
        {
            return List.of();
        }
        return List.of(attachment("file", JsonExt.object(file, "FileInfo"), file));
    }

    private static List<AttachmentRef> extractVoice(JsonObject voice)
    {
        if (voice == null)
        {
            return List.of();
        }
        return List.of(attachment("voice", JsonExt.object(voice, "FileInfo"), voice));
    }

    private static List<AttachmentRef> extractGallery(JsonObject gallery)
    {
        if (gallery == null)
        {
            return List.of();
        }
        final List<AttachmentRef> result = new ArrayList<>();
        for (final JsonObject item : JsonExt.objectList(gallery, "Items"))
        {
            final JsonObject image = JsonExt.object(item, "Image");
            final AttachmentRef ref = attachment("gallery_image", JsonExt.object(image, "FileInfo"), image);
            if (ref != null)
            {
                result.add(ref);
            }
        }
        return result;
    }

    private static AttachmentRef attachment(String kind, JsonObject fileInfo, JsonObject metadata)
    {
        if (fileInfo == null)
        {
            return null;
        }
        final Long sourceRaw = JsonExt.longValue(fileInfo, "Source");
        final String source = Long.valueOf(1L).equals(sourceRaw) ? "disk"
            : Long.valueOf(0L).equals(sourceRaw) ? "mds"
            : "unknown";
        return new AttachmentRef(
            kind,
            JsonExt.string(fileInfo, "Id2"),
            JsonExt.string(fileInfo, "Name"),
            JsonExt.longValue(fileInfo, "Size"),
            source,
            metadata == null ? null : JsonExt.integer(metadata, "Width"),
            metadata == null ? null : JsonExt.integer(metadata, "Height"),
            metadata == null ? null : JsonExt.bool(metadata, "Animated"),
            metadata == null ? null : JsonExt.integer(metadata, "Duration")
        );
    }
}

