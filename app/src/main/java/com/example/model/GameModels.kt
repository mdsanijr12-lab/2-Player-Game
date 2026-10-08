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

/**
 * Context-specific controls tailored to each game's exact genre and mechanics.
 */
enum class ControlScheme(
    val labelEn: String,
    val labelBn: String,
    val primaryBtnEn: String,
    val primaryBtnBn: String,
    val secondaryBtnEn: String?,
    val secondaryBtnBn: String?,
    val usesJoystick: Boolean,
    val howToControlEn: String,
    val howToControlBn: String
) {
    FIGHTING_CONTROLS(
        labelEn = "Joystick + Attack + Dodge",
        labelBn = "জয়স্টিক + অ্যাটাক + ডজ",
        primaryBtnEn = "ATTACK",
        primaryBtnBn = "আক্রমণ",
        secondaryBtnEn = "DODGE",
        secondaryBtnBn = "ডজ",
        usesJoystick = true,
        howToControlEn = "Use the Joystick to position your fighter, press ATTACK to strike & knock back nearby opponents, and press DODGE to evade incoming hits.",
        howToControlBn = "জয়স্টিক দিয়ে মুভ করুন, ATTACK চাপুন প্রতিপক্ষকে আঘাত করতে এবং DODGE চাপুন আক্রমণ এড়াতে।"
    ),
    PUSH_ARENA_CONTROLS(
        labelEn = "Movement + Push + Dash",
        labelBn = "মুভমেন্ট + পুশ + ড্যাশ",
        primaryBtnEn = "PUSH",
        primaryBtnBn = "ধাক্কা",
        secondaryBtnEn = "DASH",
        secondaryBtnBn = "ড্যাশ",
        usesJoystick = true,
        howToControlEn = "Move with the Joystick, press PUSH to unleash a heavy shock-shove, and use DASH to charge or recover back to the platform center.",
        howToControlBn = "জয়স্টিক দিয়ে মুভ করুন, PUSH চেপে প্রতিপক্ষকে প্ল্যাটফর্মের বাইরে ধাক্কা দিন এবং DASH দিয়ে দ্রুত চার্জ করুন।"
    ),
    TANK_CONTROLS(
        labelEn = "Move + Aim Turret + Fire",
        labelBn = "মুভ + টারেট নিশানা + ফায়ার",
        primaryBtnEn = "FIRE",
        primaryBtnBn = "ফায়ার",
        secondaryBtnEn = "AIM",
        secondaryBtnBn = "নিশানা",
        usesJoystick = true,
        howToControlEn = "Drive your tank/blaster with the Joystick, press AIM to rotate/lock your turret onto targets, and press FIRE to launch high-impact shells.",
        howToControlBn = "জয়স্টিক দিয়ে ট্যাংক চালান, AIM চেপে টারেট ঘোরান এবং FIRE চেপে গোলা নিক্ষেপ করুন।"
    ),
    RACING_CONTROLS(
        labelEn = "Steer + Accelerate + Brake",
        labelBn = "স্টিয়ার + অ্যাক্সিলারেট + ব্রেক",
        primaryBtnEn = "ACCEL",
        primaryBtnBn = "গ্যাস",
        secondaryBtnEn = "BRAKE",
        secondaryBtnBn = "ব্রেক",
        usesJoystick = true,
        howToControlEn = "Steer your vehicle with the Joystick, hold ACCEL for maximum throttle on straights, and tap BRAKE to corner tightly without hitting track walls.",
        howToControlBn = "জয়স্টিক দিয়ে স্টিয়ারিং নিয়ন্ত্রণ করুন, ACCEL চেপে গতি বাড়ান এবং মোড়ে BRAKE ব্যবহার করুন।"
    ),
    SPORTS_CONTROLS(
        labelEn = "Move + Kick/Shot + Sprint",
        labelBn = "মুভ + শট/কিক + স্প্রিন্ট",
        primaryBtnEn = "SHOT",
        primaryBtnBn = "শট",
        secondaryBtnEn = "SPRINT",
        secondaryBtnBn = "স্প্রিন্ট",
        usesJoystick = true,
        howToControlEn = "Dribble/position with the Joystick, press SHOT/KICK to strike the ball/puck toward the goal, and press SPRINT for a burst of speed.",
        howToControlBn = "জয়স্টিক দিয়ে বল ড্রিবল করুন, SHOT চেপে গোলে শট নিন এবং SPRINT চেপে দ্রুত দৌড়ান।"
    ),
    JUMP_DODGE_CONTROLS(
        labelEn = "Joystick + Jump + Dash",
        labelBn = "জয়স্টিক + জাম্প + ড্যাশ",
        primaryBtnEn = "JUMP",
        primaryBtnBn = "লাফ",
        secondaryBtnEn = "DASH",
        secondaryBtnBn = "ড্যাশ",
        usesJoystick = true,
        howToControlEn = "Move with the Joystick, press JUMP to leap into the air over floor hazards/lasers, and press DASH for quick evasive bursts.",
        howToControlBn = "জয়স্টিক দিয়ে সরে যান, JUMP চেপে লেজার বা লাভার ওপর দিয়ে লাফ দিন এবং DASH দিয়ে দ্রুত বাঁচুন।"
    ),
    PUZZLE_TACTICAL_CONTROLS(
        labelEn = "Move + Interact + Boost",
        labelBn = "মুভ + অ্যাকশন + বুস্ট",
        primaryBtnEn = "ACT",
        primaryBtnBn = "অ্যাকশন",
        secondaryBtnEn = "BOOST",
        secondaryBtnBn = "বুস্ট",
        usesJoystick = true,
        howToControlEn = "Navigate the board with the Joystick, press ACT to claim tiles/objects or trigger switches, and press BOOST to beat rivals to targets.",
        howToControlBn = "জয়স্টিক দিয়ে বোর্ডে যান, ACT চেপে টাইল/অবজেক্ট সক্রিয় করুন এবং BOOST দিয়ে এগিয়ে যান।"
    ),
    TAP_REACTION(
        labelEn = "Strike + Guard Reflex",
        labelBn = "স্ট্রাইক + গার্ড রিফ্লেক্স",
        primaryBtnEn = "STRIKE!",
        primaryBtnBn = "ট্যাপ!",
        secondaryBtnEn = "GUARD",
        secondaryBtnBn = "গার্ড",
        usesJoystick = false,
        howToControlEn = "Watch the central signal closely! Press STRIKE! the exact instant the signal turns active/green, and use GUARD to block penalties.",
        howToControlBn = "সিগন্যাল সবুজ হওয়া মাত্রই STRIKE! বাটনে ট্যাপ করুন এবং পেনাল্টি এড়াতে সতর্ক থাকুন।"
    ),
    // Legacy aliases mapped cleanly so any existing references work seamlessly
    JOYSTICK_ONLY(
        labelEn = "Movement + Dash",
        labelBn = "মুভমেন্ট + ড্যাশ",
        primaryBtnEn = "DASH",
        primaryBtnBn = "ড্যাশ",
        secondaryBtnEn = null,
        secondaryBtnBn = null,
        usesJoystick = true,
        howToControlEn = "Use the Joystick to steer and press DASH for a quick burst of speed.",
        howToControlBn = "জয়স্টিক দিয়ে মুভ করুন এবং DASH চেপে গতি বাড়ান।"
    ),
    JOYSTICK_ACTION(
        labelEn = "Move + Action + Dash",
        labelBn = "মুভ + অ্যাকশন + ড্যাশ",
        primaryBtnEn = "ACTION",
        primaryBtnBn = "অ্যাকশন",
        secondaryBtnEn = "DASH",
        secondaryBtnBn = "ড্যাশ",
        usesJoystick = true,
        howToControlEn = "Use the Joystick to move, press ACTION to interact/kick/claim, and press DASH to sprint.",
        howToControlBn = "জয়স্টিক দিয়ে মুভ করুন, ACTION দিয়ে কাজ সম্পন্ন করুন এবং DASH দিয়ে দ্রুত ছুটুন।"
    ),
    JOYSTICK_ATTACK_DASH(
        labelEn = "Joystick + Attack + Dodge",
        labelBn = "জয়স্টিক + অ্যাটাক + ডজ",
        primaryBtnEn = "ATTACK",
        primaryBtnBn = "আক্রমণ",
        secondaryBtnEn = "DODGE",
        secondaryBtnBn = "ডজ",
        usesJoystick = true,
        howToControlEn = "Move with the Joystick, press ATTACK to strike & knock back rivals, and press DODGE to evade.",
        howToControlBn = "জয়স্টিক দিয়ে মুভ করুন, ATTACK দিয়ে আঘাত করুন এবং DODGE দিয়ে সরে যান।"
    ),
    JOYSTICK_JUMP(
        labelEn = "Joystick + Jump + Dash",
        labelBn = "জয়স্টিক + জাম্প + ড্যাশ",
        primaryBtnEn = "JUMP",
        primaryBtnBn = "লাফ",
        secondaryBtnEn = "DASH",
        secondaryBtnBn = "ড্যাশ",
        usesJoystick = true,
        howToControlEn = "Move with the Joystick, press JUMP to leap over hazards, and press DASH for quick evasion.",
        howToControlBn = "জয়স্টিক দিয়ে মুভ করুন এবং JUMP চেপে বাধার ওপর দিয়ে লাফ দিন।"
    ),
    JOYSTICK_SHOOT(
        labelEn = "Move + Aim Turret + Fire",
        labelBn = "মুভ + টারেট নিশানা + ফায়ার",
        primaryBtnEn = "FIRE",
        primaryBtnBn = "ফায়ার",
        secondaryBtnEn = "AIM",
        secondaryBtnBn = "নিশানা",
        usesJoystick = true,
        howToControlEn = "Move with the Joystick, press AIM to rotate/lock turret aim, and press FIRE to shoot.",
        howToControlBn = "জয়স্টিক দিয়ে মুভ করুন, AIM চেপে নিশানা করুন এবং FIRE চেপে শুট করুন।"
    );

    fun description(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) labelBn else labelEn

    fun primaryLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) primaryBtnBn else primaryBtnEn

    fun secondaryLabel(lang: AppLanguage): String? =
        if (lang == AppLanguage.BANGLA) secondaryBtnBn else secondaryBtnEn

    fun howToPlayText(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) howToControlBn else howToControlEn
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
    val supportedPvpCounts: List<Int>,
    val supportedBotCounts: List<Int>,
    val durationSeconds: Int,
    val targetScore: Int,
    val controlScheme: ControlScheme,
    val winRule: WinRule,
    val arenaTheme: ArenaTheme,
    val mechanicGroup: MechanicGroup,
    val variantIndex: Int,
    val keywords: List<String>
) {
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) nameBn else nameEn
    fun description(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) descBn else descEn
    fun objective(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) objectiveBn else objectiveEn
    val maxPlayers: Int get() = supportedPvpCounts.maxOrNull() ?: 2

    /**
     * Resolves the genre-accurate context-specific control scheme for this game.
     */
    val effectiveControlScheme: ControlScheme
        get() = when (mechanicGroup) {
            MechanicGroup.BRAWL_KNOCKBACK ->
                if (variantIndex in listOf(1, 2, 4)) ControlScheme.PUSH_ARENA_CONTROLS else ControlScheme.FIGHTING_CONTROLS
            MechanicGroup.CASTLE_SIEGE, MechanicGroup.SHOOT_TARGETS ->
                if (variantIndex == 5) ControlScheme.FIGHTING_CONTROLS else ControlScheme.TANK_CONTROLS
            MechanicGroup.CIRCUIT_RACING, MechanicGroup.HIGHWAY_DODGE ->
                ControlScheme.RACING_CONTROLS
            MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL, MechanicGroup.BASKET_SHOOT,
            MechanicGroup.PADDLE_DEFENSE, MechanicGroup.BOWLING_ROLL, MechanicGroup.GOLF_PUTT,
            MechanicGroup.DISC_DODGEBALL ->
                ControlScheme.SPORTS_CONTROLS
            MechanicGroup.HAZARD_DODGE, MechanicGroup.SAFE_ZONE_SURVIVAL ->
                ControlScheme.JUMP_DODGE_CONTROLS
            MechanicGroup.TILE_PUZZLE, MechanicGroup.MAZE_RUNNER, MechanicGroup.MATH_PATTERN_QUIZ,
            MechanicGroup.TERRITORY_PAINT, MechanicGroup.WALL_TRAIL_TRAP,
            MechanicGroup.COLLECT_AND_RETURN, MechanicGroup.BRIDGE_BUILDER, MechanicGroup.CHECKPOINT_RUSH ->
                ControlScheme.PUZZLE_TACTICAL_CONTROLS
            MechanicGroup.BOMB_DODGE_KICK, MechanicGroup.TAG_PASS_CURSE,
            MechanicGroup.ZONE_CAPTURE, MechanicGroup.SHAPE_MORPH_CHASE ->
                ControlScheme.FIGHTING_CONTROLS
            MechanicGroup.REACTION_TAP ->
                ControlScheme.TAP_REACTION
        }

    /**
     * Short, punchy 2-3 second pre-match instruction banner explaining what the player controls and the immediate goal.
     */
    fun briefInstruction(lang: AppLanguage): String {
        val bn = lang == AppLanguage.BANGLA
        return when (mechanicGroup) {
            MechanicGroup.BRAWL_KNOCKBACK ->
                if (bn) "প্রতিপক্ষকে ধাক্কা দিয়ে এরিনা প্ল্যাটফর্মের বাইরে ফেলে দিন!" else "Push & knock opponents off the platform edge!"
            MechanicGroup.COLLECT_AND_RETURN ->
                if (bn) "অবজেক্ট সংগ্রহ করে নিজের রঙের বেসে ফিরিয়ে আনুন!" else "Grab items & return them to your corner base!"
            MechanicGroup.BOMB_DODGE_KICK ->
                if (bn) "বোমা বিস্ফোরণ এড়ান এবং শত্রুর দিকে বোমা কিক করুন!" else "Kick ticking bombs away & dodge blast rings!"
            MechanicGroup.SHOOT_TARGETS ->
                if (bn) "টারেট ঘুরিয়ে নিশানা করুন এবং চলমান টার্গেট ধ্বংস করুন!" else "Aim your blaster & destroy moving targets!"
            MechanicGroup.SAFE_ZONE_SURVIVAL ->
                if (bn) "ঝড় আসার আগেই সবুজ সেফ জোনে প্রবেশ করুন এবং শত্রুদের বাইরে ঠেলুন!" else "Rush inside the Safe Zone & shove rivals out!"
            MechanicGroup.HAZARD_DODGE ->
                if (bn) "লাফ (JUMP) ও ড্যাশ ব্যবহার করে লেজার এবং উল্কা এড়িয়ে চলুন!" else "Use JUMP & DASH to evade sweeping hazards!"
            MechanicGroup.TAG_PASS_CURSE ->
                if (variantIndex == 1) {
                    if (bn) "গোল্ডেন রিং দখল করুন এবং অন্যদের হাত থেকে পালিয়ে থাকুন!" else "Hold the Golden Ring & outrun all chasers!"
                } else {
                    if (bn) "কার্স/বোমা অন্য প্লেয়ারকে ছুঁয়ে পাস করে দিন!" else "Tag a rival to pass the curse before time runs out!"
                }
            MechanicGroup.CASTLE_SIEGE ->
                if (bn) "নিজের দুর্গ রক্ষা করুন এবং শত্রুর দুর্গে গোলাবর্ষণ করুন!" else "Aim your tank turret & shatter enemy fortifications!"
            MechanicGroup.CIRCUIT_RACING ->
                if (bn) "ট্র্যাকের দেয়াল বাঁচিয়ে সবার আগে ${targetScore.coerceAtLeast(5)} ল্যাপ শেষ করুন!" else "Steer the track & finish ${targetScore.coerceAtLeast(5)} laps first!"
            MechanicGroup.HIGHWAY_DODGE ->
                if (bn) "হাইওয়ের গাড়ি এড়িয়ে সর্বোচ্চ গতিতে এগিয়ে যান!" else "Weave through highway traffic without crashing!"
            MechanicGroup.BRIDGE_BUILDER ->
                if (bn) "নিজের রঙের তক্তা কুড়িয়ে নদীর ব্রিজ সম্পন্ন করুন!" else "Collect matching planks & build your bridge first!"
            MechanicGroup.CHECKPOINT_RUSH ->
                if (bn) "গোল্ডেন চেকপয়েন্ট গেটগুলোতে সবার আগে পৌঁছান!" else "Sprint through active checkpoint gates first!"
            MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL ->
                if (bn) "বল ড্রিবল করুন এবং শট নিয়ে প্রতিপক্ষের জালে গোল দিন!" else "Dribble the ball & shoot to score goals!"
            MechanicGroup.BASKET_SHOOT ->
                if (bn) "বাস্কেটবল নিয়ে হুপের কাছে যান এবং নিখুঁত শট নিন!" else "Grab the ball & shoot into the basketball hoop!"
            MechanicGroup.PADDLE_DEFENSE ->
                if (bn) "নিজের গোললাইন রক্ষা করুন এবং পাক/বল প্রতিপক্ষের কোর্টে পাঠান!" else "Defend your goal line & smash the puck back!"
            MechanicGroup.BOWLING_ROLL ->
                if (bn) "সঠিক কোণে বোলিং বল ছুড়ে সব পিন ফেলে দিন!" else "Line up your angle & roll strikes through the pins!"
            MechanicGroup.TILE_PUZZLE ->
                if (bn) "বোর্ডের টাইলগুলোতে গিয়ে ACT চেপে পাজল সমাধান করুন!" else "Move onto board tiles & press ACT to solve pairs!"
            MechanicGroup.MAZE_RUNNER ->
                if (bn) "গোলকধাঁধার দেয়াল পেরিয়ে সবার আগে লক্ষ্যে পৌঁছান!" else "Navigate the labyrinth walls to reach the beacon!"
            MechanicGroup.MATH_PATTERN_QUIZ ->
                if (bn) "উপরের প্রশ্নের সঠিক উত্তর লেখা প্যাডে সবার আগে দাঁড়ান!" else "Solve the prompt & stand on the matching pad first!"
            MechanicGroup.TERRITORY_PAINT ->
                if (bn) "বোর্ডে ঘুরে ও ACT চেপে সবচেয়ে বেশি টাইল নিজের রঙে রাঙান!" else "Paint the floor tiles in your player color!"
            MechanicGroup.ZONE_CAPTURE ->
                if (bn) "কমান্ড জোনের ভেতরে অবস্থান নিয়ে পয়েন্ট অর্জন করুন!" else "Hold the command zones & knock challengers out!"
            MechanicGroup.WALL_TRAIL_TRAP ->
                if (bn) "পেছনে এনার্জি দেয়াল তৈরি করে প্রতিপক্ষকে আটকে ফেলুন!" else "Leave barrier trails to box in your opponents!"
            MechanicGroup.REACTION_TAP ->
                if (bn) "সিগন্যাল সবুজ হওয়া মাত্রই সবার আগে STRIKE! ট্যাপ করুন!" else "Wait for GREEN signal, then tap STRIKE! immediately!"
            MechanicGroup.GOLF_PUTT ->
                if (bn) "সঠিক শক্তিতে শট নিয়ে গলফ বল হোলে ফেলুন!" else "Aim carefully & putt the ball into the flag hole!"
            MechanicGroup.DISC_DODGEBALL ->
                if (bn) "ডিস্ক/বল কুড়িয়ে প্রতিপক্ষকে হিট করুন এবং শট ডজ করুন!" else "Throw ricocheting discs/balls & dodge enemy shots!"
            MechanicGroup.SHAPE_MORPH_CHASE ->
                if (bn) "● হারায় ▲, ▲ হারায় ■, ■ হারায় ● — নিজের শিকারকে ধাওয়া করুন!" else "● beats ▲, ▲ beats ■, ■ beats ● — chase your prey!"
        }
    }

    fun playerControlsEntityDescription(lang: AppLanguage): String {
        val bn = lang == AppLanguage.BANGLA
        return when (mechanicGroup) {
            MechanicGroup.CIRCUIT_RACING, MechanicGroup.HIGHWAY_DODGE ->
                if (bn) "রেসিং কার (স্টিয়ারিং, গ্যাস ও ব্রেক)" else "Top-Down Race Car (Steering, Throttle & Brake)"
            MechanicGroup.CASTLE_SIEGE, MechanicGroup.SHOOT_TARGETS ->
                if (bn) "কমব্যাট ট্যাংক / ব্লাস্টার (মুভমেন্ট ও রোটেটিং টারেট)" else "Armored Tank / Blaster (Chassis + Rotating Turret)"
            MechanicGroup.BALL_SPORTS, MechanicGroup.PENALTY_DUEL, MechanicGroup.BASKET_SHOOT,
            MechanicGroup.PADDLE_DEFENSE, MechanicGroup.BOWLING_ROLL, MechanicGroup.GOLF_PUTT ->
                if (bn) "স্পোর্টস অ্যাথলেট (ড্রিবল, শট পাওয়ার ও স্প্রিন্ট)" else "Sports Athlete / Striker (Dribble, Shot Power & Sprint)"
            MechanicGroup.BRAWL_KNOCKBACK ->
                if (bn) "এরিনা ফাইটার (মিলি অ্যাটাক, নকব্যাক ও ডজ)" else "Arena Brawler (Melee Strike, Knockback & Dodge)"
            else ->
                if (bn) "ট্যাকটিক্যাল রানার (মুভমেন্ট, জাম্প ও অ্যাকশন)" else "Tactical Contestant (Movement, Jump & Action)"
        }
    }
}

enum class MatchMode(val labelEn: String, val labelBn: String) {
    PVP("Player vs Player", "প্লেয়ার বনাম প্লেয়ার"),
    VS_BOT("VS Bot Mode", "বনাম বট মোড");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) labelBn else labelEn
}

/**
 * Fair Bot Difficulty with realistic speed scaling and natural human-like reaction delays.
 * - EASY: 75%–90% speed, 500ms–900ms reaction delay
 * - NORMAL: 90%–105% speed, 250ms–500ms reaction delay
 * - HARD: 100%–115% speed, 120ms–300ms reaction delay (wins via smart positioning & tactics)
 */
enum class BotDifficulty(
    val labelEn: String,
    val labelBn: String,
    val descEn: String,
    val descBn: String,
    val minSpeedScale: Float,
    val maxSpeedScale: Float,
    val minReactionDelaySec: Float,
    val maxReactionDelaySec: Float
) {
    EASY(
        labelEn = "Easy",
        labelBn = "সহজ",
        descEn = "75–90% speed • 500–900ms reaction • Makes occasional mistakes",
        descBn = "৭৫–৯০% গতি • ৫০০–৯০০ মি.সে. প্রতিক্রিয়া • সহজ মুভমেন্ট",
        minSpeedScale = 0.75f,
        maxSpeedScale = 0.90f,
        minReactionDelaySec = 0.50f,
        maxReactionDelaySec = 0.90f
    ),
    NORMAL(
        labelEn = "Normal",
        labelBn = "সাধারণ",
        descEn = "90–105% speed • 250–500ms reaction • Balanced tactics",
        descBn = "৯০–১০৫% গতি • ২৫০–৫০০ মি.সে. প্রতিক্রিয়া • ভারসাম্যপূর্ণ কৌশল",
        minSpeedScale = 0.90f,
        maxSpeedScale = 1.05f,
        minReactionDelaySec = 0.25f,
        maxReactionDelaySec = 0.50f
    ),
    HARD(
        labelEn = "Hard",
        labelBn = "কঠিন",
        descEn = "100–115% speed • 120–300ms reaction • Smart positioning & aim",
        descBn = "১০০–১১৫% গতি • ১২০–৩০০ মি.সে. প্রতিক্রিয়া • উন্নত কৌশল ও পজিশনিং",
        minSpeedScale = 1.00f,
        maxSpeedScale = 1.15f,
        minReactionDelaySec = 0.12f,
        maxReactionDelaySec = 0.30f
    );

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
    val winner: PlayerId?,
    val playerResults: List<PlayerResult>,
    val durationPlayedSeconds: Int,
    val finishReasonEn: String,
    val finishReasonBn: String
) {
    fun finishReason(lang: AppLanguage): String =
        if (lang == AppLanguage.BANGLA) finishReasonBn else finishReasonEn
}
