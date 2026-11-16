# Transition fixes and frontend workflows — 13 November 2026

Aligned the SRS, system analysis and work packets for four planned transitions.
Bills can be cancelled from UNPAID with a reason. The submitting resident can
close or reopen their resolved complaint. Staff can reject a pending
maintenance request. The booker can cancel a pending or approved booking
before its slot starts. These rules follow the detailed workflow and owning
packets and govern future implementations.

Implemented the P07 React shell and P08 property screens on local igumi.
The browser keeps the bearer in memory and uses the server's first-use flag,
cookie rotation and permissions. Concurrent requests in a tab share one
refresh. Browsers with Web Locks serialize cookie mutations across tabs.
A terminal refresh failure returns to sign-in. Logout retries after a 401
using one fresh bearer for the same user and tenant, and reports when server
revocation could not be confirmed.

The property interface supports block and unit management, manual occupancy,
pagination, filters where the role has access, and the Guard unit directory.
After a lost response or server error, the editor preserves the draft and
requires a successful list reload before it re-enables the write.
The generated client types come from the running backend's OpenAPI contract.
See the [verification record](../06_TESTING_EVIDENCE/2026-11-13_p07_p08_frontend.md).

The remaining audit implementation items belong to P09 user deactivation
and P16 visit consistency. P09 also owns residency-driven occupancy and the
rest of the first-hour setup journey. Hosted CI verification belongs to P06.
