package com.rimor.minipape

import android.app.ActivityOptions
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CoverWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val intent = Intent(context, CoverPreviewActivity::class.java)
        val options = ActivityOptions.makeBasic().apply { launchDisplayId = COVER_DISPLAY_ID }.toBundle()
        val pendingIntent = PendingIntent.getActivity(
            context,
            7,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            options,
        )
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.cover_widget)
            views.setOnClickPendingIntent(R.id.cover_widget_root, pendingIntent)
            manager.updateAppWidget(id, views)
        }
    }

    private companion object { const val COVER_DISPLAY_ID = 1 }
}

