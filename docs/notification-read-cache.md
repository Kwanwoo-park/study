# Notification read-state synchronization

## Flow

- Individual reads, read-all, chat-room reads, and call notification closure register a read watermark in the existing DB transaction.
- Only `afterCommit` updates the current server's SSE cache and publishes the watermark to Redis channel `notification-read-events`. Rollback leaves both unchanged.
- Every application instance subscribes to this channel. Its cached notifications and connected browser tabs receive the same read scope.
- Reconnection reads only local memory, not the notification table. Existing unread-count API calls are unchanged.

## Ordering and scope

The existing `updateTime` (or `registerTime` when absent) is the event revision. No new DB column is required. Keep application server clocks/time zones synchronized, as with the existing notification timestamps.

Read watermarks contain the member ID, optional notification ID or group/URL, and the read cutoff time. Bulk UPDATEs use the same cutoff. Notifications newer than the cutoff remain unread, even if delivered while a read is committing.

The cache keeps at most 100 latest notifications per member, keyed by notification ID rather than a delivery UUID. Older revisions cannot replace newer ones; READ wins over UNREAD at the same revision. Replayed READ entries do not produce popup events. Browser-side ordering/read-watermark checks also guard messages already in flight and delayed list responses.

## Lifetime and failures

Read watermarks survive the last tab disconnecting. Both cached notification revisions and watermarks have a one-hour realtime replay horizon. Events older than that horizon are not shown as new popups; they remain available in the DB-backed notification list. Periodic cleanup removes expired memory state.

Redis publish failures and zero subscribers are logged in integration event logs and retained for retry. `notification.read-state.retry-delay-ms` defaults to 30000; a retry pass handles up to 100 pending scopes and coalesces repeated reads of the same scope. Failed reads do not navigate away on the client.

This is memory-cache synchronization, not durable event storage: pending retries do not survive a process restart, and Redis Pub/Sub cannot acknowledge delivery by every individual application instance. A disconnected subscriber can miss a watermark. Local cache expiry bounds stale replay; strict cross-instance delivery through outages/restarts would require durable messaging or replaying persisted read watermarks. The source of truth remains the notification table.
