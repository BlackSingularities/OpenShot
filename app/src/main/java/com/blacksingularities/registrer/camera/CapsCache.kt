package com.blacksingularities.registrer.camera

import android.content.Context
import android.os.Build
import android.util.Range
import com.blacksingularities.registrer.BuildConfig
import com.blacksingularities.registrer.data.PhotoFormat
import com.blacksingularities.registrer.data.VideoResolution
import org.json.JSONArray
import org.json.JSONObject

/**
 * Zapamiętane możliwości aparatów: sondowanie wszystkich obiektywów (zwłaszcza profili
 * zwolnionego tempa) trwa przy starcie nawet kilka sekund. Wynik zapisany na urządzeniu
 * pozwala związać kamerę od razu; odświeżany jest w tle po starcie. Aktualizacja systemu
 * lub aplikacji unieważnia pamięć.
 */
class CapsCache(context: Context) {
    private val prefs = context.getSharedPreferences("caps_cache", Context.MODE_PRIVATE)
    private val stamp = Build.FINGERPRINT + "|" + BuildConfig.VERSION_CODE

    fun load(facing: Int): CameraCaps? {
        if (prefs.getString("stamp", null) != stamp) return null
        val raw = prefs.getString("caps_$facing", null) ?: return null
        return runCatching { decode(JSONObject(raw)) }.getOrNull()
    }

    fun save(facing: Int, caps: CameraCaps) {
        runCatching {
            prefs.edit()
                .putString("stamp", stamp)
                .putString("caps_$facing", encode(caps).toString())
                .apply()
        }
    }

    private fun range(r: Range<Int>) = JSONArray().put(r.lower).put(r.upper)
    private fun rangeOf(a: JSONArray) = Range(a.getInt(0), a.getInt(1))

    private fun encode(c: CameraCaps): JSONObject = JSONObject().apply {
        put("hasFront", c.hasFront)
        put("hasBack", c.hasBack)
        put("night", c.nightExtension)
        put("bokeh", c.bokehExtension)
        put("hdr", c.hdrExtension)
        put("retouch", c.retouchExtension)
        put("formats", JSONArray(c.photoFormats.map { it.name }))
        put("zsl", c.zsl)
        put("manual", c.manualSensor)
        c.isoRange?.let { put("iso", JSONArray().put(it.first).put(it.last)) }
        c.exposureRange?.let { put("exp", JSONArray().put(it.first).put(it.last)) }
        put("minFocus", c.minFocusDistance.toDouble())
        put("flash", c.hasFlash)
        put("videoRes", JSONArray(c.videoResolutions.map { it.name }))
        put("hlg", c.videoHlg)
        put("stab", c.videoStabilization)
        put("fps60", c.fps60)
        put("fps24", c.fps24)
        put("slowRanges", JSONArray(c.slowMoRanges.map { range(it) }))
        put("slowProfiles", JSONArray(c.slowMoProfiles.map { JSONObject().put("res", it.resolution.name).put("range", range(it.frameRate)) }))
        put("lowLight", c.lowLightBoost)
        put("lensZooms", JSONArray(c.lensZooms.map { it.toDouble() }))
        put("maxZoom", c.maxZoom.toDouble())
        put("aux", JSONArray(c.auxLenses.map { JSONObject().put("id", it.cameraId).put("factor", it.factor.toDouble()).put("physical", it.physical) }))
        put("summary", c.cameraSummary)
        c.rawCameraId?.let { put("rawId", it) }
        put("rawPhysical", c.rawCameraPhysical)
        put("rawFactor", c.rawCameraFactor.toDouble())
        put("flatProfile", c.flatProfile)
    }

    private fun JSONArray.strings() = List(length()) { getString(it) }
    private fun JSONArray.floats() = List(length()) { getDouble(it).toFloat() }

    private fun decode(j: JSONObject): CameraCaps = CameraCaps(
        hasFront = j.getBoolean("hasFront"),
        hasBack = j.getBoolean("hasBack"),
        nightExtension = j.getBoolean("night"),
        bokehExtension = j.getBoolean("bokeh"),
        hdrExtension = j.getBoolean("hdr"),
        retouchExtension = j.getBoolean("retouch"),
        photoFormats = j.getJSONArray("formats").strings().mapNotNull { n -> PhotoFormat.entries.firstOrNull { it.name == n } }.toSet(),
        zsl = j.getBoolean("zsl"),
        manualSensor = j.getBoolean("manual"),
        isoRange = j.optJSONArray("iso")?.let { it.getInt(0)..it.getInt(1) },
        exposureRange = j.optJSONArray("exp")?.let { it.getLong(0)..it.getLong(1) },
        minFocusDistance = j.getDouble("minFocus").toFloat(),
        hasFlash = j.getBoolean("flash"),
        videoResolutions = j.getJSONArray("videoRes").strings().mapNotNull { n -> VideoResolution.entries.firstOrNull { it.name == n } },
        videoHlg = j.getBoolean("hlg"),
        videoStabilization = j.getBoolean("stab"),
        fps60 = j.getBoolean("fps60"),
        fps24 = j.getBoolean("fps24"),
        slowMoRanges = j.getJSONArray("slowRanges").let { a -> List(a.length()) { rangeOf(a.getJSONArray(it)) } },
        slowMoProfiles = j.getJSONArray("slowProfiles").let { a ->
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                SlowMoProfile(VideoResolution.valueOf(o.getString("res")), rangeOf(o.getJSONArray("range")))
            }
        },
        lowLightBoost = j.getBoolean("lowLight"),
        lensZooms = j.getJSONArray("lensZooms").floats(),
        maxZoom = j.getDouble("maxZoom").toFloat(),
        auxLenses = j.getJSONArray("aux").let { a ->
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                AuxLens(o.getString("id"), o.getDouble("factor").toFloat(), o.getBoolean("physical"))
            }
        },
        cameraSummary = j.optString("summary", ""),
        rawCameraId = if (j.has("rawId")) j.getString("rawId") else null,
        rawCameraPhysical = j.optBoolean("rawPhysical", false),
        rawCameraFactor = j.optDouble("rawFactor", 1.0).toFloat(),
        flatProfile = j.optBoolean("flatProfile", false),
    )
}
