/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.settings

import app.crimera.patches.instagram.entity.developerOptions.developerOptionsEntity
import app.crimera.patches.instagram.entity.dialogbox.instagramDialogBoxEntity
import app.crimera.patches.instagram.entity.instagramButton.instagramButtonEntity
import app.crimera.patches.instagram.entity.profileinfo.profileInfoEntity
import app.crimera.patches.instagram.entity.userdata.userDataEntity
import app.crimera.patches.instagram.links.validateLinks.validateLinksPatch
import app.crimera.patches.instagram.misc.actionBar.mainFeedActionBarButton.hideHomeActionButtonsPatch
import app.crimera.patches.instagram.misc.actionBar.mainFeedActionBarButton.mainFeedActionBarButtonPatch
import app.crimera.patches.instagram.misc.actionBar.userProfileActionBarButton.userProfileActionBarButtonPatch
import app.crimera.patches.instagram.misc.buildExpiredPopup.removeBuildExpiredPopupPatch
import app.crimera.patches.instagram.misc.extension.hooks.instagramInitHook
import app.crimera.patches.instagram.misc.extension.sharedExtensionPatch
import app.crimera.patches.instagram.misc.hookFlags.hookFlagsPatch
import app.crimera.patches.instagram.misc.notification.fixDirectNotificationActionCrashPatch
import app.crimera.patches.instagram.misc.notification.fixNotificationRegistrationCrashPatch
import app.crimera.patches.instagram.misc.userProfile.preserveUnknownFriendshipStatusPatch
import app.crimera.patches.instagram.misc.userProfile.userProfileButtonPatch
import app.crimera.patches.instagram.utils.Constants.COMPATIBILITY_INSTAGRAM
import app.crimera.patches.instagram.utils.Constants.CONSTANTS_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.LOAD_FLAGS_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.PATCHES_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.SSTS_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.ACTIVITY_SETTINGS_CLASS
import app.crimera.patches.shared.parameterRegisterStart
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.addAppResources
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val settingsPatch =
    bytecodePatch(
        name = "Add settings",
        description = "Adds settings to control preferences are patching",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_INSTAGRAM)
        dependsOn(
            sharedExtensionPatch,
            addSettingsActivityPatch,
            nativeSettingsSwitchStylePatch,
            mainFeedActionBarButtonPatch,
            hideHomeActionButtonsPatch,
            userProfileActionBarButtonPatch,
            userDataEntity,
            userProfileButtonPatch,
            hookFlagsPatch,
            fixNotificationRegistrationCrashPatch,
            fixDirectNotificationActionCrashPatch,
            validateLinksPatch,
            removeBuildExpiredPopupPatch,
            preserveUnknownFriendshipStatusPatch,
            profileInfoEntity,
            instagramDialogBoxEntity,
            instagramButtonEntity,
            developerOptionsEntity,
            addResourcesPatch,
        )
        execute {
            addAppResources("shared")
            addAppResources("instagram")

            // Returning from a modal must restore the activity used for native themes.
            for (fingerprint in listOf(IgFragmentActivityOnCreate, IgFragmentActivityOnResume)) {
                fingerprint.method.addInstruction(
                    fingerprint.instructionMatches.first().index,
                    """
                    invoke-static {p0}, Lapp/morphe/extension/shared/Utils;->setActivity(Landroid/app/Activity;)V
                    """.trimIndent(),
                )
            }

            ModalActivityOnCreate.method.apply {
                val overrideIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_SUPER &&
                        getReference<MethodReference>()?.name == "onCreate"
                } + 1

                val thisRegister = parameterRegisterStart(this)
                val freeRegister = findFreeRegister(overrideIndex, thisRegister)

                addInstructionsWithLabels(
                    overrideIndex,
                    """
                        invoke-static/range { v$thisRegister .. v$thisRegister }, $ACTIVITY_SETTINGS_CLASS/ActivityHook;->create(Landroid/app/Activity;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :not_piko
                        return-void
                        :not_piko
                        nop
                    """,
                )
            }

            ModalActivityOnPostCreate.method.apply {
                val overrideIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_SUPER &&
                        getReference<MethodReference>()?.name == "onPostCreate"
                } + 1

                val thisRegister = parameterRegisterStart(this)
                val freeRegister = findFreeRegister(overrideIndex, thisRegister)

                addInstructionsWithLabels(
                    overrideIndex,
                    """
                        invoke-static/range { v$thisRegister .. v$thisRegister }, $ACTIVITY_SETTINGS_CLASS/ActivityHook;->isPiko(Landroid/app/Activity;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :not_piko_post_create
                        return-void
                        :not_piko_post_create
                        nop
                    """,
                )
            }

            ModalActivityInitStartingFragment.method.apply {
                val thisRegister = parameterRegisterStart(this)
                val freeRegister = findFreeRegister(0, thisRegister)

                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static/range { v$thisRegister .. v$thisRegister }, $ACTIVITY_SETTINGS_CLASS/ActivityHook;->isPiko(Landroid/app/Activity;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :not_piko_init
                        return-void
                        :not_piko_init
                        nop
                    """,
                )
            }

            instagramInitHook.fingerprint.method.apply {

                val firstInvokeSuperIndex = indexOfFirstInstructionOrThrow(Opcode.INVOKE_SUPER)
                val contextRegister = getInstruction(firstInvokeSuperIndex).registersUsed[0]
                val freeRegister = findFreeRegister(firstInvokeSuperIndex, listOf(firstInvokeSuperIndex))

                addInstructions(
                    firstInvokeSuperIndex + 1,
                    """
                    new-instance v$freeRegister, Lapp/morphe/extension/crimera/CustomCrashHandler;
                    invoke-direct {v$freeRegister, v$contextRegister}, Lapp/morphe/extension/crimera/CustomCrashHandler;-><init>(Landroid/content/Context;)V
                    invoke-static {v$freeRegister}, Ljava/lang/Thread;->setDefaultUncaughtExceptionHandler(Ljava/lang/Thread${'$'}UncaughtExceptionHandler;)V
                    
                    ${SSTS_DESCRIPTOR.format("load")}
                    ${LOAD_FLAGS_DESCRIPTOR.format("load")}
                    ${LOAD_FLAGS_DESCRIPTOR.format("load")}
                    invoke-static {}, $CONSTANTS_DESCRIPTOR/Constants;->load()V
                    """.trimIndent(),
                )
            }

            // For welcome message.
            MainFeedFragmentOnCreateFingerprint.matchSingle().let {
                val contextResultIndex = it.instructionMatches.last().index
                val contextRegister = it.instructionMatches.last().getInstruction<OneRegisterInstruction>().registerA

                it.method.addInstruction(
                    contextResultIndex + 1,
                    "invoke-static/range {v$contextRegister .. v$contextRegister}, " +
                        "$PATCHES_DESCRIPTOR/WelcomeMessage;->openWelcomeMessage(Landroid/content/Context;)V",
                )
            }
        }
    }
