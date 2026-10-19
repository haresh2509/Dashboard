package com.nothing.togetherwidget

import android.app.Activity
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import java.time.LocalDate

class ConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read the widget ID from the intent if launched from widget placement or tap
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val current = TogetherWidgetProvider.getWeddingDate(this)

        val dialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val selected = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                val today = LocalDate.now()
                val clamped = if (selected.isAfter(today)) today else selected

                TogetherWidgetProvider.saveWeddingDate(this, clamped)
                TogetherWidgetProvider.updateAllWidgets(this)

                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    setResult(RESULT_OK, resultValue)
                }
                finish()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        )

        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.setTitle("Set Wedding Date")
        dialog.setOnCancelListener {
            finish()
        }
        dialog.show()
    }
}
