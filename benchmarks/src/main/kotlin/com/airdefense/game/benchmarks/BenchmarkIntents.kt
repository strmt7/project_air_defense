package com.airdefense.game.benchmarks

import android.content.Intent

internal fun menuIntent(): Intent = Intent(Intent.ACTION_MAIN).apply {
    setClassName(TARGET_PACKAGE, TARGET_ACTIVITY)
    addCategory(Intent.CATEGORY_LAUNCHER)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun battleIntent(): Intent = menuIntent().apply {
    putExtra(EXTRA_LAUNCH_TARGET, BATTLE_TARGET)
    putExtra(EXTRA_BENCHMARK_SEED, BENCHMARK_SEED)
}
