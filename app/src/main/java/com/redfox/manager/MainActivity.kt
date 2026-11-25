package com.redfox.manager

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var tvStats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStats = findViewById(R.id.tvStats)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { updateStats() }

        findViewById<Button>(R.id.btnGaming).setOnClickListener {
            runRoot("sysctl -w net.ipv4.tcp_congestion_control=bbr; sysctl -w vm.swappiness=60; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$g 2>/dev/null; done")
            toast("Активирован профиль RedFox Ultimate ⚡")
            updateStats()
        }

        findViewById<Button>(R.id.btnBalanced).setOnClickListener {
            runRoot("sysctl -w net.ipv4.tcp_congestion_control=bbr; sysctl -w vm.swappiness=100; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo schedutil > \$g 2>/dev/null; done")
            toast("Активирован профиль Balanced ⚖️")
            updateStats()
        }

        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            runRoot("sysctl -w vm.swappiness=130; for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo powersave > \$g 2>/dev/null; done")
            toast("Активирован профиль Battery Saver 🔋")
            updateStats()
        }

        findViewById<Button>(R.id.btnApplyFixes).setOnClickListener {
            runRoot("setprop persist.sys.phh.backlight.scale 1; setprop persist.sys.phh.linear_brightness 1; setprop persist.sys.phh.force_sw_codecs true")
            toast("Фиксы подсветки 4095 и видеокодеков применены ☀️")
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
        val cmd = """
            echo "Ядро: $(uname -r)"
            echo "Root: $(id -un)"
            echo "CPU Gov: $(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo n/a)"

echo "Темп CPU: $(( $(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || echo 0) / 1000 ))°C"
echo "Сеть TCP: $(cat /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null)"
echo "Яркость: $(cat /sys/class/backlight/sprd_backlight/brightness 2>/dev/null) / $(cat /sys/class/backlight/sprd_backlight/max_brightness 2>/dev/null)"
echo "Батарея: $(cat /sys/class/power_supply/battery/capacity 2>/dev/null)%"
""".trimIndent()
val out = runRoot(cmd)
tvStats.text = out.ifBlank { "Нет доступа root (выдайте права в KernelSU)" }
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
