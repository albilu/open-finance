package org.openfinance.entity;

/**
 * Enum representing the priority/urgency level of a financial insight.
 *
 * <p>Priority levels help users understand which insights require immediate attention
 * versus informational notifications.</p>
 *
 * <ul>
 *   <li><strong>HIGH</strong>: Requires immediate attention (e.g., budget exceeded, unusual activity, low balance)</li>
 *   <li><strong>MEDIUM</strong>: Important but not urgent (e.g., savings opportunity, trend notification, goal progress)</li>
 *   <li><strong>LOW</strong>: Informational only (e.g., monthly summary, general tips, educational content)</li>
 * </ul>
 *
 * @since Sprint 11 - AI Assistant Integration (Task 11.4)
 */
public enum InsightPriority {

    /**
     * High priority - requires immediate attention.
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li>Budget exceeded by 20% or more</li>
     *   <li>Unusual spending detected (50%+ increase)</li>
     *   <li>Account balance critically low (below $100)</li>
     *   <li>High-interest debt payment due soon</li>
     * </ul>
     *
     * <p><strong>Display:</strong> Red badge, shown at top of insight list</p>
     */
    HIGH("High Priority", "Requires immediate attention", 1),

    /**
     * Medium priority - important but not urgent.
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li>Savings opportunity identified</li>
     *   <li>Budget warning (75-99% spent)</li>
     *   <li>Investment rebalancing suggestion</li>
     *   <li>Goal progress milestone reached</li>
     * </ul>
     *
     * <p><strong>Display:</strong> Yellow/orange badge, middle of insight list</p>
     */
    MEDIUM("Medium Priority", "Important but not urgent", 2),

    /**
     * Low priority - informational only.
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li>Monthly spending summary</li>
     *   <li>General financial tips</li>
     *   <li>Educational content</li>
     *   <li>Positive trend notifications</li>
     * </ul>
     *
     * <p><strong>Display:</strong> Blue/gray badge, bottom of insight list</p>
     */
    LOW("Low Priority", "Informational only", 3);

    private final String displayName;
    private final String description;
    private final int sortOrder;

    InsightPriority(String displayName, String description, int sortOrder) {
        this.displayName = displayName;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    /**
     * Get human-readable display name for this priority level.
     *
     * @return Display name (e.g., "High Priority")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get description explaining this priority level.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get sort order for priority (1 = highest, 3 = lowest).
     *
     * <p>Used for sorting insights with HIGH priority first, LOW priority last.</p>
     *
     * @return Sort order (1-3)
     */
    public int getSortOrder() {
        return sortOrder;
    }
}
