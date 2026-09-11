package com.fixmyphone.executor

/**
 * Native-side mirror of src/actions/ActionSchema.js.
 * Kept as a plain enum + required-key map so ActionValidator (Kotlin) and
 * ActionValidator (JS) can never silently drift — any action arriving from
 * JS that isn't in this enum is rejected before dispatch.
 */
enum class ActionType {
    RESOLVE_CONTACT,
    CALL,
    SPEAKER_ON,
    SPEAKER_OFF,
    OPEN_APP,
    TAP,
    LONG_PRESS,
    TYPE_TEXT,
    CLEAR_TEXT,
    SWIPE,
    SCROLL,
    SET_VOLUME,
    TOGGLE_WIFI,
    TOGGLE_BLUETOOTH,
    TOGGLE_AIRPLANE_MODE,
    MEDIA_PLAY,
    MEDIA_PAUSE,
    MEDIA_SEEK,
    BACK,
    HOME,
    RECENTS,
    ASK_USER,
    WAIT_FOR_STATE;

    companion object {
        fun fromWire(value: String): ActionType? =
            entries.firstOrNull { it.name == value }
    }
}

data class FixMyPhoneAction(
    val id: String,
    val action: ActionType,
    val params: Map<String, Any?>,
    val requiresConfirmation: Boolean
)

object ActionParamSchema {
    // Required parameter keys per action, mirrors ActionParamSchema.js.
    val required: Map<ActionType, List<String>> = mapOf(
        ActionType.RESOLVE_CONTACT to listOf("name"),
        ActionType.CALL to listOf("contactRef"),
        ActionType.SPEAKER_ON to emptyList(),
        ActionType.SPEAKER_OFF to emptyList(),
        ActionType.OPEN_APP to listOf("appName"),
        ActionType.TAP to listOf("target"),
        ActionType.LONG_PRESS to listOf("target"),
        ActionType.TYPE_TEXT to listOf("target", "text"),
        ActionType.CLEAR_TEXT to listOf("target"),
        ActionType.SWIPE to listOf("direction"),
        ActionType.SCROLL to listOf("direction"),
        ActionType.SET_VOLUME to listOf("level"),
        ActionType.TOGGLE_WIFI to listOf("state"),
        ActionType.TOGGLE_BLUETOOTH to listOf("state"),
        ActionType.TOGGLE_AIRPLANE_MODE to listOf("state"),
        ActionType.MEDIA_PLAY to emptyList(),
        ActionType.MEDIA_PAUSE to emptyList(),
        ActionType.MEDIA_SEEK to listOf("positionMs"),
        ActionType.BACK to emptyList(),
        ActionType.HOME to emptyList(),
        ActionType.RECENTS to emptyList(),
        ActionType.ASK_USER to listOf("question"),
        ActionType.WAIT_FOR_STATE to listOf("condition", "timeoutMs")
    )

    // Mirrors ALWAYS_CONFIRM in ActionSchema.js — enforced natively too,
    // so a compromised or buggy JS layer can't skip confirmation for CALL.
    val alwaysConfirm: Set<ActionType> = setOf(ActionType.CALL)
}
