package com.example.model

import androidx.compose.ui.graphics.Color

enum class AppLanguage(val code: String, val labelEn: String, val labelBn: String) {
    ENGLISH("en", "English", "ইংরেজি"),
    BANGLA("bn", "Bangla", "বাংলা")
}

enum class PlayerId(
    val index: Int,
    val number: Int,
    val color: Color,
    val darkColor: Color,
    val lightColor: Color,
    val emoji: String,
    val nameEn: String,
    val nameBn: String
) {
    P1(0, 1, Color(0xFFEF4444), Color(0xFF991B1B), Color(0xFFFCA5A5), "🔴", "Player 1", "প্লেয়ার ১"),
    P2(1, 2, Color(0xFF3B82F6), Color(0xFF1E40AF), Color(0xFF93C5FD), "🔵", "Player 2", "প্লেয়ার ২"),
    P3(2, 3, Color(0xFFEAB308), Color(0xFF854D0E), Color(0xFFFDE047), "🟡", "Player 3", "প্লেয়ার ৩"),
    P4(3, 4, Color(0xFF22C55E), Color(0xFF166534), Color(0xFF86EFAC), "🟢", "Player 4", "প্লেয়ার ৪");

    fun displayBadge(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) "$nameBn $emoji" else "$nameEn $emoji"
}

enum class GameCategory(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val accentColor: Color
) {
    ALL("all", "All Games", "সব গেম", Color(0xFF38BDF8)),
    ACTION("action", "Action", "অ্যাকশন", Color(0xFFEF4444)),
    RACING("racing", "Racing", "রেসিং", Color(0xFFF97316)),
    SPORTS("sports", "Sports", "স্পোর্টস", Color(0xFF22C55E)),
    PUZZLE("puzzle", "Puzzle", "পাজল", Color(0xFFA855F7)),
    ARCADE("arcade", "Arcade", "আর্কেড", Color(0xFFEAB308)),
    STRATEGY("strategy", "Strategy", "স্ট্র্যাটেজি", Color(0xFF06B6D4)),
    CASUAL("casual", "Casual", "ক্যাজুয়াল", Color(0xFFEC4899)),
    SURVIVAL("survival", "Survival", "সারভাইভাল", Color(0xFF14B8A6));

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) titleBn else titleEn
}

enum class ControlScheme(
    val labelEn: String,
    val labelBn: String,
    val primaryBtnEn: String,
    val primaryBtnBn: String,
    val secondaryBtnEn: String?,
    val secondaryBtnBn: String?,
    val usesJoystick: Boolean
) {
    JOYSTICK_ONLY(
        "Movement Joystick",
        "মুভমেন্ট জয়স্টিক",
        "",
        "",
        null,
        null,
        true
    ),
    JOYSTICK_ACTION(
        "Joystick + Action",
        "জয়স্টিক + অ্যাকশন",
        "ACTION",
        "অ্যাকশন",
        null,
        null,
        true
    ),
    JOYSTICK_ATTACK_DASH(
        "Joystick + Attack + Dash",
        "জয়স্টিক + অ্যাটাক + ড্যাশ",
        "ATTACK",
        "আক্রমণ",
        "DASH",
        "ড্যাশ",
        true
    ),
    JOYSTICK_JUMP(
        "Joystick + Jump",
        "জয়স্টিক + জাম্প",
        "JUMP",
        "লাফ",
        null,
        null,
        true
    ),
    JOYSTICK_SHOOT(
        "Joystick + Shoot",
        "জয়স্টিক + শুট",
        "SHOOT",
        "শুট",
        "DASH",
        "ড্যাশ",
        true
    ),
    RACING_CONTROLS(
        "Steer + Gas + Brake",
        "স্টিয়ার + গ্যাস + ব্রেক",
        "GAS",
        "গ্যাস",
        "BRAKE",
        "ব্রেক",
        true
    ),
    SPORTS_CONTROLS(
        "Move + Kick/Shot + Dash",
        "মুভ + শট + ড্যাশ",
        "SHOT",
        "শট",
        "PASS",
        "পাস",
        true
    ),
    TAP_REACTION(
        "Quick Tap / Trigger",
        "কুইক ট্যাপ / ট্রিগার",
        "TAP!",
        "ট্যাপ!",
        "ALT",
        "বিকল্প",
        false
    );

    fun description(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) labelBn else labelEn

    fun primaryLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) primaryBtnBn else primaryBtnEn

    fun secondaryLabel(lang: AppLanguage): String? =
        if (lang == AppLanguage.BANGLA) secondaryBtnBn else secondaryBtnEn
}

enum class WinRule(val labelEn: String, val labelBn: String) {
    HIGHEST_SCORE("Highest Score Wins", "সর্বোচ্চ স্কোর বিজয়ী"),
    LAST_SURVIVING("Last Surviving Player Wins", "শেষ টিকে থাকা প্লেয়ার বিজয়ী"),
    FIRST_TO_TARGET("First to Target Score Wins", "লক্ষ্যে আগে পৌঁছানো প্লেয়ার বিজয়ী"),
    MOST_GOALS("Most Goals Scored Wins", "সবচেয়ে বেশি গোল বিজয়ী"),
    MOST_TERRITORY("Most Territory Captured Wins", "সবচেয়ে বেশি এলাকা দখল বিজয়ী"),
    FIRST_TO_FINISH("First to Finish Line Wins", "সবার আগে ফিনিশ লাইনে পৌঁছানো বিজয়ী"),
    MOST_COLLECTED("Most Objects Collected Wins", "সবচেয়ে বেশি অবজেক্ট সংগ্রহ বিজয়ী"),
    BEST_PUZZLE_RESULT("Best Puzzle Score Wins", "সেরা পাজল ফলাফল বিজয়ী"),
    KING_TIME("Longest Zone Control Wins", "সবচেয়ে বেশি সময় জোন নিয়ন্ত্রণ বিজয়ী");

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) labelBn else labelEn
}

enum class ArenaTheme {
    CYBER_ARENA,
    CRYSTAL_CAVERN,
    BOMB_FACTORY,
    TARGET_RANGE,
    STORM_ZONE,
    METEOR_CRATER,
    LASER_GRID,
    SHADOW_DUNGEON,
    TREASURE_TEMPLE,
    CASTLE_FORTRESS,
    ASPHALT_TRACK,
    OVAL_SPEEDWAY,
    HIGHWAY_TRAFFIC,
    TURBO_STRIP,
    ICE_RINK,
    DESERT_CANYON,
    NEON_BRIDGE,
    CITY_CORNERS,
    WATER_BRIDGES,
    CHECKPOINT_CITY,
    SOCCER_PITCH,
    BASKETBALL_COURT,
    AIR_HOCKEY_TABLE,
    TENNIS_COURT,
    PING_PONG_TABLE,
    BOWLING_LANE,
    PUZZLE_MATRIX,
    MAZE_LABYRINTH,
    LAVA_VOLCANO,
    MAGNET_LAB,
    GOLF_GREEN,
    CARNIVAL_RING,
    MIRROR_HALL
}

enum class MechanicGroup {
    BRAWL_KNOCKBACK,
    COLLECT_AND_RETURN,
    BOMB_DODGE_KICK,
    SHOOT_TARGETS,
    SAFE_ZONE_SURVIVAL,
    HAZARD_DODGE,
    TAG_PASS_CURSE,
    CASTLE_SIEGE,
    CIRCUIT_RACING,
    HIGHWAY_DODGE,
    BRIDGE_BUILDER,
    CHECKPOINT_RUSH,
    BALL_SPORTS,
    PENALTY_DUEL,
    BASKET_SHOOT,
    PADDLE_DEFENSE,
    BOWLING_ROLL,
    TILE_PUZZLE,
    MAZE_RUNNER,
    MATH_PATTERN_QUIZ,
    TERRITORY_PAINT,
    ZONE_CAPTURE,
    WALL_TRAIL_TRAP,
    REACTION_TAP,
    GOLF_PUTT,
    DISC_DODGEBALL,
    SHAPE_MORPH_CHASE
}

data class GameSpec(
    val id: Int,
    val nameEn: String,
    val nameBn: String,
    val category: GameCategory,
    val descEn: String,
    val descBn: String,
    val objectiveEn: String,
    val objectiveBn: String,
    val supportedPvpCounts: List<Int>, // e.g., listOf(2), listOf(2, 3), or listOf(2, 3, 4)
    val supportedBotCounts: List<Int>, // e.g., listOf(1), listOf(1, 2), or listOf(1, 2, 3)
    val durationSeconds: Int,
    val targetScore: Int, // 0 if time-based only
    val controlScheme: ControlScheme,
    val winRule: WinRule,
    val arenaTheme: ArenaTheme,
    val mechanicGroup: MechanicGroup,
    val variantIndex: Int, // Specific sub-mechanic variation so every game behaves uniquely
    val keywords: List<String>
) {
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) nameBn else nameEn
    fun description(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) descBn else descEn
    fun objective(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) objectiveBn else objectiveEn
    val maxPlayers: Int get() = supportedPvpCounts.maxOrNull() ?: 2
}

enum class MatchMode(val labelEn: String, val labelBn: String) {
    PVP("Player vs Player", "প্লেয়ার বনাম প্লেয়ার"),
    VS_BOT("VS Bot Mode", "বনাম বট মোড");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) labelBn else labelEn
}

enum class BotDifficulty(val labelEn: String, val labelBn: String, val descEn: String, val descBn: String) {
    EASY("Easy", "সহজ", "Slower reactions & simpler positioning", "ধীর প্রতিক্রিয়া এবং সহজ মুভমেন্ট"),
    NORMAL("Normal", "সাধারণ", "Balanced reaction speed & smart tactics", "ভারসাম্যপূর্ণ গতি এবং কৌশল"),
    HARD("Hard", "কঠিন", "Sharp positioning, predictive aim & strategy", "নিখুঁত পজিশনিং এবং উন্নত কৌশল");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) labelBn else labelEn
    fun description(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) descBn else descEn
}

enum class GraphicsQuality(val labelEn: String, val labelBn: String, val particleMultiplier: Float) {
    LOW("Low (Battery Saver)", "লো (ব্যাটারি সেভার)", 0.4f),
    MEDIUM("Medium (Balanced)", "মিডিয়াম (ভারসাম্যপূর্ণ)", 0.8f),
    HIGH("High (60 FPS + Effects)", "হাই (৬০ এফপিএস + ইফেক্টস)", 1.2f);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) labelBn else labelEn
}

data class MatchConfig(
    val game: GameSpec,
    val mode: MatchMode,
    val pvpPlayerCount: Int = game.supportedPvpCounts.last(),
    val botCount: Int = game.supportedBotCounts.last(),
    val botDifficulty: BotDifficulty = BotDifficulty.NORMAL
) {
    val totalActivePlayers: Int
        get() = if (mode == MatchMode.PVP) pvpPlayerCount else (1 + botCount)

    fun isBot(playerIndex: Int): Boolean =
        mode == MatchMode.VS_BOT && playerIndex > 0
}

data class PlayerResult(
    val playerId: PlayerId,
    val isBot: Boolean,
    val score: Int,
    val survived: Boolean,
    val rank: Int,
    val statNoteEn: String,
    val statNoteBn: String
) {
    fun statNote(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) statNoteBn else statNoteEn
}

data class MatchResult(
    val config: MatchConfig,
    val winner: PlayerId?, // null if draw
    val playerResults: List<PlayerResult>,
    val durationPlayedSeconds: Int,
    val finishReasonEn: String,
    val finishReasonBn: String
) {
    fun finishReason(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) finishReasonBn else finishReasonEn
}
