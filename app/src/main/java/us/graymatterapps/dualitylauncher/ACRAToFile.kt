package us.graymatterapps.dualitylauncher

import android.content.Context
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory
import java.io.File
import java.io.FileWriter

class ACRAToFile: ReportSender {
    override fun send(context: Context, errorContent: CrashReportData) {
        val dir = File(context.filesDir, "ACRA")
        if(!dir.exists()) {
            dir.mkdir()
        }

        val file = File(dir, "crash" + System.currentTimeMillis().toString())
        val writer = FileWriter(file)
        val trace = errorContent.get("STACK_TRACE")
        writer.append(trace.toString())
        writer.flush()
        writer.close()
    }
}

class ACRAToFileFactory: ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration): ReportSender {
        return ACRAToFile()
    }
}