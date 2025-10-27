package com.example.appinstagramreelsblocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class ReelsBlockerService : AccessibilityService() {

    private lateinit var settingsManager: SettingsManager
    private var isInstagramActive = false
    private var lastCheckTime = 0L
    private val CHECK_DELAY = 1500L

    companion object {
        private const val TAG = "ReelsBlockerService"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"

        // IDs ESPECÍFICOS del visor de Reels (cuando ESTÁS viendo reels)
        private val REELS_VIEWER_IDS = listOf(
            "clips_viewer_view_pager",
            "clips_viewer_root",
            "reel_viewer_page",
            "reels_viewer_fragment_container",
            "clips_viewer_fragment",
            "reel_feed_item"
        )

        // IGNORAR estos - son solo la pestaña/navegación
        private val IGNORE_DESCRIPTIONS = listOf(
            "bandeja de reels",  // ← Este es el problema
            "reels tab",
            "reels button",
            "ir a reels"
        )
    }

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        Log.d(TAG, "Service created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (!settingsManager.isBlockingEnabled()) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        if (packageName != INSTAGRAM_PACKAGE) {
            isInstagramActive = false
            return
        }

        isInstagramActive = true

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastCheckTime < CHECK_DELAY) {
                    return
                }
                lastCheckTime = currentTime

                checkForReels()
            }
        }
    }

    private fun checkForReels() {
        try {
            val rootNode = rootInActiveWindow ?: return

            val isReelsDetected = isInReelsViewer(rootNode)

            if (isReelsDetected) {
                Log.d(TAG, "✓ REELS VIEWER DETECTED - Blocking!")
                handleReelsDetected()
            } else {
                Log.d(TAG, "✗ Not in Reels viewer")
            }

            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
        }
    }

    private fun isInReelsViewer(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        try {
            // Buscar SOLO por viewId específico del visor
            return searchForViewerId(node)
        } catch (e: Exception) {
            Log.e(TAG, "Error in detection: ${e.message}")
            return false
        }
    }

    private fun searchForViewerId(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        try {
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

            // IGNORAR si es solo la pestaña/navegación
            val shouldIgnore = IGNORE_DESCRIPTIONS.any { ignore ->
                contentDesc.contains(ignore)
            }

            if (shouldIgnore) {
                Log.d(TAG, "⊘ Ignoring navigation element: $contentDesc")
                return false
            }

            // Buscar ViewID específico del VISOR de Reels
            val hasReelsViewerId = REELS_VIEWER_IDS.any { id ->
                viewId.contains(id)
            }

            if (hasReelsViewerId) {
                Log.d(TAG, "✓ Found Reels VIEWER: $viewId")
                return true
            }

            // Buscar en hijos
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val found = searchForViewerId(child)
                    child.recycle()
                    if (found) return true
                }
            }

            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun handleReelsDetected() {
        if (settingsManager.isInAllowedTimeWindow()) {
            Log.d(TAG, "In allowed time window - not blocking")
            return
        }

        Log.d(TAG, "Showing blocker overlay")
        showBlockerOverlay()
    }

    private fun showBlockerOverlay() {
        try {
            val intent = Intent(this, BlockerOverlayActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay: ${e.message}", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}