# Java Library Implementation Progress: Yandex Messenger API Client (ya360)

Status: in progress, milestone 1 (minimal vertical slice)
Source spec: ya360-java-library.spec
Language: Java 17
Build system: Gradle (Groovy DSL)
Package root: org.luwrain.io.api.ya360

This file records the current implementation progress and the planned next steps.
It is a working document, not a replacement for ya360-java-library.spec.

---

## 1. Project layout

The Java project lives under:

    java/

Files created so far:

    java/settings.gradle
    java/build.gradle
    java/.gitignore

    java/src/main/java/org/luwrain/io/api/ya360/Ya360Config.java
    java/src/main/java/org/luwrain/io/api/ya360/Ya360ConfigException.java
    java/src/main/java/org/luwrain/io/api/ya360/Ya360Exception.java
    java/src/main/java/org/luwrain/io/api/ya360/Ya360TransportException.java
    java/src/main/java/org/luwrain/io/api/ya360/Ya360ApplicationException.java
    java/src/main/java/org/luwrain/io/api/ya360/Ya360PushException.java
    java/src/main/java/org/luwrain/io/api/ya360/Ya360AuthException.java
    java/src/main/java/org/luwrain/io/api/ya360/Ya360ProtocolException.java

    java/src/main/java/org/luwrain/io/api/ya360/protocol/IdUtil.java
    java/src/main/java/org/luwrain/io/api/ya360/protocol/ThreadIds.java
    java/src/main/java/org/luwrain/io/api/ya360/protocol/ChatResolver.java
    java/src/main/java/org/luwrain/io/api/ya360/protocol/TransportErrorCodes.java
    java/src/main/java/org/luwrain/io/api/ya360/protocol/AppErrorCodes.java
    java/src/main/java/org/luwrain/io/api/ya360/protocol/PushStatusCodes.java

    java/src/main/java/org/luwrain/io/api/ya360/transport/FrameType.java
    java/src/main/java/org/luwrain/io/api/ya360/transport/DecodedFrame.java
    java/src/main/java/org/luwrain/io/api/ya360/transport/FrameCodec.java

    java/src/main/java/org/luwrain/io/api/ya360/model/MessageSender.java
    java/src/main/java/org/luwrain/io/api/ya360/model/AttachmentRef.java
    java/src/main/java/org/luwrain/io/api/ya360/model/MessageRef.java
    java/src/main/java/org/luwrain/io/api/ya360/model/MessageContext.java
    java/src/main/java/org/luwrain/io/api/ya360/model/Message.java
    java/src/main/java/org/luwrain/io/api/ya360/model/Chat.java
    java/src/main/java/org/luwrain/io/api/ya360/model/HistoryPage.java
    java/src/main/java/org/luwrain/io/api/ya360/model/ThreadHistory.java
    java/src/main/java/org/luwrain/io/api/ya360/model/PushResult.java

    java/src/main/java/org/luwrain/io/api/ya360/internal/timestamps/Timestamps.java
    java/src/main/java/org/luwrain/io/api/ya360/internal/json/JsonExt.java
    java/src/main/java/org/luwrain/io/api/ya360/internal/json/MessageNormalizer.java
    java/src/main/java/org/luwrain/io/api/ya360/internal/json/ChatNormalizer.java

    java/src/test/java/org/luwrain/io/api/ya360/transport/FrameCodecTest.java
    java/src/test/java/org/luwrain/io/api/ya360/protocol/ThreadIdsTest.java
    java/src/test/java/org/luwrain/io/api/ya360/protocol/ChatResolverTest.java
    java/src/test/java/org/luwrain/io/api/ya360/protocol/PushStatusCodesTest.java
    java/src/test/java/org/luwrain/io/api/ya360/internal/json/MessageNormalizerTest.java
    java/src/test/java/org/luwrain/io/api/ya360/internal/json/ChatNormalizerTest.java
    java/src/test/java/org/luwrain/io/api/ya360/internal/timestamps/TimestampsTest.java
    java/src/test/java/org/luwrain/io/api/ya360/Ya360ConfigTest.java

---

## 2. Build configuration

Gradle build file: java/build.gradle

Decisions applied:

- Plugin: java-library
- Group: org.luwrain.io.api
- Version: 0.1.0
- Java toolchain: 17
- Dependencies:
    - com.squareup.okhttp3:okhttp:4.12.0 (implementation)
    - com.google.code.gson:gson:2.11.0 (implementation)
    - org.junit.jupiter:junit-jupiter:5.10.2 (testImplementation)
- Test platform: JUnit Platform

No build was executed yet. Compilation must be verified later.

---

## 3. Implemented components

### 3.1 Configuration

Ya360Config:

- Constructor accepts cookie, my_uid, my_guid.
- Loads from a Path using Gson.
- Default path: ~/.config/yandex-messenger/config.json
- Path can be overridden with system property yamb.config.
- Parses the cookie header into name/value pairs.
- Requires Session_id and yandexuid cookie values.
- Unknown JSON fields are ignored.
- Provides cookie(), myUid(), myGuid(), yandexUid(), cookieValue(name).

Ya360ConfigException is a checked exception.

### 3.2 Error hierarchy

Base class Ya360Exception extends RuntimeException and stores:

- layer
- code
- codeName
- retriable

Subclasses:

- Ya360TransportException (layer = "transport")
- Ya360ApplicationException (layer = "application")
- Ya360PushException (layer = "push")
- Ya360AuthException (layer = "auth")
- Ya360ProtocolException (layer = "protocol")

The push exception exposes status().

### 3.3 Protocol helpers

IdUtil (package private):

- Generates xiva session token xxxx-xxxx-xxxx-xxxx from 8 random bytes.
- Generates RequestId 8-4-4-8 from 12 random bytes.
- Uses SecureRandom.

ThreadIds:

- isPrivateChatId, isNumericChatId, isThreadId.
- buildThreadId(chatId, timestampMcs).
- parentChatId(threadId).
- Rejects unsupported numeric chat prefix 2/... for thread derivation.

ChatResolver:

- isChatId.
- buildPrivateChatId using String.compareTo (UTF-16 code unit order).
- resolve(ref, myGuid) returning literal ids unchanged and bare guids as private ids.

Error code tables:

- TransportErrorCodes with names and retriable classification.
- AppErrorCodes with names and retriable classification.
- PushStatusCodes with names, success() and duplicate() helpers.

### 3.4 Frame codec

FrameType constants:

- DATA = 1
- PROXY_STATUS = 2
- PUSH = 3

FrameCodec:

- encodeDataFrame(serviceIndex, seq, method, jsonBody).
- decodeFrame(byte[]).
- Writes the fixarray header 0x93 plus uint/string elements.
- Writes a 12 byte data section starting with 0x05.
- Decodes by advancing MessagePack element lengths, not by searching for 0x05.
- Handles positive fixint, uint8, uint16, uint32.
- Handles fixstr, str8, str16.
- Returns DecodedFrame with frameType, headerElements, jsonBody.

Known decoder detail: the next offset is derived from the actual wire tag, so
non-canonical uint encodings are still advanced correctly.

### 3.5 Models

Created immutable value objects:

- Chat
- Message
- MessageSender
- AttachmentRef
- MessageContext
- MessageRef
- HistoryPage
- ThreadHistory
- PushResult

### 3.6 Timestamps

Timestamps utility:

- microsToIso(String)
- microsToIso(long)
- isoToMillis(String)

The public model stores timestamps as strings.

### 3.7 Normalizers

JsonExt:

- Safe Gson getters for strings, ints, longs, booleans, objects, object lists.

MessageNormalizer:

- Normalizes ServerMessage -> Message.
- Detects kind: text, sticker, image, file, card, gallery, voice, poll, system,
  deleted, unknown.
- Extracts text from Plain.Text, Voice.Text, Gallery.Text.
- Extracts attachment refs for image, file, voice, gallery_image.
- Maps FileInfo.Source 0 to mds and 1 to disk.
- Computes edited from LastEditTimestamp > 0.
- Applies deleted override to kind.

ChatNormalizer:

- Normalizes a history response chat element -> Chat.
- Detects private vs group.
- Resolves name from PartnerInfo or ChatInfo.
- Computes unread count as max(0, LastSeqNo - LastSeenByMeSeqNo).
- Reads the first Messages element as the last message.

Context normalization is not yet implemented; the minimal milestone does not require
send-by-context, and reading context is optional.

---

## 4. Tests written so far

FrameCodecTest:

- Encodes whoami byte for byte.
- Decodes seq=5 without confusing the data section marker.
- Encodes a seq above 127 as uint16.
- Round trips ProxyStatus.
- Round trips a frame with a longer header element.

ThreadIdsTest:

- Private thread derivation and parse.
- Group thread derivation and parse.
- Channel thread derivation and parse.
- Rejects unsupported 2/... prefix.
- Thread id detection.

ChatResolverTest:

- Partner-before-me ordering.
- Me-before-partner ordering.
- Bare guid resolution.
- Literal thread id resolution.

PushStatusCodesTest:

- Success and duplicate detection.
- Error code names including UNKNOWN(...) fallback.

MessageNormalizerTest:

- Text message normalization.
- Deleted and edited normalization.
- Image attachment normalization.
- Voice text and disk source normalization.
- Unknown kind when no content.

ChatNormalizerTest:

- Private chat normalization with unread clamp.
- Group chat normalization with unread count and last message.

Ya360ConfigTest:

- Cookie parsing and yandexuid extraction.
- Rejects missing Session_id.
- Rejects missing yandexuid.

TimestampsTest:

- micros to ISO conversion.
- Rejects invalid micros.
- ISO to millis conversion.

---

## 5. Remaining work

### 5.1 Transport layer

Implement MessengerWebSocket using OkHttp:

- Build the xiva URL with the exact percent-encoded service value.
- Generate session and attach user=my_uid.
- Attach Cookie and Origin headers.
- Attach listeners before waiting for open.
- Capture subscription-id from the subscribed text frame.
- Capture server-interval-sec from ping text frames.
- Maintain a request sequence counter starting at 1.
- Correlate responses by header seq.
- Optionally verify RequestId equality.
- Handle ProxyStatus by rejecting the pending request.
- Distinguish push responses from other application responses.
- Fail pending futures on close.
- Implement read retry/reconnect with bounded backoff.
- Implement push fail-fast without auto retry.
- Marshal OkHttp callbacks onto CompletableFuture.

### 5.2 Request building and response parsing

Implement internal JSON builders/parsers for:

- whoami (needs confirmation against the TypeScript reference source).
- history with list chats and pagination.
- message_info.
- push plain text.

These may live under internal/json or a small internal request package.

### 5.3 Public facade

Implement Ya360Client with the public methods from ya360-java-library.spec section 7.1:

- close()
- listChats()
- listChats(limit, unreadOnly, includeLastMessageText)
- getHistory(chatRef, limit)
- getHistory(chatRef, limit, beforeCursor)
- getMessage(chatRef, messageId)
- sendText(chatRef, text)
- getThread(parentChatRef, parentMessageId, limit)
- getThread(threadId, limit, beforeCursor)

Document the threading model (single threaded usage is acceptable for this milestone).

### 5.4 Thread empty handling

Map application error code 4 (ENTITY_NOT_FOUND) while reading a thread into
ThreadHistory.empty = true instead of an exception.

### 5.5 Logging

Add java.util.logging (JUL) logging at FINE/INFO for connection, operation frames,
request methods and errors. Never log cookie, message text, or attachment names.

### 5.6 Verification

Run the Gradle build and tests.

### 5.7 Optional opt-in live test

Add an opt-in read-only whoami test guarded by YAMB_LIVE=1. It must never send.

---

## 6. Open questions

1. Exact whoami request JSON and response fields are not specified in
   ya360-java-library.spec. Confirm against ts/src (reference TypeScript MCP server)
   or ts/protocol-research.md before implementing whoami.
2. Whether an explicit subscribe method must be sent over the WebSocket is not fully
   specified. Confirm against the TypeScript reference implementation.
3. Exact numeric timeout and reconnect constants are not specified. Use centralized
   constants until a decision is made.

---

## 7. Notes

- No build has been run yet, as requested during the initial implementation pass.
- The library remains form-factor library only: no CLI, no server.
- Out-of-scope features from ya360-java-library.spec section 3 must not leak into the
  public API.

