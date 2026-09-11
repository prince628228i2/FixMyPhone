package com.fixmyphone.executor

/**
 * Native-side mirror of src/actions/ActionValidator.js. Runs the same check
 * a second time on the Kotlin side so that a bug (or tampering) in the JS
 * layer can never get an unvalidated or under-specified action past this
 * point — nothing reaches an ActionHandler without going through here.
 */
data class ValidationResult(
    val valid: Boolean,
    val reason: String? = null
)

object ActionValidator {

    fun validate(action: FixMyPhoneAction): ValidationResult {
        val requiredParams = ActionParamSchema.required[action.action]
            ?: return ValidationResult(false, "unknown_action:${action.action}")

        for (key in requiredParams) {
            if (!action.params.containsKey(key) || action.params[key] == null) {
                return ValidationResult(false, "missing_param:$key")
            }
        }

        // Belt-and-braces: even if the JS layer forgot to set
        // requiresConfirmation for an always-confirm action, we treat it as
        // requiring confirmation here. A caller that skips confirmation for
        // one of these gets rejected, not silently downgraded.
        if (action.action in ActionParamSchema.alwaysConfirm && !action.requiresConfirmation) {
            return ValidationResult(false, "confirmation_required:${action.action}")
        }

        return ValidationResult(true)
    }
}
