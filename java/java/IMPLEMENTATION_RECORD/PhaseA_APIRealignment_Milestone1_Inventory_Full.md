# Phase A Milestone 1 Inventory (Full)

Date: 2026-05-03
Author: OpenCode

Overview
- Concrete inventory of public API surfaces used by beatgen-core from the core-model package.

Surfaces
- Player (com.angrysurfer.core.model.Player): getId(), setId(Long); getName(), setName(String); getInstrument(), setInstrument(InstrumentWrapper); getInstrumentId(), setInstrumentId(Long); getSession(), setSession(Session); getEnabled(), setEnabled(Boolean); isSelected(); getRules(), setRules(Set<Rule>); getChannel(), setChannel(Integer); getDefaultChannel(), setDefaultChannel(Integer); isPlaying(), setPlaying(Boolean); isMuted(); getProbability(); getRootNote(); getLevel().
- InstrumentWrapper (com.angrysurfer.core.model.InstrumentWrapper): getId(); getName(); getDeviceName(); getDevice(); getChannel(); getDefaultChannel(); getPreset(); getBankIndex(); getProperties(); getIsDefault() (shim).
- Rule (com.angrysurfer.core.model.Rule): getId(); getPlayer(); getOperator(); getComparison(); getValue(); getPart(); getStart(); getEnd(); toRow(); fromRow(Object[]).
- Command (com.angrysurfer.core.api.Command): getCommand(); getSender(); getData().
- IBusListener (com.angrysurfer.core.api.IBusListener): onAction(Command action).

Notes
- This inventory focuses on public methods that are observable from phase-A work and used by downstream components.
