# Phase A Milestone 1 Inventory: API Surfaces (Option 2) - v2

Date: 2026-05-03
Author: OpenCode

Overview
- Concrete inventory of public API surfaces used by beatgen-core from core-model classes.

Public Surfaces (core.model)
- Player (com.angrysurfer.core.model.Player)
  - getId(), setId(Long)
  - getName(), setName(String)
  - getInstrument(), setInstrument(InstrumentWrapper)
  - getInstrumentId(), setInstrumentId(Long)
  - getSession(), setSession(Session)
  - getEnabled(), setEnabled(Boolean)
  - isSelected()
  - getRules(), setRules(Set<Rule>)
  - getChannel(), setChannel(Integer)  // derived from instrument in implementation
  - getDefaultChannel(), setDefaultChannel(Integer)
  - isPlaying(), setPlaying(boolean)
  - isMuted(), setMuted(Boolean)
  - getProbability(), getRootNote(), getLevel()

- InstrumentWrapper (com.angrysurfer.core.model.InstrumentWrapper)
  - getId(), getName(), getDeviceName(), getDevice()
  - getChannel(), getDefaultChannel(), getPreset(), getBankIndex()
  - getProperties()
  - getIsDefault()  // shim, non-breaking

- Rule (com.angrysurfer.core.model.Rule)
  - getId(), getPlayer(), getOperator(), getComparison(), getValue(), getPart(), getStart(), getEnd()
  - toRow(), fromRow(Object[])

- Command (com.angrysurfer.core.api.Command)
  - getCommand(), getSender(), getData()

- IBusListener (com.angrysurfer.core.api.IBusListener)
  - onAction(Command action)

Notes:
- Canonical inventory for Milestone 1 public API surfaces; used to drive delta decisions and patching cadence.
