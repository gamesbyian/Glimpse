# Glimpse: everyday app experience and support-feature specification

**2026-10-23 · Proposed product additions, not implemented features.** These are missing or thinly specified common app responsibilities outside original user workflows W01–W70 and F071–F073. They extend those workflows; their separate identifiers `S01–S16` must never be confused with the original feature IDs.

**Intent:** a quiet, trustworthy, local-first wallpaper companion with helpful controls available when needed. No account requirement, online dependency, intrusive review nag, hidden background upload or faux customer-service infrastructure. Keep the paper-reel metaphor where it helps people choose **photographs**. Ordinary help, legal text, crash reports and settings use readable native sheets/lists.

## Release-tier vocabulary

- **M1**: necessary for a credible first independently usable sideload release, even if simply implemented.
- **M2**: daily-driver polish or packaging readiness; should be specified and drawn now, not necessarily built immediately.
- **M3**: distribution-specific integration or optional future enhancement; design and feasibility gates first.
- **M4**: ideas to preserve without speculative implementation commitments.

### Proposed new experience requirements

| ID | Surface / human job | M | Required default and state variation |
| --- | --- | --- | --- |
| **S01** | **About Glimpse**: identify the installed app, its purpose, developer, version/build and release channel | M1 | Settings → About; version name/code, Build SHA if known, debug/preview/release qualifier, privacy, licences, source/site and help links; offline read-only; unavailable links plainly labelled |
| **S02** | **Help / How it works**: discover how to import, browse, preview, set, shuffle and recover | M1 | Task-first help cards, not dense pitch prose; explain Reel versus Grid and *preview ≠ applied*; offline core FAQ, Android-specific limits, search/jump within help if it becomes long; help sheet and expanded article |
| **S03** | **Contact and feedback**: contact developer without creating a Glimpse identity | M1 | About/Help → Contact; choose email/Android share or public GitHub issues (GitHub account may be required). Never invent a contact email or promise a support SLA; no-handler fallback with Copy message; feature suggestion variant |
| **S04** | **Report a problem**: describe a reproducible problem | M1 | Text input, optional steps, category, optional sanitized diagnostics; preview payload and edit before Android Sharesheet; never include personal images, file paths, photo titles or location by default |
| **S05** | **Unusual-exit recovery**: learn that the prior session ended unexpectedly without losing work | M1 | After an identifiable prior crash/process exit (where Android exposes reliable reasons), a **non-blocking** notice or relevant repair prompt on next launch; don't mistake normal OS process eviction for a crash; Recovery/Details/Not now; suppression and accessible alternative |
| **S06** | **Diagnostic report preview**: inspect and share redacted technical context | M1 | Explicit Generate locally → preview categories/content → optionally remove/redact/edit → Send with Android chooser, Save, Copy or Cancel; **off-device reporting is never automatic**; sensitive values default excluded |
| **S07** | **What's new**: know what changed after installing an update | M2 | Settings/About → What's new, local changelog tied to installed version; optional once-per-major-update unobtrusive entrypoint, not modal forced before wallpaper use; offline |
| **S08** | **Updates and distribution**: understand whether and how to update | M1 | About → Check for updates; sideload: open trusted releases page if enabled and require user-driven install; Play-distributed: use Play-supported mechanisms only when eligible; don't imply in-app update works for debug/sideload APKs |
| **S09** | **Rate and review**: offer a review at a genuinely appropriate time | M3 / Play only | No prompt or rating action in sideload/preview build. A plainly labelled **Rate on Google Play** link can open a real published listing; optional Play In-App Review may be requested sparingly after genuine repeated success, under Play quotas; never star-gate feedback, force a rating, or build a button that calls the in-app review API |
| **S10** | **Privacy and data practices**: what is kept, exported and shared | M1 | Offline Privacy screen/policy; explain local photographs/metadata, picker permissions, no proprietary cloud/no background telemetry, support report consent and external Android app handoffs; if data practices change, revisit all statements and Play Data safety |
| **S11** | **Storage and reset**: inspect storage and deliberately reclaim space | M1 | Local bytes/photograph count, inspect/remove imported copies only with membership awareness, **Clear cached previews** separate from **Delete imported photos and collection data**; confirmations, backup advisory, estimated consequences, progress and low-storage failure |
| **S12** | **Permissions and capabilities**: understand system restrictions | M1 | Status and explanation for Photo Picker/provider availability, set-wallpaper target, Android power restrictions, notifications if introduced, Home/Lock capability. Use scoped system intents; don't present non-requestable policy limits as grantable permissions |
| **S13** | **Credits, licences, and legal**: accurate attribution and third-party notices | M2 / before public release | About → Licences & Credits; bundled source notices, image rights, app licence, open-source dependencies, source provider credits and versions; never mark unchosen app licence as selected |
| **S14** | **Release/testing feedback**: contribute useful test observations | M2 / opt-in | Preview/beta-only “Help improve Glimpse” entry; screenshot or state annotations chosen by user; no behavioral surveillance; show exactly what a submission contains; public issues versus private share channel clearly distinguished |
| **S15** | **Notification preferences**: only notify when an actionable event merits it | M2 | Per-class opt-in notification switches and platform permission rationale; no notification permission unless real notifications implemented; quiet errors accessible through Activity/Diagnostics independently |
| **S16** | **Accountless support and external-handler fallback** | M1 | Every external link has an unavailable-browser/email/share fallback. Copy issue template/URL, save report locally, or return to Help; no “sent” acknowledgment merely because a chooser opened |

### Local-first crash capture design

Default **none of the following is uploaded automatically**. Record bounded redacted error metadata on-device only when necessary to recover/reproduce a fault, with an explicit retention limit and a way to clear it. Suggested technical data: installed version/build SHA, Android API level, device model class if user approves, operation state and timestamps, an exception classification, and a sanitized stack trace (after path/URI/provider/title scrub). Exclude imported filenames, content URIs, image bytes/previews, contact details, account tokens, clipboard, freeform app history, and exact location by default. Treat unusual exit indicators as *possible* rather than definitive crash evidence; the Android `ActivityManager.getHistoricalProcessExitReasons` API (API 30+) exposes a bounded history, and not every exit means a crash. Surface review before export, a cancellation route, and expiry. Avoid global exception-handler code that changes native crash semantics unless justified by tests.

A crash prompt is not a suitable reward/punishment loop. **One restrained notification after a verified relevant crash**, with an option to dismiss/turn off, is the proposed behavior; no prompt after every wallpaper apply or every ordinary Android app exit. The user may open diagnostics manually at any time.

### Contact and support channel truth

A public issues page exists at `https://github.com/gamesbyian/Glimpse/issues`. Opening GitHub can require a GitHub account to submit; copy/share a report is always an independent route. **No contact email address has been verified or authorized**, so render Email only when actually configured. “Send report” means opening Android's chooser with an explicitly scoped URI; external completion cannot be known. Never publish an automatically generated report as a public GitHub issue.

### Reviews and updates: channel-specific contracts

- **Debug/sideload**: show build/version and safe manual update path; **no fake Google Play listing, Play In-App Review or Play update flow**.
- **Play listing exists, Play-installed build**: static rate-link only to its verified package listing; optional infrequent Play In-App Review request at a neutral, non-task-blocking milestone. Google Play may suppress the dialog, so the app **cannot infer a review was shown or submitted**.
- **Future alternative stores**: use channel-appropriate links/installers only after compatibility and policy proof. Feedback remains independent of rating.
- **No fake update checks**: if checking release metadata requires network, label unavailability and perform only on explicit request unless the user opts into automatic checks.

### Priorities and placement

**Normal top-level navigation remains exactly four destinations**: Home, Discover, Collections, Automation. None of the new support features deserves a fifth tab, a second reel or a persistent large Home card. Place Settings from a stable accessible menu; Settings owns About, Privacy, Storage, Permissions, Accessibility, and Notifications. About owns What's New, Credits/Licences, version/build, Help, Contact and Review when eligible. Contextual error messages offer Report problem and Help without redirecting the primary photo action.

**M1 design references** must include About, Help, Contact, Report problem, Crash recovery, Diagnostic preview, Updates, Privacy, Storage, Permissions, external-handler fallback, and their offline/empty/denied alternatives. **M2 references** add What's New, credits/licences, beta-feedback, notification settings. **M3** includes Play review with a no-Play variant. Color, typography, dialog padding and back/dismiss semantics must match the primary Glimpse visual language while the special reel artwork stays out of ordinary support controls.

### Navigation contract and recoverability

- Deep links from Home or a failure status to a detail screen return to the original screen and selected photo ID; no changed wallpaper unless explicitly requested.
- Dismissal of About, Help or a recovery notice never silently starts/restarts rotation.
- Persist drafts only locally if feasible and useful; draft report deletion must be possible without network.
- Confirm destructive storage/reset actions with specific collection/item counts; protect a working background rotation from dangling deleted image refs.
- Error reporting UI must have an accessible “Copy details” action independent of external handlers.

### Variations to preserve but not prematurely build

Opt-in performance diary for advanced debugging; local-only export of crash history and selected image provenance hashes; a structured user-feedback short survey following an **explicitly voluntary** support action; maintained compatibility matrix; localized help and typography; one-click GitHub prefilled bug template after warning about public submission; QR linking to a local report; Play release changelog; an alternate store and F-Droid distributions; signed release update verification; shareable reproducible non-sensitive diagnostic bundle. **None** implies mandatory telemetry, implicit reporting or a proprietary Glimpse cloud.

### Definition of done

Each new S-feature needs a route, primary action, failure/absence state, Compose semantics/keyboard/TalkBack alternative, source-versus-device side effect, privacy contract, pixel proposal and evidence classification. A picture alone is not completion. For every external channel, test handler-present and handler-absent; for crash reporting, inspect actual bytes that would leave the phone.

## Upstream policy references

- Google Play [In-App Reviews API](https://developer.android.com/guide/playcore/in-app-review): quotas and prohibition on a user-triggered button expecting the prompt.
- Google Play [In-App Updates](https://developer.android.com/guide/playcore/in-app-updates): Play-specific eligibility.
- Android [ActivityManager process-exit API](https://developer.android.com/reference/android/app/ActivityManager#getHistoricalProcessExitReasons(java.lang.String,int,int)): bounded prior exit history.
- Android [common intents](https://developer.android.com/guide/components/intents-common): email/sharing, handler availability.
- Google Play [Data safety disclosures](https://support.google.com/googleplay/android-developer/answer/10787469): crash/diagnostic collection, handling disclosures and privacy-policy requirements for Play distribution.

These are *plans*, not claims that Glimpse currently has a Play listing or the listed features.
