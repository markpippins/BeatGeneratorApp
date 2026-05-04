# Phase A Milestone 2 Plan: API Realignment (Option 2)

Date: 2026-05-03
Author: OpenCode

Goals
- Implement Phase A Milestone 2 changes in small, reviewable commits.
- Target additional core-model surfaces identified in Milestone 1 inventory.
- Validate green builds after each patch and update Delta Matrix accordingly.
- Prepare groundwork for Phase B: Sequencer stabilization.

Approach
- Break Milestone 2 into patches by surface group (Player, InstrumentWrapper, Rule, Session, Command, IBusListener).
- For each patch: adjust core-model surfaces to align with beatgen-core expectations; if breaking changes are unavoidable, document and implement minimal non-breaking adapters as a transitional step.
- After each patch: run mvn -f java/pom.xml -DskipTests install to verify compile health; capture and fix compile-time issues quickly.
- Maintain a Phase A delta journal in IMPLEMENTATION_RECORD to track decisions and rationale for each surface change.

Deliverables
- A sequence of small, well-scoped patches on the phase-a-api-realignment branch.
- Updated PhaseA_APIRealignment_DeltaMatrix.md with per-surface changes.
- Milestone 2 progress summary in PhaseA_APIRealignment_Milestone1_Report.md.

Acceptance Criteria
- Java module builds green after Milestone 2 patches.
- Delta Matrix updated to reflect Milestone 2 changes.
- Documentation remains consistent across Milestone 1 and 2 artifacts.

Notes
- Milestone 2 is the bridge to Phase B’s Sequencer stabilization work.
