# Vocabulary Sync & Online Tracking Guidelines for Backend

This document outlines the API design, database schemas, and synchronization rules for integrating online vocabulary tracking with the client app's offline tracker.

---

## 1. Overview of Synchronization Strategy

The mobile client stores vocabulary progress locally in a local cache file (`vocab_progress.json`). To enable cross-device synchronization and web platform availability, the backend needs to store, update, and merge progress records.

A **Modified-Timestamp-based Conflict Resolution (Last-Write-Wins)** strategy is recommended.

---

## 2. Database Schema Recommendations

The backend should maintain a table `user_vocab_progress` to track progress at a granular level.

### Table: `user_vocab_progress`

| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique record identifier. |
| `user_id` | UUID | FOREIGN KEY, INDEX | Identifies the user. |
| `word` | VARCHAR(100) | INDEX | The vocabulary word. |
| `mastery_score` | INT | DEFAULT 0, CHECK (0-100) | Current mastery level (0% to 100%). |
| `is_bookmarked` | BOOLEAN | DEFAULT FALSE | If the user starred/bookmarked this word. |
| `last_reviewed` | TIMESTAMP | DEFAULT NOW() | When the word was last tested. |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Timestamp for conflict resolution check. |

*Unique constraint: `(user_id, word)` should be unique.*

---

## 3. Sync API Specifications

We propose a single synchronization endpoint `/api/v1/vocab/sync` supporting two modes:
1. **GET**: Retrieve all vocabulary progress modified since a given timestamp.
2. **POST**: Submit local progress changes to the server to merge.

### 3.1. Fetch Server Updates (GET)
Used by the client during app startup or when manually syncing.

- **Endpoint**: `/api/v1/vocab/sync`
- **Method**: `GET`
- **Query Parameters**:
  - `last_sync_time` (Optional ISO 8601 Timestamp): If provided, returns only records updated on the server *after* this time.
- **Headers**:
  - `Authorization: Bearer <token>`
- **Response**: `200 OK`
  ```json
  {
    "server_time": "2026-06-15T19:00:00Z",
    "updates": [
      {
        "word": "aggregate",
        "mastery_score": 60,
        "is_bookmarked": true,
        "last_reviewed": "2026-06-15T18:45:00Z",
        "updated_at": "2026-06-15T18:45:00Z"
      },
      {
        "word": "substantiate",
        "mastery_score": 40,
        "is_bookmarked": false,
        "last_reviewed": "2026-06-15T17:30:00Z",
        "updated_at": "2026-06-15T17:30:00Z"
      }
    ]
  }
  ```

---

### 3.2. Upload Client Updates (POST)
Used by the client to push local changes to the server.

- **Endpoint**: `/api/v1/vocab/sync`
- **Method**: `POST`
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "client_time": "2026-06-15T19:05:00Z",
    "changes": [
      {
        "word": "aggregate",
        "mastery_score": 80,
        "is_bookmarked": true,
        "last_reviewed": "2026-06-15T19:01:00Z",
        "updated_at": "2026-06-15T19:01:00Z"
      }
    ]
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "status": "success",
    "merged_count": 1,
    "conflicts_resolved": 0
  }
  ```

---

## 4. Conflict Resolution Rules (Server Side)

When a client submits changes via `POST /api/v1/vocab/sync`, the server must merge them:

1. **New Word**: If the word record does not exist for the user, insert the new record.
2. **Existing Word**: If the word record exists, compare the incoming `updated_at` (or `last_reviewed`) with the server's database `updated_at`:
   - **Case A (Client is Newer)**: If `client.updated_at > server.updated_at`, update the database record with the client's values.
   - **Case B (Server is Newer)**: If `server.updated_at > client.updated_at`, ignore the client's update for this word. (It will be sent down to the client on the next `GET` sync).
   - **Case C (Mastery Accumulation Rule - Optional)**: Alternatively, the server can choose to take the **highest mastery score** (`MAX(client.mastery_score, server.mastery_score)`) and union the bookmark boolean (`client.is_bookmarked OR server.is_bookmarked`) to avoid accidental progress loss.

---

## 5. Security & Rate Limiting

- **Authorization**: All sync endpoints must require valid user sessions/JWTs.
- **Bulk Limit**: Batch sync requests should be capped at 500 records per API call to prevent database locks and latency spikes.
