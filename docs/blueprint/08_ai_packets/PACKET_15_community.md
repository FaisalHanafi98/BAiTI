# P15 — Community Module (announcements, events) (v1.x)

**Objective**: One-to-many communication: targeted announcements with attachments, and an events listing.

**Scope**: announcements CRUD + audience targeting (FR-COM-01); events CRUD (FR-COM-02); resident feed + staff composer screens. Out: comments/reactions, RSVPs, push/email fan-out (channel interface only).

**Dependencies**: P18 epic gate — technically P09 (+P10's `StorageService` for attachments). **Requirements**: FR-COM-01/02 · matrix rows `ANNOUNCEMENT_*`, `EVENT_*`.

**Files** — backend `my.baiti.community/`: `AnnouncementController` (`/api/announcements`), `EventController` (`/api/events`), services, DTOs, `CommunityModuleIT`. Frontend: `pages/community/{FeedPage,ComposeAnnouncementPage,EventsPage,ManageEventsPage}.tsx` + hooks/specs.

**Architecture deltas**: audience filter is a server-side predicate from the caller's residency type — `ALL`, or `OWNERS`/`TENANTS` matching `resident_type` (staff see everything); enforced in the query, not the UI. Attachment via `StorageService` (5 MB, jpg/png/pdf — same rules as P10). Events validation `end_date ≥ start_date` (DB CHECK + Bean Validation); feed sorted `posted_at desc`, events by `start_date` with past/upcoming split.

**Testing**: README §3 set + audience-targeting proofs (owner sees OWNERS, tenant-occupant doesn't; and vice versa) + attachment rules + date validation.

**DoD**: README §3 module DoD + browser evidence: targeted announcement visible to the right resident and invisible to the other type.
