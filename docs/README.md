> **Engineering audit and action program:** [October 29 Android codebase audit, 17 findings, six agent work packages and dependency-aware release gates](quality/ARCHITECTURE-AUDIT-2026-10-29.md). Prioritize alongside the [live queue](planning/implementation-queue.md). Existing historical blueprints remain useful design sources, not proof of merged implementation.

# Documentation map

**Read [current decisions](planning/current-decisions.md) first.** Documents from the historical Waltz phase sometimes describe superseded UI and staged implementation ideas. The more recent specific contract takes precedence.

## Product and requirements

- [Product specification](product/product-specification.md): complete long-form historical scope, with supersession notice
- [73-scenario feature completeness audit](product/feature-audit.md): the detailed requirement audit
- [70 natural-language workflows](product/workflows.md): core user journeys
- [Rainbow quickstart](product/rainbow-quickstart.md): first valuable end-to-end path
- [Screenshot importing and outbound sharing](product/image-import-sharing.md)
- [Everyday app experience](product/everyday-app-experience.md): About, support, reviews, diagnostics, privacy and updates
- [Workflow design response](product/workflow-design.md) and [coverage matrix](product/workflow-coverage.md)

## UX and design

- [Paper-reel browsing](design/reel-interaction.md) and [critical functional UX audit](design/reel-ux-audit.md)
- [Reel layout comparison](design/reel-layout-comparison.md): experimental design variants
- [Human expectations walkthroughs](design/human-expectations.md)
- [Bidirectional reference gap audit](design/reference-gap-audit.md) and [unified proposed images](../design/proposals/README.md)
- [Current-generation proposal mockup coverage gap register](design/proposal-mockup-coverage-gaps-2026-10-23.md): 34 missing named state variants plus separately tracked storyboard/platform/stress-image gaps; inventory only, no design scoring
- [UI surface inventory](design/ui-surface-inventory.md) and [machine-readable screen contract](design/screen-contract.json)

## External mobile UX research and future proposal audit

- [Multilingual mobile UX principle inventory](research/mobile-ux-weighted-principles-2026-10-23.md): 174 generalized checks, individually weighted 0–5 for Glimpse with source register (research only)
- [Refined 60-question UI/UX proposal audit instrument](quality/mobile-ux-proposal-audit-instrument-2026-10-23.md): prepared future audit criteria, **no proposals assessed yet**

## Engineering

- [Android architecture](architecture/android.md)
- [Private-original no-overwrite and crash-recovery contract](architecture/private-original-publication.md): API29/34 evidence, concurrent publishers, interrupted-copy provenance and safe recovery
- [Lossless Room v3 integrity and legacy recovery plan](planning/ROOM-INTEGRITY-RECOVERY.md): validation, escrow alternatives, and explicit acceptance prerequisites
- [Technical behavior contracts](architecture/behavior-contracts.md)
- [Reel motion and rendering](architecture/reel-motion.md)

## Delivery and evidence

- [Current decisions](planning/current-decisions.md), [decision queue](planning/decision-queue.md)
- [Reference acceptance plan](quality/reference-acceptance.md), [Android implementation visual atlas](quality/android-visual-inspection.md)
- [Design assets and availability](assets.md), [editable design sources](../design/README.md)
- [2026-10-22 conversation record](history/conversation-record-2026-10-22.md)
- [2026-10-23 development conversation: available verbatim user messages and marked assistant summaries](history/glimpse-development-conversation-2026-10-23.md): archival, with original verbatim limits disclosed

**Important:** There is now an initial [native app source tree](../app/README.md), but no completed device acceptance or verified release artifact. Follow the [implementation queue](planning/implementation-queue.md) and [evidence ledger](quality/first-slice-evidence.md).
