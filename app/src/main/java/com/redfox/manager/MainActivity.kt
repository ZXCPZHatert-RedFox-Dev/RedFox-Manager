package com.redfox.manager

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity() {

    private lateinit var tvStats: TextView
    private lateinit var tvDeviceSubtitle: TextView
    private lateinit var btnApplyFixes: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dp = resources.displayMetrics.density
        fun px(v: Int): Int = (v * dp).toInt()

        fun roundedBox(bgHex: String, radiusDp: Int = 14): GradientDrawable {
            return GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = radiusDp * dp
            }
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#11111B"))
            setPadding(px(18), px(24), px(18), px(24))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "🦊 RedFox Manager"
            setTextColor(Color.parseColor("#F38BA8"))
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
        }
        root.addView(title)

        tvDeviceSubtitle = TextView(this).apply {
            text = "Universal Android Kernel & System Control"
            setTextColor(Color.parseColor("#A6ADC8"))
            textSize = 14f
            setPadding(0, px(4), 0, px(16))
        }
        root.addView(tvDeviceSubtitle)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBox("#1E1E2E", 16)
            setPadding(px(16), px(16), px(16), px(16))
        }
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = px(18) }

        tvStats = TextView(this).apply {
            text = "Запрос прав root (KernelSU / Magisk)..."
            setTextColor(Color.parseColor("#CDD6F4"))
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setLineSpacing(px(4).toFloat(), 1f)
        }
        card.addView(tvStats)

        fun makeBtn(label: String, bgHex: String, textHex: String, topMarginDp: Int = 8, onClick: () -> Unit): Button {
            return Button(this).apply {
                text = label
                isAllCaps = false
                background = roundedBox(bgHex, 12)
                setTextColor(Color.parseColor(textHex))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setPadding(px(14), px(12), px(14), px(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = px(topMarginDp) }
                setOnClickListener { onClick() }
            }
        }

        card.addView(makeBtn("🔄 Обновить показатели", "#313244", "#CDD6F4", 14) {
            updateStatsAsync()
        })
        root.addView(card, cardParams)

        fun sectionHeader(label: String): TextView {
            return TextView(this).apply {
                text = label
                setTextColor(Color.parseColor("#F5E0DC"))
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, px(6), 0, px(4))
            }
        }

        root.addView(sectionHeader("Профили производительности"))

        root.addView(makeBtn("⚡ RedFox Ultimate (Максимум / Игры)", "#F38BA8", "#11111B") {
            execRootAsync("sysctl -w net.ipv4.tcp_congestion_control=bbr 2>/dev/null; sysctl -w vm.swappiness=60 2>/dev/null; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$g 2>/dev/null; done") {
                toast("Активирован профиль RedFox Ultimate ⚡")
                updateStatsAsync()
            }
        })

        root.addView(makeBtn("⚖️ Balanced (Оптимальный баланс)", "#89B4FA", "#11111B") {
            execRootAsync("sysctl -w net.ipv4.tcp_congestion_control=bbr 2>/dev/null; sysctl -w vm.swappiness=100 2>/dev/null; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo schedutil > \$g 2>/dev/null || echo interactive > \$g 2>/dev/null; done") {
                toast("Активирован профиль Balanced ⚖️")
                updateStatsAsync()
            }
        })

        root.addView(makeBtn("🔋 Battery Saver (Автономность)", "#A6E3A1", "#11111B") {
            execRootAsync("sysctl -w vm.swappiness=130 2>/dev/null; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo conservative > \$g 2>/dev/null || echo powersave > \$g 2>/dev/null; done") {
                toast("Активирован профиль Battery Saver 🔋")
                updateStatsAsync()
            }
        })

        root.addView(sectionHeader("Системные действия и GSI-фиксы"))

        btnApplyFixes = makeBtn("☀️ Применить GSI-фиксы (Яркость и кодеки)", "#313244", "#CDD6F4") {
            execRootAsync("setprop persist.sys.phh.backlight.scale 1; setprop persist.sys.phh.linear_brightness 1; setprop persist.sys.phh.force_sw_codecs true") {
                toast("GSI-фиксы подсветки и кодеков применены ☀️")
                updateStatsAsync()
            }
        }
        root.addView(btnApplyFixes)

        root.addView(makeBtn("🛠️ Перезагрузить в Recovery", "#313244", "#F9E2AF") {
            execRootAsync("reboot recovery") {}
        })

        root.addView(makeBtn("🚀 Перезагрузить в Bootloader (Fastboot)", "#313244", "#FAB387") {
            execRootAsync("reboot bootloader") {}
        })

        scroll.addView(root)
        setContentView(scroll)

        updateStatsAsync()
    }

    private fun updateStatsAsync() {
        Thread {
            val dev = runRootSync("echo \"\$(getprop ro.product.manufacturer) \$(getprop ro.product.model) (\$(getprop ro.product.device))\"")
            val hasPhh = runRootSync("getprop | grep -c 'persist.sys.phh'").toIntOrNull() ?: 0
            val cmd = """
                BL=${'$'}(ls -d /sys/class/backlight/* 2>/dev/null | head -n 1)
                CUR_BL=${'$'}(cat "${'$'}BL/brightness" 2>/dev/null || echo "n/a")
                MAX_BL=${'$'}(cat "${'$'}BL/max_brightness" 2>/dev/null || echo "n/a")
                TEMP_RAW=${'$'}(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || echo 0)
                [ "${'$'}TEMP_RAW" -gt 1000 ] && TEMP=${'$'}((TEMP_RAW / 1000)) || TEMP=${'$'}TEMP_RAW
                ZRAM=${'$'}(cat /sys/block/zram0/comp_algorithm 2>/dev/null | grep -o '\[.*\]' || echo "n/a")

                echo "Ядро: ${'$'}(uname -r)"
                echo "Root: ${'$'}(id -un)"
                echo "Платформа: ${'$'}(getprop ro.board.platform)"
                echo "CPU Gov: ${'$'}(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo n/a)"
                echo "Темп CPU: ${'$'}{TEMP}°C"
                echo "Сеть TCP: ${'$'}(cat /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null || echo n/a)"
                echo "ZRAM: ${'$'}ZRAM"
                echo "Яркость: ${'$'}CUR_BL / ${'$'}MAX_BL"
                echo "Батарея: ${'$'}(cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo n/a)%"
            """.trimIndent()
            val stats = runRootSync(cmd)
            runOnUiThread {
                if (dev.isNotBlank() && !dev.startsWith("Ошибка")) {
                    tvDeviceSubtitle.text = dev
                }
                if (hasPhh == 0) {
                    btnApplyFixes.visibility = View.GONE
                }
                tvStats.text = stats.ifBlank { "Нет доступа root.\nОткройте KernelSU / Magisk и разрешите доступ для RedFox Manager." }
            }
        }.start()
    }

    private fun execRootAsync(command: String, onDone: () -> Unit) {
        Thread {
            runRootSync(command)
            runOnUiThread { onDone() }
        }.start()
    }

    private fun runRootSync(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

val output = reader.readText()
process.waitFor()
output.trim()
} catch (e: Exception) {
"Ошибка: ${e.message}"
}
}
private fun toast(msg: String) {
Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
}
