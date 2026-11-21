package com.aistudio.kbeditor.r7v4qx.storage

import android.content.Context
import com.aistudio.kbeditor.r7v4qx.model.KbEditorProject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProjectStorage(context: Context) {
    private val projectFile = File(context.filesDir, "kb_editor_project.json")
    private val libraryFile = File(context.filesDir, "kb_projects_library.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val undoStack = ArrayDeque<KbEditorProject>()
    private val redoStack = ArrayDeque<KbEditorProject>()
    private val maxHistorySteps = 30

    fun loadProject(): KbEditorProject {
        return try {
            if (projectFile.exists()) {
                json.decodeFromString<KbEditorProject>(projectFile.readText())
            } else {
                KbEditorProject()
            }
        } catch (_: Exception) {
            KbEditorProject()
        }
    }

    fun saveProject(project: KbEditorProject) {
        try {
            projectFile.writeText(json.encodeToString(project))
            val currentList = loadAllProjects().filterNot { it.projectId == project.projectId }.toMutableList()
            currentList.add(0, project)
            libraryFile.writeText(json.encodeToString(currentList.take(20)))
        } catch (_: Exception) {
        }
    }

    fun loadAllProjects(): List<KbEditorProject> {
        return try {
            if (libraryFile.exists()) {
                json.decodeFromString<List<KbEditorProject>>(libraryFile.readText())
            } else {
                listOf(loadProject())
            }
        } catch (_: Exception) {
            listOf(loadProject())
        }
    }

    fun deleteProjectFromLibrary(projectId: String): List<KbEditorProject> {
        val next = loadAllProjects().filterNot { it.projectId == projectId }
        val safe = next.ifEmpty { listOf(KbEditorProject()) }
        try {
            libraryFile.writeText(json.encodeToString(safe))
        } catch (_: Exception) {
        }
        return safe
    }

    fun pushUndoSnapshot(current: KbEditorProject) {
        if (undoStack.lastOrNull() == current) return
        if (undoStack.size >= maxHistorySteps) {
            undoStack.removeFirst()
        }
        undoStack.addLast(current)
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(current: KbEditorProject): KbEditorProject? {
        val previous = undoStack.removeLastOrNull() ?: return null
        if (redoStack.size >= maxHistorySteps) {
            redoStack.removeFirst()
        }
        redoStack.addLast(current)
        saveProject(previous)
        return previous
    }

    fun redo(current: KbEditorProject): KbEditorProject? {
        val next = redoStack.removeLastOrNull() ?: return null
        if (undoStack.size >= maxHistorySteps) {
            undoStack.removeFirst()
        }
        undoStack.addLast(current)
        saveProject(next)
        return next
    }

    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }
}
