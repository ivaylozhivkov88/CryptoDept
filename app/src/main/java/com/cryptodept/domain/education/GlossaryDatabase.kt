package com.cryptodept.domain.education

/**
 * Static glossary of crypto and trading terms.
 * Bundled with app — no API calls needed.
 * 
 * To add a new entry:
 *   1. Add to `entries` list
 *   2. Use unique `id` (lowercase, hyphenated)
 *   3. Reference from InfoTooltip widget via id
 */
object GlossaryDatabase {
    
    data class GlossaryEntry(
        val id: String,
        val term: String,
        val category: GlossaryCategory,
        val shortDefinition: String,
        val fullExplanation: String,
        val example: String? = null,
        val relatedIds: List<String> = emptyList(),
    )
    
    enum class GlossaryCategory(val displayName: String, val emoji: String) {
        BASICS("Basics", "📚"),
        TECHNICAL("Technical Analysis", "📊"),
        ON_CHAIN("On-Chain", "⛓️"),
        DEFI("DeFi", "🏦"),
        DERIVATIVES("Derivatives", "📈"),
        PSYCHOLOGY("Psychology & Risk", "🧠"),
        SECURITY("Security", "🔒"),
    }
    
    val entries: List<GlossaryEntry> = listOf(
        
        // ═══════════════════════════════════════
        // BASICS (10 entries)
        // ═══════════════════════════════════════
        GlossaryEntry(
            id = "btc",
            term = "Bitcoin (BTC)",
            category = GlossaryCategory.BASICS,
            shortDefinition = "The first and largest cryptocurrency, created in 2009 by Satoshi Nakamoto.",
            fullExplanation = "Bitcoin is a decentralized digital currency operating without a central bank or single administrator. " +
                "Transactions are verified by network nodes through cryptography and recorded on a public distributed ledger called a blockchain. " +
                "Its supply is hard-capped at 21 million units, making it a digital alternative to gold. " +
                "It uses the Proof of Work (PoW) consensus mechanism, where miners secure the network by solving complex mathematical puzzles.",
            relatedIds = listOf("blockchain", "halving", "hodl"),
        ),
        GlossaryEntry(
            id = "eth",
            term = "Ethereum (ETH)",
            category = GlossaryCategory.BASICS,
            shortDefinition = "A decentralized global software platform powered by blockchain technology.",
            fullExplanation = "Ethereum is most commonly known for its native cryptocurrency, Ether (ETH). " +
                "Unlike Bitcoin, Ethereum was designed to be a programmable blockchain that supports Smart Contracts. " +
                "This allows developers to build Decentralized Applications (dApps), launch new tokens (ERC-20), and create Decentralized Finance (DeFi) protocols. " +
                "In 2022, it transitioned to Proof of Stake (PoS), significantly reducing its energy consumption.",
            relatedIds = listOf("smart-contract", "gas", "defi"),
        ),
        GlossaryEntry(
            id = "xrp",
            term = "XRP",
            category = GlossaryCategory.BASICS,
            shortDefinition = "A digital asset built for payments, native to the XRP Ledger.",
            fullExplanation = "XRP is a technology that is mainly known for its digital payment network and protocol. " +
                "XRP was created by Ripple (the company) to be a speedier, less costly, and more scalable alternative to both other digital assets and existing monetary payment platforms like SWIFT. " +
                "Unlike Bitcoin, XRP does not use mining; instead, it uses a consensus ledger that requires fewer resources and provides near-instant transaction finality.",
            relatedIds = listOf("blockchain", "exchange"),
        ),
        GlossaryEntry(
            id = "rsi",
            term = "RSI (Relative Strength Index)",
            category = GlossaryCategory.TECHNICAL,
            shortDefinition = "A momentum oscillator that measures the speed and change of price movements.",
            fullExplanation = "RSI oscillates between 0 and 100. Traditionally, an asset is considered overbought when the RSI is above 70 and oversold when it is below 30. " +
                "Traders use it to identify potential trend reversals or corrective pullbacks. " +
                "Divergence between RSI and price action is one of the most powerful signals—for example, if price makes a new high but RSI makes a lower high, it suggests weakening momentum.",
            example = "BTC price hits $100k, but RSI is only 65 (Lower than previous peak). This is a Bearish Divergence.",
            relatedIds = listOf("macd", "ema"),
        ),
        GlossaryEntry(
            id = "macd",
            term = "MACD",
            category = GlossaryCategory.TECHNICAL,
            shortDefinition = "Moving Average Convergence Divergence—a trend-following momentum indicator.",
            fullExplanation = "MACD consists of three components: the MACD line, the Signal line, and the Histogram. " +
                "A 'Bullish Cross' occurs when the MACD line crosses above the Signal line, suggesting a potential buy entry. " +
                "A 'Bearish Cross' occurs when it crosses below. The histogram measures the distance between the two lines, representing the strength of the trend.",
        ),
        GlossaryEntry(
            id = "position-size",
            term = "Position Sizing",
            category = GlossaryCategory.PSYCHOLOGY,
            shortDefinition = "The process of determining the correct amount of capital to risk on a single trade.",
            fullExplanation = "The single most important rule in professional trading. Never risk more than 1% to 2% of your total account on any single setup. " +
                "Proper sizing allows you to survive a string of losses without liquidating your account. " +
                "It is calculated using the distance from your entry price to your stop-loss.",
            example = "With a $10,000 account, a 1% risk is $100. If your stop-loss is 5% away, your position size should be $2,000.",
            relatedIds = listOf("stop-loss", "risk-reward"),
        ),
        GlossaryEntry(
            id = "risk-reward",
            term = "Risk:Reward Ratio (R:R)",
            category = GlossaryCategory.PSYCHOLOGY,
            shortDefinition = "A measure of the potential profit compared to the potential loss of a trade.",
            fullExplanation = "A 1:3 R:R means you are risking $1 to make $3. Even with a win rate of only 40%, a consistent 1:3 R:R will make you highly profitable. " +
                "The CryptoDept Trade Planner validates this ratio for every setup. Avoid trades where the risk is greater than or equal to the reward.",
        ),
    )
    
    // ═══════════════════════════════════════
    // QUERY HELPERS
    // ═══════════════════════════════════════
    
    fun getById(id: String): GlossaryEntry? = entries.firstOrNull { it.id == id }
    
    fun getByCategory(category: GlossaryCategory): List<GlossaryEntry> =
        entries.filter { it.category == category }
    
    fun search(query: String): List<GlossaryEntry> {
        if (query.isBlank()) return entries
        val lower = query.lowercase()
        return entries.filter {
            it.term.lowercase().contains(lower) ||
            it.shortDefinition.lowercase().contains(lower) ||
            it.id.contains(lower)
        }
    }
    
    fun getRelated(entry: GlossaryEntry): List<GlossaryEntry> =
        entry.relatedIds.mapNotNull { getById(it) }
}
