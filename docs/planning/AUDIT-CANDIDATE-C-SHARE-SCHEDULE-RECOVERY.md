# Audit candidate C: R06–R08 command, recovery and visual continuity contract

**Status:** DRAFT specification amendment, 2026-10-23. Not owner-approved, not native-verified. Applies to the current PR #6 visual reference only. Preserve audit cycle A at **18 source PASS, 8 ISSUE, 34 UNVERIFIED**. No score changes from this desk reconciliation.

## Provenance and existing drawings (reuse before adding artwork)

- R06: `design/proposals/audit-revisions/r02-crop-pending.svg`, `r02-copy-saved.svg`, `r02-share-choice.svg`, `r02-return-dismiss.svg`; `design/proposals/advanced-storyboards/x13/02-edited.svg`. These already illustrate selection and a return. They do **not** establish a delivery acknowledgement or a real Android Sharesheet.
- R07: `design/proposals/storyboards/x04/05-paused.svg`, `design/proposals/state-refinements/automation-delayed.svg`, and the X04 rotation sequence. Paused/delayed already have phone-side states; do not redraw identical states as supposedly missing.
- R08: `design/proposals/state-refinements/source-detail-offline.svg`, `backup-and-restore-restore.svg`, `destinations-unsupported.svg`, `error-empty-permission-storage.svg`, `settings-permissions.svg`. Source-offline and restore already have distinct illustrations. Remaining missing distinctions are *transitions, actions and platform evidence*.
- Architectural authority: `docs/architecture/behavior-contracts.md`; approved intent: `docs/planning/current-decisions.md`; implementation status: `docs/planning/implementation-queue.md`; originating audit: `docs/design/glimpse-ux-audit-cycle-a.v1.json`.

## R06: share/copy and external handoff (GUX-15, 16, 17, 18)

**User goal:** Send either exact original `p12` bytes or an explicitly saved edited derivative `p12c`, return to the same browsed image, and know what did and did not happen.

**Normative state and commands:** `Browse(p12)` → `EditDraft(p12, recipe)` → explicit `SaveRecipe` acknowledgement → `VariantSaved(p12c, parent=p12)`; or abandon draft with p12 immutable. `ChooseSharePayload(original|edited)` must bind a concrete immutable asset/revision/byte hash. `PrepareExport` allocates an ephemeral content URI; `LaunchSharesheet` records only a handoff attempt; `ReturnFromExternal` observes activity lifecycle only, even if a result code exists. No external recipient-delivery state can be inferred. Selection/rotation/last-confirmed wallpaper do not change.

**Visible action and feedback:** show two labeled choices, “Original photo” and “Edited copy”, with visually distinct crops, revision information and the outgoing-file preview; primary action “Open Android sharing”. Preparing, unavailable-handler, dismissed/returned and temporary-export failure each use different status labels. After return, say “Back in Glimpse; delivery is not confirmed”, never “Sent”.

**Recoverability:** Back before Share retains the saved copy, and cancels a disposable export; share failure leaves p12/p12c unchanged and offers Try again or Save a copy. Expired provider grants require restaging from the verified local copy, or a visible relink route. On crash restore persisted saved variant, not a phantom in-progress recipient. URI grants are temporary, read-only and scoped to the selected payload; never log the payload or a private path.

**Accessibility/motion:** original/edited radio options have unique labels and selected states; preview descriptions identify the crop and photo ID; keyboard/TalkBack order = title → payload selection → payload preview → Share/Back → status. Motion-free return route; 48dp-class targets, 200% text and screen-inset review required.

**Before/after proof:** compare existing X13 `02-edited.svg` with `r02-share-choice.svg` and `r02-return-dismiss.svg` at identical 401×890 crop. These are presently draft-source comparisons, **not Android Screensheet captures**. A dedicated “return after picker dismissal” illustration is justified only if the current r02 return lacks separate cancelled/not-delivered feedback after optical inspection.

**Acceptance:** original SHA-256 invariant before/after crop/save/share, persisted p12c version identity, no extra wallpaper gateway invocation, selected browse ID stable across external activity, robust no-handler and grant-expiry paths; on-device verify actual Android Sharesheet choice/dismiss/return, content URI read and accessible announcements. Regression must reject any `recipientConfirmed:true` claim without a verified integration that can establish it.

## R07: rotation eligibility and scheduling truth (GUX-24–29)

**User goal:** Know whether rotation is enabled, whether a photo can change now, when the next attempt is eligible, and why it did not change at the requested cadence.

**Separate fields:** `configuredInterval` (e.g., preferred 5 min), `rotationEnabled`, `pausedByUser`, `eligibleAssetCount`, `nextEligibilityNotBefore`, `lastWorkerInvocation`, `lastAttempt`, `lastAcknowledgedByGlimpse`, `schedulerRestriction` and `outcomeConfidence`. A worker invocation is neither a successful attempt nor a visually confirmed wallpaper.

**Derived visible states:** WAITING = enabled, eligible items exist, due time in future; ELIGIBLE = policy allows a change, **not** promise of invocation; PAUSED = explicit reversible user flag; DELAYED = eligibility passed without completed acknowledgement; RESTRICTED = evidence of relevant battery/background policy; UNKNOWN = missing, interrupted or ambiguous outcome; RESUMED = pause cleared and future eligibility re-evaluated. Distinguish zero eligible items from Android delay.

**Commands:** `Pause` and `Resume` mutate only scheduling intention/eligibility. `ChangeNow` is a separate foreground command and must respect per-target gateway and safety gates. `Retry` is not permitted to blindly repeat an UNKNOWN post-OS-call outcome. Resume never executes a backlog burst. A 5-minute configured interval remains best-effort; do not promise exact Android wake-ups.

**Visible controls:** show Enabled/Paused, eligible item count, target(s), “Next eligible after …” (not “Will change at …”), last confirmed Glimpse attempt with target, “Change now”, “Pause/Resume”, “Why delayed?” and reachable battery/settings help when restriction is detected. Unknown needs “Check activity” rather than a success banner. Last applied must not be overwritten by a queued candidate.

**Accessibility/motion:** status text independent of color; full timestamp and relative duration exposed; 200% font supports multiline status and controls; reduced-motion equivalent replaces spinner/animated countdown with status text.

**Before/after:** reuse X04 pause and existing `automation-delayed.svg` as reference; inspect X04 active/eligible states first. Target genuine missing distinguishing screenshots for battery-restricted versus simply delayed, and UNKNOWN after platform call. Native proof must include reboot, process death, battery saver, Doze/OEM condition changes, and at least one deliberately delayed eligible window; do not generate synthetic device measurements.

**Acceptance:** pure state reducer fixture covers all seven states, never labels eligibility as acknowledged apply, queue cursor only commits on acknowledged success, missed windows do not catch up in bursts, per-target result isolation. Native telemetry captures policy version, planned eligible, actual invocation, attempted and acknowledged timestamps with redacted identifiers.

## R08: offline sources, revoked access, storage and restore (GUX-41, 43–47, 49, 50)

**User goal:** Continue using verified local photos when a provider disappears, repair missing permissions or storage without losing originals or silently overwriting choices, and return to the prior browsing context.

**Source distinction:** OFFLINE means provider temporarily unreachable, REVOKED means access authorization lost, MISSING means formerly referenced remote content unavailable, but VERIFIED LOCAL COPY remains usable until independently invalidated. No automatic broad permission request. Surface each as separate cause and recommended action. A failed external fetch must not demote verified local bytes or discard membership.

**Storage:** before importing or restoring, compute required and available bytes conservatively; stage to temp, decode/hash, promote atomically, and maintain durable committed-item counts. On ENOSPC: show confirmed retained items versus uncommitted items, choices “Keep imported”, “Free space and retry”, “Cancel pending”; never count a staged partial file as saved or delete the provider original. Reconcile partial journal after process death.

**Restore:** `ReviewRestore` identifies exact SHA duplicates, same-ID/different-byte collisions, source grants that cannot be restored, recipe/schema conflicts and current active rotation/apply state. Default is non-destructive preview with no wallpaper apply and no schedule activation. Require user-selected per-conflict policy (keep existing / import separate copy / skip), show totals before Commit Restore, ensure rollback/restart idempotency. Unknown future rule grammar remains disabled with reason.

**Unsupported wallpaper target:** detect capability on device before offering Home/Lock/Both; when Lock unsupported, show a reachable target choice that **does not silently apply Home**. Partial Both results must report each target separately; never claim both succeeded because one call returned. Back navigation preserves selected image and pending recovery draft.

**Recovery pathways:** source offline → Continue with local copies / Retry source / View source; revoked → Reconnect via Android picker / Keep local / Back; storage full → Manage storage / Retry / Keep verified; restore conflicts → Review each / Save for later / Cancel; unsupported target → Choose supported target / Back with state unchanged.

**Accessibility/motion:** focus returns to initiating control after Android permission activity; error title and recovery action announced in order; each condition has text+icon, not color alone; support 200% type/keyboard and gesture-free paths.

**Before/after:** independently compare existing `source-detail-offline.svg`, `backup-and-restore-restore.svg`, `destinations-unsupported.svg` and storage variants. Only create new revoked, conflict-choice and recovery-return drawings where existing phone-side content cannot express the distinguishable command. Render source-hashed annotation-free 401×890 crops and inspect pixels; no screenshot/device approval follows from drawing them.

**Acceptance:** offline/revoked local-copy tests, URI-grant expiry, disk exhaustion during stage and promotion, crash/restart dedup, restore conflict branching with preserved original bytes, backward navigation to previous source/selection, unsupported targets produce zero gateway calls. Physical device/provider and restore testing remains **UNVERIFIED**.

## Evidence progression and release gating

For each candidate record four independent fields: `illustratedInDraft`, `opticallyReviewedFromExactSourceSHA`, `verifiedOnAndroid(build,device,API,scenario)`, `ownerApprovedExactSourceSHA`. Never coerce one into another. Candidate C changes **zero** cycle-A scores. Before art production, inspect existing rendered phone crops; before UI acceptance, collect Android screen captures/accessibility hierarchy and real operation traces. All proposed amendments are subject to owner review.
