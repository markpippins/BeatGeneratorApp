# Phase A Milestone 1 Report: Inventory & Delta Mapping (Option 2)

Date: 2026-05-03
Author: OpenCode

Purpose
- Establish a running understanding of API surface gaps and decisions for Phase A realignment (Option 2).
- Provide a concrete inventory of public core-model surfaces used by beatgen-core and a delta plan that will drive subsequent changes.

Scope
- Public API surfaces touched by beatgen-core: Player, InstrumentWrapper, Rule, Session, Command, IBusListener, TimingBus, CommandBus, and related interfaces.
- Exclude RainSequencer and DSL work for Phase A; those are whiteboard concepts for later phases.

Inventory Approach
- Static code analysis and targeted searches (e.g., ripgrep) to identify method calls and surface usage against core-model classes.
- Manual cross-check against core-api surfaces to identify mismatches (renames, removed methods, added methods).
- Consolidate findings into a delta matrix (PhaseA_APIRealignment_DeltaMatrix.md).

Delta Mapping (initial)
- Delta entries will capture: surface, core-model surface, gap type (rename/add/remove), impact, recommended approach (adapt API vs adapter shim), risk, and owner.

Milestone 1 Acceptance Criteria
- A complete inventory of impacted public API surfaces is documented in this report.
- The initial PhaseA_APIRealignment_DeltaMatrix.md captures at least the first set of delta entries.
- A lightweight per-tick interaction example is documented (to anchor future tests).

Next steps
- Populate this report with concrete inventory results and link to the Delta Matrix.
- Begin Phase A Milestone 2: implement API realignment in a controlled commit sequence and validate green build.
