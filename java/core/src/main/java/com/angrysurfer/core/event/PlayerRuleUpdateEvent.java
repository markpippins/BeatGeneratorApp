package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import lombok.Getter;

/**
 * Event sent when a player's rules have been updated
 */
public record PlayerRuleUpdateEvent(Player player, Rule rule, RuleUpdateType updateType) {
}