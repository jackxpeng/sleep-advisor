package com.sleepadvisor.domain.model

enum class CbtWeek(
    val weekNum: Int,
    val displayName: String,
    val instructions: String
) {
    INITIAL_ASSESSMENT(
        -1,
        "Initial Interview",
        "Complete your initial sleep assessment with the advisor."
    ),
    BASELINE(
        0,
        "Baseline Logging",
        "Log your sleep daily for 7 days to establish baseline."
    ),
    WEEK_1(
        1,
        "Changing Thoughts",
        "Challenge negative sleep thoughts and replace with positive ones."
    ),
    WEEK_2(
        2,
        "Sleep Restriction",
        "Limit time in bed to match actual sleep duration."
    ),
    WEEK_3(
        3,
        "Stimulus Control",
        "Strengthen bed-sleep association. Only bed when sleepy."
    ),
    WEEK_4(
        4,
        "Relaxation",
        "Practice relaxation response techniques daily."
    ),
    WEEK_5(
        5,
        "Stress Reduction",
        "Implement cognitive restructuring for daytime stress."
    ),
    WEEK_6(
        6,
        "Sleep-Enhancing Attitudes",
        "Develop long-term healthy sleep attitudes."
    );

    companion object {
        fun fromWeekNum(num: Int): CbtWeek =
            entries.firstOrNull { it.weekNum == num } ?: INITIAL_ASSESSMENT
    }
}
