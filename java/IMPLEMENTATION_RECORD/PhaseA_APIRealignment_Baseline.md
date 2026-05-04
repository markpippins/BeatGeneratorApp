# Phase A Baseline: API Realignment (Option 2)

Date: 2026-05-03
Author: OpenCode

- This baseline documents the Phase A realignment work (Option 2) and references Milestone 1 (Phase A Milestone 1 Report) for inventory and delta planning.
- The root Milestone 1 artifacts live under the IMPLEMENTATION_RECORD directory to ensure a single source of truth.
Scope
- Realign core-model API surfaces touched by beatgen-core: Player, InstrumentWrapper, Rule, Session, Command, IBusListener, TimingBus, CommandBus, and related interfaces.
- Do not implement RainSequencer or Live DSL work in Phase A; that remains white-board scope for later phases.

Milestone 1 Status
- Inventory doc: PhaseA_APIRealignment_Milestone1_Inventory.md (root) to be populated.
- Milestone 1 Report: PhaseA_APIRealignment_Milestone1_Report.md (root) created.
- Delta Matrix: PhaseA_APIRealignment_DeltaMatrix.md (root) exists.
- Baseline: PhaseA_APIRealignment_Baseline.md (root) remains the canonical baseline reference.
- This baseline documents the Phase A realignment work to converge the core-model API surface with beatgen-core expectations, without introducing adapters. The aim is to produce a green build as a foundation for Phase B sequencing work.

Scope
- Realign core-model API surfaces touched by beatgen-core: Player, InstrumentWrapper, Rule, Session, Command, IBusListener, TimingBus, CommandBus, and related interfaces.
- Do not implement RainSequencer or Live DSL work in Phase A; that remains white-board scope for later phases.

Milestone 1: Inventory & Delta Mapping (Phase A)
- Objective: Generate a concrete inventory of public API surfaces used by beatgen-core and an initial delta mapping that will drive subsequent changes.
- Deliverables:
- PhaseA_APIRealignment_Milestone1_Report.md: Milestone 1 results (inventory and delta entries).
- PhaseA_APIRealignment_DeltaMatrix.md: Initial delta mapping for Phase A.
- PhaseA_APIRealignment_Milestone1_Report.md: Detailed Milestone 1 results (inventory, deltas, acceptance criteria).

Milestone 1 Acceptance Criteria
- A complete inventory of impacted public API surfaces is documented.
- The Delta Matrix contains the initial set of concrete deltas with rationales.
- A lightweight test scaffold exists for a minimal per-tick interaction path.

Next Steps
- Populate Milestone 1 results and delta matrix.
- Begin Phase A Milestone 2: implement API realignment in a controlled commit sequence and validate green build.
