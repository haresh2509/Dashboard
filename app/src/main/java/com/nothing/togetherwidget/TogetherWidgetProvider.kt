package com.nothing.togetherwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.max

class TogetherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TogetherWidgetProvider::class.java))
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        private const val PREFS_NAME = "nothing_together_prefs"
        private const val KEY_YEAR = "wedding_year"
        private const val KEY_MONTH = "wedding_month"
        private const val KEY_DAY = "wedding_day"

        // 5x7 Dot-Matrix patterns for digits 0-9
        private val DIGIT_MATRIX = mapOf(
            '0' to arrayOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
            '1' to arrayOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
            '2' to arrayOf("01110", "10001", "00001", "00110", "01000", "10000", "11111"),
            '3' to arrayOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
            '4' to arrayOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
            '5' to arrayOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
            '6' to arrayOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
            '7' to arrayOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
            '8' to arrayOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
            '9' to arrayOf("01110", "10001", "10001", "01111", "00001", "00010", "01100")
        )

        fun getWeddingDate(context: Context): LocalDate {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val defaultDate = LocalDate.now().minusYears(6).minusMonths(8).minusDays(12)
            val y = prefs.getInt(KEY_YEAR, defaultDate.year)
            val m = prefs.getInt(KEY_MONTH, defaultDate.monthValue)
            val d = prefs.getInt(KEY_DAY, defaultDate.dayOfMonth)
            return try {
                LocalDate.of(y, m, d)
            } catch (e: Exception) {
                defaultDate
            }
        }

        fun saveWeddingDate(context: Context, date: LocalDate) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putInt(KEY_YEAR, date.year)
                .putInt(KEY_MONTH, date.monthValue)
                .putInt(KEY_DAY, date.dayOfMonth)
                .apply()
        }

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TogetherWidgetProvider::class.java))
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val weddingDate = getWeddingDate(context)
            val today = LocalDate.now()

            val totalDays = max(0L, ChronoUnit.DAYS.between(weddingDate, today))
            val period = if (!weddingDate.isAfter(today)) {
                Period.between(weddingDate, today)
            } else {
                Period.ZERO
            }
            val ymdText = "${period.years}Y ${period.months}M ${period.days}D"

            val bitmap = renderNothingWidgetBitmap(totalDays.toString(), ymdText)
            val views = RemoteViews(context.packageName, R.layout.widget_together)
            views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

            // Tap anywhere on widget to set wedding date
            val configIntent = Intent(context, ConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_canvas_image, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun renderNothingWidgetBitmap(daysStr: String, ymdStr: String): Bitmap {
            val width = 960
            val height = 480
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            val pad = 8f
            val cardRect = RectF(pad, pad, width - pad, height - pad)
            val cornerRadius = 72f

            // 1. Near-black rounded card background
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    Color.parseColor("#141418"),
                    Color.parseColor("#0C0C0E"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

            // Subtle border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = Color.argb(30, 255, 255, 255)
            }
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

            // 2. Top Left: "TOGETHER SINCE"
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(215, 245, 245, 247)
                typeface = Typeface.MONOSPACE
                textSize = 26f
                letterSpacing = 0.22f
            }
            canvas.drawText("TOGETHER SINCE", 64f, 82f, labelPaint)

            // 3. Top Right: Red Heart ♥ and White Infinity ∞
            val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF1E27")
                typeface = Typeface.DEFAULT_BOLD
                textSize = 38f
                textAlign = Paint.Align.RIGHT
            }
            val infinityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.MONOSPACE
                textSize = 44f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("∞", width - 64f, 84f, infinityPaint)
            canvas.drawText("♥", width - 118f, 82f, heartPaint)

            // 4. Centre Number: 5x7 Dot-Matrix with faint unlit dots behind digits + "DAYS" beside it
            val charCols = 5
            val charRows = 7
            val gapCols = 1
            val padCols = 1
            val padRows = 1

            val totalCols = padCols * 2 + daysStr.length * charCols + max(0, daysStr.length - 1) * gapCols
            val totalRows = charRows + padRows * 2

            val pitch = 19f
            val dotRadius = 6.6f
            val matrixStartX = 64f
            val matrixHeight = totalRows * pitch
            val matrixStartY = (height - matrixHeight) / 2f + 4f

            val unlitDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(24, 255, 255, 255)
                style = Paint.Style.FILL
            }
            val litDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }

            val litMap = Array(totalRows) { BooleanArray(totalCols) }
            daysStr.forEachIndexed { idx, ch ->
                val pattern = DIGIT_MATRIX[ch] ?: DIGIT_MATRIX['0']!!
                val startCol = padCols + idx * (charCols + gapCols)
                for (r in 0 until charRows) {
                    for (c in 0 until charCols) {
                        if (pattern[r][c] == '1') {
                            litMap[padRows + r][startCol + c] = true
                        }
                    }
                }
            }

            for (r in 0 until totalRows) {
                for (c in 0 until totalCols) {
                    val cx = matrixStartX + c * pitch + pitch / 2f
                    val cy = matrixStartY + r * pitch + pitch / 2f
                    canvas.drawCircle(
                        cx,
                        cy,
                        dotRadius,
                        if (litMap[r][c]) litDotPaint else unlitDotPaint
                    )
                }
            }

            // "DAYS" beside the dot-matrix number
            val daysLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(210, 245, 245, 247)
                typeface = Typeface.MONOSPACE
                textSize = 34f
                letterSpacing = 0.22f
            }
            val matrixEndX = matrixStartX + totalCols * pitch + 28f
            val daysBaselineY = matrixStartY + (totalRows - 1) * pitch
            canvas.drawText("DAYS", matrixEndX, daysBaselineY, daysLabelPaint)

            // 5. Bottom Left: Years, Months, Days (e.g. 6Y 8M 12D)
            val ymdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(235, 245, 245, 247)
                typeface = Typeface.MONOSPACE
                textSize = 30f
                letterSpacing = 0.18f
            }
            canvas.drawText(ymdStr, 64f, height - 60f, ymdPaint)

            // 6. Bottom Right: Small red dot
            val redDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF1E27")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(width - 74f, height - 70f, 12f, redDotPaint)

            return bmp
        }
    }
}
