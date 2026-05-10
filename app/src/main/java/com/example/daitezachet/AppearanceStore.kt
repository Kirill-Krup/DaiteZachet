package com.example.daitezachet

import android.content.Context
import android.graphics.Color
import com.example.daitezachet.engine.EyeStyle
import com.example.daitezachet.engine.Hat
import com.example.daitezachet.engine.PlayerAppearance

object AppearanceStore {
    private const val PREF = "player_appearance"

    fun save(ctx: Context, a: PlayerAppearance) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putInt("bodyColor", a.bodyColor)
            .putString("eyeStyle", a.eyeStyle.name)
            .putString("hat", a.hat.name)
            .apply()
    }

    fun load(ctx: Context): PlayerAppearance {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        val eyeStyle = runCatching {
            EyeStyle.valueOf(p.getString("eyeStyle", EyeStyle.NORMAL.name) ?: EyeStyle.NORMAL.name)
        }.getOrDefault(EyeStyle.NORMAL)

        val hat = runCatching {
            Hat.valueOf(p.getString("hat", Hat.NONE.name) ?: Hat.NONE.name)
        }.getOrDefault(Hat.NONE)

        return PlayerAppearance(
            bodyColor = p.getInt("bodyColor", Color.rgb(230, 230, 255)),
            eyeStyle = eyeStyle,
            hat = hat
        )
    }
}