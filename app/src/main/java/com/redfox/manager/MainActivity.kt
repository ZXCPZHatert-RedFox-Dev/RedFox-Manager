package com.redfox.manager

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var tvStats: TextView
    private lateinit var tvDeviceSubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStats = findViewById(R.id.tvStats)
        tvDeviceSubtitle = findViewById(R.id.tvDeviceSubtitle)

        val btnApplyFixes = findViewById<Button>(R.id.btnApplyFixes)
        val hasPhh = runRoot("getprop | grep -c 'persist.sys.phh'").toIntOrNull() ?: 0
        if (hasPhh == 0) {
            btnApplyFixes.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { updateStats() }

        findViewById<Button>(R.id.btnGaming).setOnClickListener {
            runRoot("sysctl -w net.ipv4.tcp_congestion_control=bbr 2>/dev/null; sysctl -w vm.swappiness=60 2>/dev/null; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$g 2>/dev/null; done")
            toast("Активирован профиль RedFox Ultimate ⚡")
            updateStats()
        }

        findViewById<Button>(R.id.btnBalanced).setOnClickListener {
            runRoot("sysctl -w net.ipv4.tcp_congestion_control=bbr 2>/dev/null; sysctl -w vm.swappiness=100 2>/dev/null; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo schedutil > \$g 2>/dev/null || echo interactive > \$g 2>/dev/null; done")
            toast("Активирован профиль Balanced ⚖️")
            updateStats()
        }

        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            runRoot("sysctl -w vm.swappiness=130 2>/dev/null; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo conservative > \$g 2>/dev/null || echo powersave > \$g 2>/dev/null; done")
            toast("Активирован профиль Battery Saver 🔋")
            updateStats()
        }

        btnApplyFixes.setOnClickListener {
            runRoot("setprop persist.sys.phh.backlight.scale 1; setprop persist.sys.phh.linear_brightness 1; setprop persist.sys.phh.force_sw_codecs true")
            toast("GSI-фиксы подсветки и кодеков применены ☀️")
            updateStats()
        }

        findViewById<Button>(R.id.btnRecovery).setOnClickListener {
            runRoot("reboot recovery")
        }

        findViewById<Button>(R.id.btnBootloader).setOnClickListener {
            runRoot("reboot bootloader")
        }

        updateStats()
    }

    private fun updateStats() {
        val deviceName = runRoot("echo \"\$(getprop ro.product.manufacturer) \$(getprop ro.product.model) (\$(getprop ro.product.device))\"")
        if (deviceName.isNotBlank() && !deviceName.startsWith("Ошибка")) {
            tvDeviceSubtitle.text = deviceName
        }

        val cmd = """
            BL=\$(ls -d /sys/class/backlight/* 2>/dev/null | head -n 1)
            CUR_BL=\$(cat "\$BL/brightness" 2>/dev/null || echo "n/a")
            MAX_BL=\$(cat "\$BL/max_brightness" 2>/dev/null || echo "n/a")
            TEMP_RAW=\$(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || echo 0)
            [ "\$TEMP_RAW" -gt 1000 ] && TEMP=\$((TEMP_RAW / 1000)) || TEMP=\$TEMP_RAW
            ZRAM=\$(cat /sys/block/zram0/comp_algorithm 2>/dev/null | grep -o '\[.*\]' || echo "n/a")

            echo "Ядро: \$(uname -r)"
            echo "Root: \$(id -un)"
            echo "Платформа: \$(getprop ro.board.platform)"
            echo "CPU Gov: \$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo n/a)"
            echo "Темп CPU: \${TEMP}°C"
            echo "Сеть TCP: \$(cat /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null || echo n/a)"
            echo "ZRAM: \$ZRAM"
            echo "Яркость: \$CUR_BL / \$MAX_BL"
            echo "Батарея: \$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo n/a)%"
        """.trimIndent()
        val out = runRoot(cmd)
        tvStats.text = out.ifBlank { "Нет доступа root (выдайте права в KernelSU / Magisk / APatch)" }
    }

    private fun runRoot(command: String): String {
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
