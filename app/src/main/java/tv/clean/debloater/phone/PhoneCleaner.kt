package tv.clean.debloater.phone

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/** Estado de Shizuku en este celular. */
enum class ShizukuState { MISSING, STOPPED, DENIED, READY }

data class PhoneRow(val app: PhoneApp, val installed: Boolean)

/** Limpia ESTE celular con Shizuku: sin PC y sin root. Todo se puede devolver. */
class PhoneCleaner(private val context: Context) {

    @Volatile
    private var shell: IPhoneShell? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            shell = IPhoneShell.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shell = null
        }
    }

    private val args by lazy {
        Shizuku.UserServiceArgs(ComponentName(context.packageName, PhoneShell::class.java.name))
            .tag("sgalfa-phone")
            .processNameSuffix("shell")
            .daemon(false)
            .version(1)
    }

    fun state(): ShizukuState {
        val alive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (alive) {
            val granted = runCatching {
                !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
            return if (granted) ShizukuState.READY else ShizukuState.DENIED
        }
        val installed = runCatching {
            context.packageManager.getPackageInfo(SHIZUKU_PKG, 0)
        }.isSuccess
        return if (installed) ShizukuState.STOPPED else ShizukuState.MISSING
    }

    fun requestPermission() {
        runCatching { Shizuku.requestPermission(REQUEST) }
    }

    /** Apps de la lista que vienen en este celular (puestas o ya quitadas). */
    @Suppress("DEPRECATION")
    suspend fun scan(): List<PhoneRow> = withContext(Dispatchers.IO) {
        PhoneCatalog.apps.mapNotNull { app ->
            runCatching {
                val info = context.packageManager.getApplicationInfo(app.pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
                PhoneRow(app, info.flags and ApplicationInfo.FLAG_INSTALLED != 0)
            }.getOrNull()
        }
    }

    /** Quita o devuelve. Devuelve los paquetes que se pudieron cambiar. */
    suspend fun apply(pkgs: List<String>, remove: Boolean, onEach: (String, Boolean) -> Unit): Int =
        withContext(Dispatchers.IO) {
            if (shell == null) runCatching { Shizuku.bindUserService(args, connection) }
            repeat(40) { if (shell == null) delay(150) }
            val s = shell ?: return@withContext -1
            var ok = 0
            for (pkg in pkgs.filter(PhoneCatalog::isAllowed)) {
                val cmd = if (remove) PhoneCatalog.removeCommand(pkg) else PhoneCatalog.restoreCommand(pkg)
                val out = runCatching { s.exec(cmd) }.getOrDefault("")
                val done = out.contains("Success") || out.contains("installed for user")
                if (done) ok++
                onEach(pkg, done)
            }
            ok
        }

    fun close() {
        runCatching { Shizuku.unbindUserService(args, connection, true) }
        shell = null
    }

    companion object {
        const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
        const val REQUEST = 4713
    }
}
