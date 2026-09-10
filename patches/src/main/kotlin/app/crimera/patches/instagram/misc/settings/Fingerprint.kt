/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.settings

import app.crimera.patches.instagram.utils.Constants.ACTIVITY_SETTINGS_STATUS_CLASS
import app.crimera.patches.instagram.utils.Constants.HOOK_FLAGS_DESCRIPTOR
import app.morphe.patcher.Fingerprint

internal object HookFlagsLoadFingerprint : Fingerprint(
    definingClass = HOOK_FLAGS_DESCRIPTOR,
    name = "load",
)

internal object SettingsStatusLoadFingerprint : Fingerprint(
    definingClass = ACTIVITY_SETTINGS_STATUS_CLASS,
    name = "load",
)
// ----------------------

internal object MainFeedFragmentOnCreateFingerprint : Fingerprint(
    name = "onCreate",
    strings = listOf("MainFeedFragment.onCreate"),
)

internal object IgFragmentActivityOnCreate : Fingerprint(
    name = "onCreate",
    definingClass = "Lcom/instagram/base/activity/IgFragmentActivity;",
)

internal object IgFragmentActivityOnResume : Fingerprint(
    name = "onResume",
    definingClass = "Lcom/instagram/base/activity/IgFragmentActivity;",
    parameters = emptyList(),
    returnType = "V",
)

internal object ModalActivityOnCreate : Fingerprint(
    name = "onCreate",
    definingClass = "Lcom/instagram/modal/ModalActivity;",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
    strings = listOf("ModalActivity.onCreate"),
)

internal object ModalActivityOnPostCreate : Fingerprint(
    name = "onPostCreate",
    definingClass = "Lcom/instagram/modal/ModalActivity;",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
)

internal object ModalActivityInitStartingFragment : Fingerprint(
    definingClass = "Lcom/instagram/base/activity/BaseFragmentActivity;",
    parameters = listOf("Landroid/os/Bundle;"),
    returnType = "V",
    strings = listOf("ModalActivity.initializeStartingFragment"),
)
