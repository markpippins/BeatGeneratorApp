# Phase A API Realignment Delta Matrix (Delta 4)

Date: 2026-05-03
Author: OpenCode

Delta: Delta 4 - Lightweight test scaffold plan
- Surface: Introduce a minimal test module that exercises a basic per-tick trigger path using mocked Player/Instrument surfaces to verify public API interactions.
- Rationale: Provides deterministic smoke tests for API realignment without hardware.
- Approach: Create a small test utility under test/ that constructs a Player-like surface and InstrumentWrapper mocks, and invokes a minimal per-tick flow to ensure surface glue compiles and basic interactions compile.

Notes: This delta is intentionally lightweight and non-breaking; it serves as a test scaffold for Milestone 1 progress.
