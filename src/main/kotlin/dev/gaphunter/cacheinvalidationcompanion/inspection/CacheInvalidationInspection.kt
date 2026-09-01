package dev.gaphunter.cacheinvalidationcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import dev.gaphunter.cacheinvalidationcompanion.detect.JavaCacheInvalidationFinder
import dev.gaphunter.cacheinvalidationcompanion.model.CacheInvalidationHit
import dev.gaphunter.cacheinvalidationcompanion.review.ReviewPrompt

/**
 * Flags a `.save(...)` call inside a method whose class also has a
 * hand-rolled `Map`-backed cache field, when that method never touches
 * the cache field -- stale data served after a real update, a silent
 * inconsistency between what's written and what's read.
 *
 * Runs via `checkFile` (same shape as every other inspection in this
 * catalog); [JavaCacheInvalidationFinder] does the real PSI walk.
 */
class CacheInvalidationInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null
        if (file !is PsiJavaFile) return null

        val hits = JavaCacheInvalidationFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: CacheInvalidationHit): String =
        "'${hit.mutatorMethodName}' writes to the real data source but never touches the '${hit.cacheFieldName}' " +
            "cache field -- a read after this update can silently serve stale data from the cache"
}
