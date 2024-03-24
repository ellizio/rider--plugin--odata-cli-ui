package com.jetbrains.rider.plugins.odatacliui.actions

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.*
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.rider.plugins.odatacliui.dialogs.CliDialog
import com.jetbrains.rider.plugins.odatacliui.extensions.entityForAction
import com.jetbrains.rider.plugins.odatacliui.extensions.toMetadata
import com.jetbrains.rider.plugins.odatacliui.models.CliDialogModel
import com.jetbrains.rider.plugins.odatacliui.terminal.BatchCommandLineExecutor
import com.jetbrains.rider.plugins.odatacliui.toolwindows.CliToolWindowManager
import com.jetbrains.rider.projectView.actions.isProjectModelReady
import com.jetbrains.rider.projectView.workspace.isProject
import com.jetbrains.rider.projectView.workspace.isWebReferenceFolder

class OpenCliDialogAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val actionMetadata = event.toMetadata() ?: return

        val dialogModel = CliDialogModel(project, actionMetadata)

        // launchOnUi is available since 233.11799.241
        // RD-2023.3 has build number 233.11799.261
        @Suppress("MissingRecentApi")
        project.lifetime.launchOnUi {
            val dialog = CliDialog(dialogModel)
            if (dialog.showAndGet()) {
                withBackgroundContext {
                    executeCommand(project, dialogModel)
                }
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val entity = event.entityForAction
        if (entity == null) {
            event.presentation.isVisible = false
            return
        }
        event.presentation.isEnabled = event.project?.isProjectModelReady() ?: false
        event.presentation.isVisible = entity.isWebReferenceFolder() || entity.isProject()
    }

    private suspend fun executeCommand(project: Project, model: CliDialogModel)
    {
        var consoleView: ConsoleView? = null
        withUiContext {
            consoleView = CliToolWindowManager.getInstance(project).instantiateConsole()
        }

        val executor = BatchCommandLineExecutor(project, model.buildCommand(), consoleView!!)
        executor.execute()

        VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
    }
}