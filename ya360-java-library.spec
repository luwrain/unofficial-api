# Java Library Specification: Yandex Messenger API Client (ya360)

Status: approved draft for the first milestone
Package: org.luwrain.io.api.ya360
Language: Java 17
Build system: Gradle (Groovy DSL or Kotlin DSL, author's choice)
Form factor: library only, no CLI, no server

This document captures the full reasoning and protocol knowledge gathered from two
reference implementations so that work can be continued from this point without
re-reading the whole repository.

Reference sources already read in full:

- ts/ - TypeScript MCP server (yandex-messenger-mcp v0.4.0). This is the most complete
  implementation of the protocol and the source of truth for wire-level details.
- py/ - Python stdlib CLI (yamb_config.py, yamb_read.py, yamb_send.py, yamb_api.py).
  Useful for the minimal subset and for the pieces that do not depend on a browser.
- ts/protocol-research.md - the reverse engineering reference for the protocol.
- ts/deep-interview-v1.md and ts/deep-interview-v2.md - product and confirm-policy specs.

---

## 1. Purpose

Write a Java 17 library that can talk to Yandex Messenger
(messenger.360.yandex.ru, tld ru) using the same unofficial, reverse engineered
protocol as the TypeScript MCP server and the Python CLI. The library must be usable
from an external application. There is no CLI and no MCP server in this milestone.

The library must implement a minimal but genuinely working vertical slice:

1. Load authentication identity from a JSON config file.
2. Open the xiva WebSocket and speak the binary frame protocol.
3. Read the chat list.
4. Read a page of history from one chat.
5. Read one message by timestamp (optional but cheap and useful).
6. Send a plain text message.
7. Resolve a private chat id from a partner guid and my own guid.
8. Derive and read threads.

Everything else (attachments download/upload, search, reactions, polls, mentions,
edit/delete, join/leave threads, mark read, etc.) is explicitly out of scope for
this milestone and must not leak into the public API in a half working form.

---

## 2. High level architecture

Yandex Messenger uses two transports:

1. WebSocket (xiva push):
   wss://push.yandex.ru/v2/subscribe/websocket

   This is the ONLY channel for reading history and sending messages.

2. HTTP registry:
   POST https://yandex.ru/messenger/api/registry/api/

   Used for search, user metadata, chat metadata, file upload/download, threads
   subscription. For the minimal milestone this transport is deliberately deferred,
   except that the protocol notes below describe it enough to add it later.

Because history and push are both on WebSocket, the minimal library can be built
entirely around one WebSocket client plus a small number of JSON shape normalizers.

Token/browser automation is not required by this milestone. Authentication is a plain
cookie string supplied in the config file. This mirrors the Python client's spirit but
removes the Firefox SQLite dependency.

---

## 3. Minimal scope (this milestone)

In scope:

- Load config from JSON.
- Parse the cookie string into a header and individual cookie values.
- Build the xiva WebSocket URL with percent encoding.
- Open the WebSocket over TLS with OkHttp.
- Implement the frame codec (encode and decode).
- Implement request/response correlation.
- Capture the operation frame `subscribed` and use its `subscription-id` for push.
- Handle app level ping text frames and detect a dead connection.
- whoami (optional but useful for verifying identity).
- history:
  - list chats (all chats with minimal last message metadata);
  - read one chat history page with pagination cursor;
  - support a raw before-cursor (micros string) from the previous page.
- push:
  - send one plain text message.
- message_info:
  - read one message by chat id + timestamp.
- Thread id derivation (pure string math) and reading a thread as a chat.
- Resolve a private chat id from a partner guid using UTF-16 code-unit ordering.
- Error typing for the three protocol error layers.

Out of scope (explicitly deferred):

- HTTP registry methods (search, get_chats_info, get_users_data, request_user,
  csrf-token, join_to_thread, leave_thread, add_files, upload_to_disk).
- Attachments download and upload.
- Reactions, polls, mentions, reply/forward, edit, delete, pin, mark read.
- Real time events and bot style subscriptions.
- OAuth/uniproxy transport.
- Browser based login and session refresh.
- Multi account management.

---

## 4. Technology decisions

### 4.1 Build: Gradle, Java 17

Use Gradle with the `java-library` plugin. Set the Java toolchain to 17.

Reasoning: Gradle is requested by the user. Java 17 is requested by the user.
`java-library` is appropriate because this module has no main class and no application
entry point.

### 4.2 HTTP and WebSocket: OkHttp

OkHttp is used for TLS and the WebSocket. OkHttp has a built in WebSocket client
(`okhttp3.WebSocket`) that handles:

- TLS;
- RFC6455 handshake;
- binary and text frames;
- protocol level ping/pong automatically.

This replaces the ~150 lines of socket code in the Python client with roughly 40 lines.

Recommended dependency version: `com.squareup.okhttp3:okhttp:4.12.0`. OkHttp 4.x is
stable and supports Java 8+ (hence Java 17). OkHttp 5.x was not stable at the time of
writing; it is not required.

### 4.3 JSON: Gson

Gson is used for serialization and deserialization. Gson is enough because only plain
JSON objects, strings and numbers are read from the wire.

Recommended dependency version: `com.google.code.gson:gson:2.11.0`. Gson 2.10+ supports
records, which makes value objects convenient.

### 4.4 MessagePack: no external dependency

An external MessagePack library is NOT needed.

The frame header is the only MessagePack payload, and its shape is fixed and tiny:

    msgpack fixarray length 3: [serviceIndex, seq, method]

Only three MessagePack encodings are required for writing:

- positive fixint for uint in [0, 0x7f);
- uint8 (0xcc), uint16 (0xcd), uint32 (0xce) for larger seq values;
- fixstr (0xa0 | length) for method strings shorter than 32 bytes;
- str8 (0xd9) and str16 (0xda) for longer method names (defensive only).

Only three decodings are required for reading:

- uint as above;
- fixstr/str8/str16 as above.

This is about 80 lines of Java and avoids the dependency and the byte offset
bookkeeping that a full MessagePack library would add.

---

## 5. Protocol summary

This section is a self contained restatement of the parts the library needs. It is
what the implementation must follow; where ambiguity exists the wire behavior is
authoritative over this document.

### 5.1 WebSocket URL

Base URL:

    wss://push.yandex.ru/v2/subscribe/websocket

Query parameters (cookie mode, logged in user):

    service=<percent encoded service>
    session=<4 groups of 4 hex characters, separated by '-'>
    client=web_main
    user=<numeric uid>

The service value is:

    messenger-prod:version5*common+version5*main

Wire encoding is x-www-form-urlencoded. Specifically:

- `:` must become `%3A`;
- `+` must become `%2B`;
- `*` remains a literal `*`.

So the actual query string contains:

    service=messenger-prod%3Aversion5*common%2Bversion5*main

IMPORTANT: do not pass the raw service string with a literal `+`; in a query string a
literal `+` means a space, and the server closes the socket with `4400 invalid
argument "service"`.

Java note: do NOT use `URLEncoder.encode` on the whole service value because `*` would
be encoded as well. Build the query manually, or use `URLEncoder.encode` first and then
restore `*`. The safest approach for a fixed constant is to use the precomputed
percent-encoded form directly.

`session` is a random client connection token. Generate 8 random bytes, hex encode them
(16 hex chars) and format as `xxxx-xxxx-xxxx-xxxx`. Example: `74d0-3c17-7924-9497`.
Use `java.security.SecureRandom`.

`user` is the numeric Passport uid as a decimal string. In cookie mode for a logged in
account this value is available from the config file.

The WebSocket handshake must carry:

    Cookie: <cookie header from config>
    Origin: https://yandex.ru

OkHttp allows custom headers on a Request passed to `newWebSocket`.

### 5.2 Frame format (binary DATA frame)

Every outgoing request is one binary WebSocket message (opcode 2). Byte layout:

    [0]                      0x01                       frame type Data
    [1..n]                   MessagePack fixarray header
                             0x93 fixarray length 3
                             + uint(serviceIndex)       always 0
                             + uint(seq)                per connection counter
                             + str(method)              e.g. "history", "push"
    [n..n+11]                12 byte data section
                             0x05
                             11 zero bytes              outgoing frames: zeros
    [n+12..]                 UTF-8 JSON body
                             { "RequestId": "<request id>", ...params }

The JSON body is plain JSON, not MessagePack.

The four header bytes are:

    0x01 0x93 0x00 <seq byte> <fixstr method>

Example for `whoami` with seq=1:

    01 93 00 01 a6 77 68 6f 61 6d 69
    05 00 00 00 00 00 00 00 00 00 00 00
    {"RequestId":"...","...":...}

Example method string bytes:

    whoami      a6 77 68 6f 61 6d 69
    history     a7 68 69 73 74 6f 72 79
    push        a4 70 75 73 68
    subscribe   a9 73 75 62 73 63 72 69 62 65

MessagePack encoding table for the header:

    uint value < 0x80            -> single byte value (positive fixint)
    uint value < 0x100           -> 0xcc followed by one byte
    uint value < 0x10000         -> 0xcd followed by two bytes big endian
    uint value <= 0xffffffff     -> 0xce followed by four bytes big endian

    string utf8 length < 0x20    -> 0xa0 | length, then bytes
    string utf8 length < 0x100   -> 0xd9, one byte length, then bytes
    string utf8 length < 0x10000 -> 0xda, two bytes length (big endian), then bytes

### 5.3 Frame decoding

Incoming binary messages have the same layout, but:

- the 4 bytes after the data section marker are a server value and must be ignored;
- frames may have arity 2 or 3 (see frame types below).

Decoding MUST advance by parsing the MessagePack element lengths, never by searching
for the 0x05 byte. The reason: when seq equals 5, the positive fixint for seq is the
byte 0x05, identical to the data section marker. A naive `indexOf(0x05)` would cut the
header at the seq byte.

Decode algorithm:

1. Read frame type at offset 0.
2. Read the fixarray tag at offset 1. Lower nibble is arity (2, 3, or 4).
3. Decode `arity` MessagePack elements starting at offset 2, tracking the next offset.
4. Header end is the offset after the last element.
5. JSON starts at headerEnd + 12.
6. If JSON bytes remain, parse with Gson; otherwise payload is absent.

Frame types (first byte):

    1 Data            header [serviceIndex, reqId, method]
    2 ProxyStatus     header [reqId, errorCode]
    3 Push            header [uid, service, event, transitId]  (server event)

For this milestone only Data responses and ProxyStatus matters for correlation.
Server Push frames are ignored but must not kill the connection.

### 5.4 RequestId and seq

There are two different ids and they must not be confused:

1. msgpack header `seq` (also called reqId in some notes):
   - per connection counter, starts at 1;
   - incremented per request;
   - reset to 1 on reconnect;
   - the response header echoes it;
   - transport correlation is done with it.

2. JSON `RequestId`:
   - backend correlation;
   - format `xxxxxxxx-xxxx-xxxx-xxxxxxxx` (group lengths 8-4-4-8);
   - generated locally with `SecureRandom`;
   - injected into every JSON body.

Both are used to match a response to a request.

RequestId generation: generate 12 random bytes, hex encode them (24 hex characters),
and format as 8-4-4-8 separated by '-'.

### 5.5 Operation text frames

Xiva sends JSON text frames after the socket opens. The library must listen to them.

`{ "operation": "subscribed", "subscription-id": "<40 hex>", ... }`

This carries the connection subscription id. `push` requires this exact id in
`ClientTransportId.XivaSubscriptionId`. A push sent without it goes nowhere. If we
reconnect, a new id is issued and the old one is invalid.

`{ "operation": "ping", "server-interval-sec": <number> }`

Application level keep alive. The server expects a first ping soon after open, and
periodic pings afterwards. The client does NOT send a pong for this text frame. The
correct action is to reset a deadline timer. Deadline after the first open is about
5 seconds; after each ping it is about interval seconds * 1.3. On deadline expiry the
socket is considered dead and should be closed/reconnected.

Other operation frames (`unsubscribe`, `xivaws-error`, etc.) can be logged and closed
as appropriate. `unsubscribe` is treated as an auth/service revocation.

OkHttp already acknowledges protocol level RFC6455 pings automatically; that is
separate from these application JSON text frames.

### 5.6 history method

Request params (only the minimal subset):

    {
      "RequestId": "<generated>",
      "Limit": <int>
      // optionally one of:
      "ChatId": "<chat id>",
      "ChatIds": ["<id1>", "<id2>", ...]
      // optional:
      "MaxTimestamp": <micros number>,  // exclusive upper boundary
      "MinTimestamp": <micros number>,  // exclusive lower boundary
      "Offset": <int>,
      "ChatDataFilter": {},
      "MessageDataFilter": { "DropPayload": true }
    }

For the list of chats use:

    { "RequestId": "...", "Limit": 1, "ChatDataFilter": {} }

NOTE: `Limit` is the number of messages per chat, not the number of chats. `Limit:1`
returns every chat plus the last message of each chat. `Limit:0` returns chat metadata
only, but then there is no last message body. The list of chats is returned in full by
one call.

For a chat history page use:

    { "RequestId": "...", "ChatId": "<chat id>", "Limit": <int> }

To paginate older messages, pass `MaxTimestamp` equal to the microsecond timestamp of
the oldest message in the previous page. `MaxTimestamp` is exclusive, so passing the
oldest timestamp as-is continues exactly after that message.

Response shape (successful case, `Status` is absent):

    {
      "Chats": [
        {
          "ChatId": "...",
          "ChatInfo": { ... } | absent,
          "PartnerInfo": { "Guid": "...", "DisplayName": "..." } | absent,
          "PrivateChatInfo": { ... } | absent,
          "Counters": { "HiddenMessageCount": ..., "TotalMessageCount": ... },
          "LastSeqNo": <int>,
          "LastSeenByMeSeqNo": <int>,
          "LastTsMcs": <micros number>,
          "Muted": <bool>,
          "Messages": [
            {
              "ServerMessage": {
                "ClientMessage": { ... },
                "ServerMessageInfo": { ... }
              }
            }
          ]
        }
      ],
      "RequestId": "..."
    }

A non-zero top level `Status` means an application error (see error section), except for
the `push` response where `Status` is a commit status.

The `Messages` page is ordered from old to new.

Unread count is `LastSeqNo - LastSeenByMeSeqNo`, clamped at zero. Do NOT use `Counters`
for unread count; that field is the chat size, not unread messages.

### 5.7 push method

`push` is the only mutation channel. It is NOT retried automatically because sending is
irreversible; a lost response may still mean the message was recorded, and replaying
would duplicate it.

Request params:

    {
      "RequestId": "<generated>",
      "ClientTransportId": { "XivaSubscriptionId": "<subscription id>" },
      "UserAgent": "chats-web/3.22.0",
      "ClientMessage": {
        "Plain": {
          "ChatId": "<chat id>",
          "PayloadId": "<uuid>",
          "Text": { "MessageText": "<text>" }
        },
        "LogData": { "YandexUid": "<yandexuid cookie value>" }
      },
      "Meta": { "Origin": 27 },
      "ClientSupportedFeatures": 0
    }

Constants:

    UserAgent = "chats-web/3.22.0"
    Origin (serviceId) = 27
    ClientSupportedFeatures = 0 (bitmask; ephemeral messages not supported)

`PayloadId` is a random UUID string (java.util.UUID.randomUUID().toString()). It is the
server-side idempotency key: retrying the same PayloadId results in `DUPLICATE(8)` and
does not create a second message.

`YandexUid` is the `yandexuid` cookie value, parsed from the configured cookie string.

Response (success):

    {
      "Status": 1,
      "MessageInfo": {
        "TimestampMcs": <micros number>,
        "PrevTimestampMcs": <micros number>,
        "SeqNo": <int>,
        "Version": <int>
      },
      "DebugInfo": { ... }
    }

Success is `Status == 1` (FULLY_COMMITTED) or `Status == 8` (DUPLICATE).

A missing numeric `Status` is treated as an unconfirmed failure, never as success.

`RateLimit` with `WaitFor` may appear on throttled pushes. The unit is not documented.
Keep the raw value and clamp any sleep to 1..60 seconds.

Commit status codes (exact numeric values):

    0  UNCOMMMITED
    1  FULLY_COMMITTED
    2  UNIPROXY_COMMITTED
    3  FAILED
    4  NO_SUCH_CHAT
    5  NOT_LEADING
    6  FOREIGN_PARTITION
    7  SENDER_NOT_IN_CHAT
    8  DUPLICATE
    9  POSTPROC_COMMITTED
    10 KIKIMR_WRITE_FAILED
    11 MESSAGE_NOT_FOUND
    12 DEQUEUED_AFTER_ERROR
    13 BAD_REQUEST
    14 FILESHARE_FAILED
    15 NO_PERMISSION
    16 CONFLICT
    17 NO_SUCH_USER
    18 THROTTLED
    19 BANNED
    20 BLACKLISTED
    21 NOT_FOUND
    22 SPAM_DETECTED
    23 RATE_LIMIT_EXCEEDED
    24 BLOCKED_BY_PRIVACY_SETTINGS
    25 PUSH_UNAUTHORIZED

### 5.8 message_info method

Request:

    { "RequestId": "...", "ChatId": "<chat id>", "Timestamp": <micros number> }

Response:

    {
      "Message": {
        "ClientMessage": { ... },
        "ServerMessageInfo": { ... }
      },
      "ErrorInfo": { ... } | absent,
      "MyReactions": [ ... ] | absent,
      "ChatInfo": { ... } | absent
    }

`Message` may be wrapped in `{"ServerMessage": {...}}` or be the bare object. Support
both. `MyReactions` is absent (not an empty array) when there are no own reactions.

### 5.9 Message wire shape and normalization

ServerMessage contains:

    ClientMessage:
      Plain: {
        "ChatId": "...",
        "PayloadId": "...",
        "Text": { "MessageText": "..." }   // one of the possible content fields
        // or "Image", "MiscFile", "Voice", "Gallery", "Poll", "Sticker", "Card"
      }
      // or "SystemMessage": { ... }

    ServerMessageInfo:
      "Timestamp": <micros number>,
      "SeqNo": <int>,
      "From": { "Guid": "...", "DisplayName": "..." },
      "LastEditTimestamp": <micros number>,
      "Deleted": <bool>,
      "Version": <int>,
      "PrevTimestamp": <micros number>

Text extraction:

- `Plain.Text.MessageText` -> text;
- `Plain.Voice.Text` -> recognized text (optional);
- `Plain.Gallery.Text` -> caption (optional);
- sticker, image and misc file have no text field on the wire.

Kind detection (from content field):

    Text      -> text
    Sticker   -> sticker
    Image     -> image
    MiscFile  -> file
    Card      -> card
    Gallery   -> gallery
    Voice     -> voice
    Poll      -> poll
    SystemMessage -> system

Deleted overrides kind to `deleted`. Unknown body (no content field) is `unknown`.

Edited is `LastEditTimestamp > 0`. Deleted is `Deleted == true`.

Attachment refs are extracted from content fields, only for reading. `FileInfo` is:

    { "Id2": "<bucket>/<uuid>", "Name": "...", "Size": <int>, "Source": 0|1 }

`Source`: 0 = MDS, 1 = DISK. `Id2` contains a slash and is the value used later for
downloads. Extraction tables:

    Plain.Image.FileInfo            -> image
    Plain.MiscFile.FileInfo         -> file
    Plain.Voice.FileInfo            -> voice
    Plain.Gallery.Items[].Image.FileInfo -> gallery_image

This milestone retains the refs in the model but does not implement download.

### 5.10 Chat wire shape and normalization

    ChatId                 string
    PrivateChatInfo        present for private chats
    PartnerInfo.DisplayName / PublicName  private chat name
    ChatInfo.Name          group chat name
    LastSeqNo               int
    LastSeenByMeSeqNo       int
    LastTsMcs               micros
    Muted                   bool

Private/group detection: private if `PrivateChatInfo` is present, group otherwise.

### 5.11 Thread derivation

A thread is a chat. There is no server side thread creation. A `thread_id` is derived
from the parent chat id and the parent message timestamp (16 digit micros string) by
pure string math:

    private `<guid>_<guid>` -> `110/0/<guid>_<guid>_<timestamp>`
    group   `0/<ns>/<rest>` -> `100/<ns>/<rest>_<timestamp>`
    channel `1/<ns>/<rest>` -> `101/<ns>/<rest>_<timestamp>`

The second int is 100 + parent prefix, parsed in radix 10, not radix 2. Prefixes other
than 0 and 1 (business chats `2/...`) are unsupported and must produce an explicit
exception/result, not silently a broken id.

Reading a thread is a normal `history` call with `ChatId = thread_id` and
`ChatDataFilter = {}`. The parent message arrives inside the chat element as
`ThreadParentMessage`.

An empty (not yet materialized) thread answers `ENTITY_NOT_FOUND(4)`, which for a
thread means "empty", not "access denied".

Regular expressions (Java, case-insensitive where noted):

    private chat: (?i)^[0-9a-f-]{36}_[0-9a-f-]{36}$
    numeric chat: ^(\d+)/(\d+)/(.+)$
    thread id:    ^1(\d\d)/(\d+)/(.+)_(\d{16})$

### 5.12 Private chat id construction

For a private chat between `partnerGuid` and `myGuid`, the chat id is
`<a>_<b>` where a and b are the two guids ordered by ascending UTF-16 code units.

Java `String.compareTo` compares UTF-16 code units lexicographically, so:

    int cmp = partnerGuid.compareTo(myGuid);
    return cmp < 0 ? partnerGuid + "_" + myGuid : myGuid + "_" + partnerGuid;

This must NOT use a locale aware comparator. The order must be locale independent and
must match the server's byte for byte.

### 5.13 Error layers

Three independent layers:

Layer 1, transport (ProxyStatus frame header `[reqId, errorCode]`):

    0 SUCCESS
    1 PROTOCOL_ERROR
    2 BACKEND_CALL_ERROR
    3 INTERNAL_ERROR
    4 CORRUPTED_DATA_HEADER
    5 BACKEND_NOT_FOUND
    6 SERVICE_UNAVAILABLE
    7 TOO_MANY_REQUESTS
    8 FRAME_TOO_LARGE

Layer 2, application (DATA frame with non zero top level `Status`):

    0 SUCCESS
    1 INTERNAL_ERROR
    2 ACCESS_DENIED
    3 MISCONFIGURATION
    4 ENTITY_NOT_FOUND
    5 OVERLOAD
    6 UNAUTHORIZED

Layer 3, push commit status:

    see the table in 5.7.

CRITICAL: `Status` on a push response is a commit status (layer 3), NOT an application
status (layer 2). A `push` with `Status:1` is success, not `INTERNAL_ERROR`. The code
path must distinguish push from other methods.

Unknown codes are rendered as `UNKNOWN(<number>)` and surfaced, never silently dropped.

### 5.14 Timing and reconnect strategy

Reads (`history`, `message_info`, `whoami`) can be retried on a fresh connection after
a short backoff when the failure is transport level and retriable
(SERVICE_UNAVAILABLE, TOO_MANY_REQUESTS on layer 1; OVERLOAD on layer 2). A close with
an auth reason (e.g. `cookie auth failed`, `no credentials`, `bad sign`) has no
automatic recovery in this milestone because the cookie comes from a static config; it
is surfaced as an AuthException.

`push` is NEVER automatically retried.

Race condition to handle: between reading `subscription-id` and sending, the socket may
reconnect and issue a new id. Before sending a push, verify the id is still the current
connection id. If not, do not send and raise a clear error. This is especially
important because push is irreversible.

---

## 6. Configuration

### 6.1 Config file

Default location:

    ~/.config/yandex-messenger/config.json

Overridable by a system property and by an explicit constructor argument. The accepted
system property is `yamb.config` (full path).

JSON format:

    {
      "cookie": "Session_id=...; sessionid2=...; yandexuid=...; yp=...; ys=...; i=...",
      "my_uid": "1234567890",
      "my_guid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    }

Fields:

    cookie   required. Full Cookie header value, name=value pairs separated by "; ".
             Must contain at least Session_id and yandexuid.
    my_uid   required. Numeric Passport uid, decimal string. Used in the WS url `user=`.
    my_guid  required. Own messenger guid. Used for private chat id construction and
             for the "self" check.

Unknown fields are ignored for forward compatibility.

No secret values, guids or uids are ever hardcoded in the library. The config file is
expected to have restrictive permissions (0600), but the library does not enforce it;
it must not log the full cookie string.

### 6.2 Cookie handling

The raw cookie string is used as-is for the WebSocket handshake header.

The library must also extract the named cookie `yandexuid` for push LogData. Helper:
split the cookie string on ';', trim, split each on the first '=', and map by name.

If config is missing or a required field is absent, throw a clear checked exception
or a dedicated ConfigurationException at client construction / explicit load time.

### 6.3 Library does not start a browser

There is no Playwright/Selenium dependency and no browser automation. Cookie refresh
and login are outside this library. The caller is responsible for providing a fresh
cookie string.

---

## 7. Java package and class design

Package root:

    org.luwrain.io.api.ya360

Proposed type layout (implementation may split constants differently, but public API
must remain stable):

    org.luwrain.io.api.ya360.Ya360Client
        Main facade. Thread safe or documented as not thread safe. For this milestone
        a single threaded usage model is acceptable if documented.

    org.luwrain.io.api.ya360.Ya360Config
        Value object + static loader from File/Path with the default path resolution.

    org.luwrain.io.api.ya360.Ya360ConfigException
        Configuration errors.

    org.luwrain.io.api.ya360.Ya360Exception
        Base runtime exception for the library.

    org.luwrain.io.api.ya360.transport.FrameCodec
        encodeDataFrame(serviceIndex, seq, method, byte[] jsonBody) -> byte[]
        decodeFrame(byte[]) -> DecodedFrame

    org.luwrain.io.api.ya360.transport.DecodedFrame
        frameType, List<Object> headerElements, byte[] jsonBody

    org.luwrain.io.api.ya360.transport.FrameType
        int constants DATA=1, PROXY_STATUS=2, PUSH=3

    org.luwrain.io.api.ya360.transport.MessengerWebSocket
        OkHttp WebSocket wrapper: connect, request(method, jsonParams),
        waitForSubscriptionId(), close()

    org.luwrain.io.api.ya360.protocol.ChatResolver
        isChatId, isThreadId, buildPrivateChatId(partner, my), resolve(String ref)

    org.luwrain.io.api.ya360.protocol.ThreadIds
        buildThreadId(chatId, timestamp) / parseThreadId(threadId)

    org.luwrain.io.api.ya360.model.Chat
    org.luwrain.io.api.ya360.model.Message
    org.luwrain.io.api.ya360.model.MessageSender
    org.luwrain.io.api.ya360.model.AttachmentRef
    org.luwrain.io.api.ya360.model.MessageContext
    org.luwrain.io.api.ya360.model.HistoryPage
    org.luwrain.io.api.ya360.model.ThreadHistory
    org.luwrain.io.api.ya360.model.PushResult

Optional internal packages (not public API): `internal.json`, `internal.timestamps`.

### 7.1 Ya360Client public methods

Minimal facade:

    public Ya360Client(Ya360Config config)

    public void close()

    public List<Chat> listChats()
    public List<Chat> listChats(int limit, boolean unreadOnly, boolean includeLastMessageText)

    public HistoryPage getHistory(String chatRef, int limit)
    public HistoryPage getHistory(String chatRef, int limit, String beforeCursor)

    public Message getMessage(String chatRef, String messageId)

    public PushResult sendText(String chatRef, String text)

    public ThreadHistory getThread(String parentChatRef, String parentMessageId, int limit)
    public ThreadHistory getThread(String threadId, int limit, String beforeCursor)

`chatRef` is either:

- a literal ChatId (`<guid>_<guid>`, `0/0/<guid>`, or a thread id); or
- a bare partner guid whose private chat id is built with `buildPrivateChatId`.

`messageId` is a microsecond timestamp string (digits only).

`beforeCursor` is the `nextBefore` returned by the previous history/thread page and is
the microsecond timestamp string of the oldest message on that page.

Connections are established lazily on the first call that needs the socket. `close()`
idempotently closes the underlying socket and cancels timers.

### 7.2 Model field list

Chat:

    String chatId
    String name          (nullable)
    String kind          ("private" | "group")
    String lastActivityIso (nullable)
    String lastActivityMcs (nullable)
    int unreadCount
    boolean unread
    boolean muted
    Message lastMessage  (nullable)

Message:

    String id            // micros string
    String chatId        (nullable)
    String timestampIso
    String timestampMcs
    Integer seqNo        (nullable)
    MessageSender from
    String kind          // text|sticker|image|file|card|gallery|voice|poll|system|deleted|unknown
    String text          (nullable)
    List<AttachmentRef> attachments
    MessageContext context (nullable, minimal milestone reads it but does not feature send)
    boolean edited
    String editedAtIso   (nullable, when edited)
    boolean deleted

MessageSender:

    String guid
    String name (nullable)

AttachmentRef:

    String kind          // image|file|voice|gallery_image
    String fileId        // FileInfo.Id2
    String name (nullable)
    Long size (nullable)
    String source        // "mds" | "disk" | "unknown"
    Integer width/height (nullable, image only)
    Boolean animated (nullable, image only)
    Integer duration (nullable, voice only)

MessageContext:

    boolean reply
    List<MessageRef> refs
    List<String> quotes

MessageRef:

    String chatId (nullable)
    String timestampMcs (nullable)
    String timestampIso (nullable)

HistoryPage:

    String chatId
    List<Message> messages
    String nextBefore (nullable)
    boolean hasMore

ThreadHistory:

    String threadId
    boolean empty
    List<Message> messages
    Message parentMessage (nullable)
    String nextBefore (nullable)
    boolean hasMore

PushResult:

    int status
    String statusName
    boolean committed
    boolean duplicate
    Long timestampMcs (nullable)
    Long seqNo (nullable)
    Long version (nullable)
    Integer rateLimitWaitFor (nullable)

### 7.3 Timestamp handling

All microsecond timestamps are transported into and out of the model as String in
order to preserve exact 16 digit precision (numeric JSON on the wire carries them as a
number; this is currently safe below 2^53 but the library must not force doubles into
the public model).

Provide:

    String microsToIso(String micros)      // ISO-8601 millisecond precision
    String microsToIso(long micros)        // convenience
    long isoToMillis(String iso)           // for future date filters

Wire JSON timestamps are serialized as JSON numbers. Gson `JsonPrimitive(long/long)`
handles this. When reading, prefer `getAsLong()`.

---

## 8. Behavioral requirements

1. First use of the socket opens the WebSocket and waits for the open event.
2. The library must attach the binary/text listeners BEFORE waiting for open so the
   early `subscribed` operation frame is not lost.
3. `subscription-id` is captured from the `subscribed` text frame.
4. Request correlation key is the msgpack header seq. Pending futures are kept in a
   map keyed by seq.
5. When a DATA response arrives, locate the pending request by seq. Optionally verify
   `RequestId` equality when `RequestId` is present.
6. When a PROXY_STATUS frame arrives, reject the pending request with a transport error.
7. For non push methods, a DATA frame with a non zero `Status` is an application error.
   For push it is a commit status and is passed to the push parser.
8. `history` read calls use a request timeout. If the socket is closed or the timeout
   expires, read calls may reconnect once and retry a bounded number of times with
   backoff; close is cleaned up and pending requests are failed.
9. `push` must fail fast and never auto retry.
10. On `close()`, all pending futures are completed exceptionally or failed.
11. `listChats` must sort chats by descending `lastActivityMcs`.
12. `getHistory` `hasMore` is computed from the RAW message count of the chat entry,
    not from the normalized message count (normalization may drop malformed items).
13. `getHistory` `nextBefore` is the timestamp of the first (oldest) message in the
    returned page. If the page is empty, `nextBefore` is absent.
14. Server pages arrive from old to new; preserve that order in the returned list.
15. An empty thread (`application` error code 4 while reading a thread) produces
    `ThreadHistory.empty = true`, not an exception.

### 8.1 Error classes and mapping

Base:

    Ya360Exception extends RuntimeException
      String layer;    // "transport" | "application" | "push" | "auth" | "protocol"
      Integer code;    // nullable for non numeric errors
      String codeName; // e.g. "ENTITY_NOT_FOUND" | "UNKNOWN(42)"
      boolean retriable

Subclasses:

    Ya360AuthException        (rejected cookie, close with auth reason)
    Ya360TransportException   (layer 1)
    Ya360ApplicationException (layer 2)
    Ya360PushException        (layer 3, carries raw commit status)
    Ya360ProtocolException    (undecodable frame, missing Status, malformed response)

The push parser throws Ya360PushException when the commit status is not 1 or 8, and
Ya360ProtocolException when there is no numeric Status.

### 8.2 Logging

Use java.util.logging (JUL) by default; no SLF4J dependency is required for a library
at this size. Log at FINE/INFO:

- connection opened/closed;
- operation frame received (without cookie);
- request method sent (without message text body for push; the text is secret-like and
  must not be logged);
- errors.

Never log the full cookie string, message text, file names of attachments, or other
conversation content.

---

## 9. Gradle build skeleton

Example build.gradle (Groovy DSL):

    plugins {
        id 'java-library'
    }

    group = 'org.luwrain.io.api'
    version = '0.1.0'

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation 'com.squareup.okhttp3:okhttp:4.12.0'
        implementation 'com.google.code.gson:gson:2.11.0'

        testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    tasks.named('test') {
        useJUnitPlatform()
    }

Directory layout:

    src/main/java/org/luwrain/io/api/ya360/...
    src/test/java/org/luwrain/io/api/ya360/...

---

## 10. Testing strategy

Unit tests must not touch the live service. Use synthetic fixtures.

Priority fixture tests:

1. FrameCodec encode known frames byte for byte (whoami/history/push examples).
2. FrameCodec decode collisions: seq=5 must decode correctly (the 0x05 in the header
   must not be mistaken for the data section marker).
3. RequestId generation matches the 8-4-4-8 pattern.
4. Private chat id construction ordering, including `partner < me` and `me < partner`
   and locale independent comparison.
5. Thread id derivation and parse round trip, including rejected `2/...` prefixes.
6. Message normalization from a synthetic `ServerMessage` JSON document.
7. Chat normalization and unread count clamp.
8. Push params construction and payload id presence.
9. PushResult parsing from a synthetic push response.

Optional integration test (opt-in, using a real config file) can verify `whoami`
read only against a live account when the env var `YAMB_LIVE=1` is set. It must never
send anything.

---

## 11. Key risks and notes

- The protocol is unofficial and can change when chats-web is updated. Keep
  protocol constants that are known to rotate in one place consider preserving them in
  config where practical (hosts, service id, user agent).
- The URL service parameter requires exact percent encoding; unit test the raw URL
  string.
- Do not auto retry push. This is a hard invariant.
- Timestamps are 16 digit microsecond values; the public model must use String, not
  double.
- `Status` on push is a commit status, not an application status.
- The frame header must be decoded by MessagePack element lengths, never by scanning
  for the 0x05 marker.
- Cookie strings are sensitive; do not log them.
- OkHttp WebSocket callbacks arrive on OkHttp's own threads. The library must marshal
  user-facing completion safely (e.g. `CompletableFuture`).
- A reconnect after `subscribed` invalidates the old subscription id. Verify before
  push.

---

## 12. Future extension points (not in this milestone)

- Add a `RegistryHttpClient` on top of OkHttp with the same `call(method, params,
  csrf)` contract as the TS client. Add `search`, `get_chats_info`, `get_users_data`
  behind it.
- Add `request_user` + `csrf-token` based identity bootstrap, which makes `my_uid` and
  `my_guid` deriveable and would validate the cookie string.
- Add attachment download using `file_shortterm/{Id2}?attach=true` and the
  bucket/uuid validation rule (segments only `[A-Za-z0-9._-]`, no `.`/`..`, do not
  encode the slash).
- Add enrichment: reads, mentions, reactions (with the reaction map data), forwarded
  messages, `from_me`.
- Add extendable confirm/draft support for send if the library later exposes more
  than plain text.
- Add search based chat resolution by display name.

