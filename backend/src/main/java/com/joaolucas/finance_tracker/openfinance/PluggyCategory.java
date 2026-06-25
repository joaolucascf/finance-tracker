package com.joaolucas.finance_tracker.openfinance;

import java.util.Arrays;
import java.util.List;

/**
 * Maps provider (Pluggy) transaction categories to internal behavior — the seam between the
 * external taxonomy and how the app treats each kind of movement.
 *
 * <p>Today its only job is to flag categories that are <strong>hidden from the main ledger</strong>
 * (investment in/outflows, which are transfers to oneself, not real spending). The raw provider
 * category string is still persisted on each {@code Transaction}, so a future investments view can
 * read the same {@code INVESTMENTS} group without re-syncing.
 *
 * <p>Kept as an enum rather than a DB table on purpose: this is a code-level policy, not something
 * configured at runtime, so extending it is just adding a constant or a label here.
 *
 * <p>Matching is case-insensitive and covers a few label variants because the exact string Pluggy
 * emits can vary; verify against {@code GET /v2/transactions} and adjust the labels if needed.
 */
public enum PluggyCategory {

    /** Aportes e resgates de investimentos — ocultos do extrato principal. */
    INVESTMENTS(true, "Investments", "Investment", "Fixed income", "Variable income");

    private final boolean hiddenFromLedger;
    private final List<String> providerLabels;

    PluggyCategory(boolean hiddenFromLedger, String... providerLabels) {
        this.hiddenFromLedger = hiddenFromLedger;
        this.providerLabels = Arrays.stream(providerLabels).map(s -> s.toLowerCase()).toList();
    }

    /** Lower-cased provider category labels that should be excluded from the main ledger. */
    public static List<String> hiddenLedgerLabels() {
        return Arrays.stream(values())
                .filter(c -> c.hiddenFromLedger)
                .flatMap(c -> c.providerLabels.stream())
                .toList();
    }
}
