# Phase A Kickoff: API Realignment (Option 2)

Date: 2026-05-03
Author: OpenCode

Context
- We are pursuing Phase A as an API alignment exercise (Option 2): realign the core-model API to match the expectations of beatgen-core, instead of introducing adapters to preserve the existing surface.
- The goal is to achieve a green build while progressively collapsing the UI-driven wiring toward a clean, per-tick, rule-driven Sequencer path.

Scope
- Realign core-model API to the codebase used by beatgen-core, with minimal but focused changes to core-model to satisfy beatgen-core usage. This is not a full feature complete RainSequencer implementation; it concentrates on stabilizing compile-time APIs and surfaces that beatgen-core relies on.
- Phase A explicitly excludes introducing large DSLs or RainSequencer infrastructure; those remain whiteboard concepts to be tackled after a green baseline is achieved.

Assumptions
- The high-priority blocker is the large API drift between core-model and beatgen-core; we will address it by realigning the API surface rather than introducing cross-cutting adapters.
- We will use a single feature-branch workflow and maintain a strict backout path if needed.

Approach (Option 2: API Realignment)
- Establish a minimal, well-scoped contract that beatgen-core relies on (Player, Session, Rule, InstrumentWrapper, Command, IBusListener, etc.).
- Update core-model to expose the expected methods (or re-map via a small internal shim) to satisfy beatgen-core method calls for Phase A baselining. The emphasis is on surface-area reduction rather than deep refactors of internal behavior.
- Refactor beatgen-core usage to align with the updated core-model surface, favoring small, incremental edits with clear tests.
- Add regression tests that ensure the compile surface remains stable and that a minimal sequencing interaction can be exercised (without requiring RainSequencer work).

Plan & Milestones
- Milestone 1: API surface inventory and delta mapping (what methods are missing/renamed and where beatgen-core calls them).
- Milestone 2: Implement minimal compatibility shims or surface changes in core-model to restore expected method signatures.
- Milestone 3: Update beatgen-core call sites to use the realigned API and run a green build for the java module.
- Milestone 4: Add a lightweight per-tick unit test that demonstrates a potential path toward per-tick evaluation using the realigned API surface (as a validation anchor).

Risks & Mitigations
- Risk: Realignment touches many classes; mitigation: progress in small, reviewed commits; keep changes scoped to API signatures and public surfaces only.
- Risk: Hidden dependencies in core-model may ripple into other modules; mitigation: run full module builds and targeted tests after each milestone.
- Risk: UI regressions remain while we rework core surfaces; mitigation: separate UI work into a follow-on plan with feature flags and rollback guards.

Decision Points for Next Step
- Do we proceed with Phase A realignment on a dedicated branch (recommended) and stage the changes as small, reviewable commits per API surface chunk?
- Confirm the preferred delta set to start with (e.g., Player surface first, then Command/IBusListener, then Rule/InstrumentWrapper).

Immediate Next Steps (if approved)

Notes
- All future prompts, plans, and changes related to Phase A will be recorded in this folder in Markdown format as part of IMPLEMENTATION_RECORD.
- If you want a specific naming convention for the Phase A artifacts (e.g., PhaseA-Delta-xxx.md), I’ll follow that.
