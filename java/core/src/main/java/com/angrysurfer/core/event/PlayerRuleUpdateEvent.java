package com.angrysurfer.core.event;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import lombok.Getter;

/**
 * Event sent when a player's rules have been updated
 */
@Getter
public class PlayerRuleUpdateEvent {
    private final Player player;
    private final Rule updatedRule;
    private final RuleUpdateType updateType;
    
    /**
     * Constructor for rule added/edited/deleted events
     * 
     * @param player The player whose rules were updated
     * @param rule The specific rule that was added/edited/deleted (can be null for bulk operations)
     * @param updateType The type of update that occurred
     */
    public PlayerRuleUpdateEvent(Player player, Rule rule, RuleUpdateType updateType) {
        this.player = player;
        this.updatedRule = rule;
        this.updateType = updateType;
    }
    
    /**
     * Constructor for bulk rule updates where no specific rule is the focus
     * 
     * @param player The player whose rules were updated
     * @param updateType The type of update that occurred
     */
    public PlayerRuleUpdateEvent(Player player, RuleUpdateType updateType) {
        this(player, null, updateType);
    }
    
    /**
     * Enum to specify the type of rule update
     */
    public enum RuleUpdateType {
        RULE_ADDED,
        RULE_EDITED,
        RULE_DELETED,
        RULES_REORDERED,
        ALL_RULES_DELETED,
        RULES_IMPORTED
    }
}