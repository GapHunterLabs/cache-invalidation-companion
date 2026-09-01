package dev.gaphunter.cacheinvalidationcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.cacheinvalidationcompanion.model.CacheInvalidationHit

/**
 * Finds a hand-rolled `Map`-backed cache field (populated by a
 * `computeIfAbsent`/manual `containsKey`+`put` "get-or-compute"
 * pattern) whose class has a DIFFERENT method writing to the real
 * data source (a `.save(...)` call, the common Spring Data repository
 * convention) without ever touching (invalidating) that same cache
 * field -- stale data served after a real update, a silent
 * inconsistency between what's written and what's read.
 *
 * **v0.1 scope, stated honestly:** only a hand-rolled cache with `Map`
 * as the backing structure inside a SINGLE class -- never covers
 * `@Cacheable`/Caffeine/Redis as an external cache (those have their
 * own declarative invalidation mechanism, out of scope here). "The
 * same data source" is inferred heuristically (the mutator method
 * never mentions the cache field's name at all in its own body) --
 * a real risk of false positives if the heuristic is loose, the same
 * honest limit the original catalog design for this mechanism already
 * names.
 */
object JavaCacheInvalidationFinder {

    fun findAll(file: PsiFile): List<CacheInvalidationHit> {
        val hits = mutableListOf<CacheInvalidationHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitClass(psiClass: PsiClass) {
                super.visitClass(psiClass)
                hits += hitsForClass(psiClass)
            }
        })
        return hits
    }

    private fun hitsForClass(psiClass: PsiClass): List<CacheInvalidationHit> {
        val cacheFields = psiClass.fields.filter { isMapCacheField(it, psiClass) }
        if (cacheFields.isEmpty()) return emptyList()

        val hits = mutableListOf<CacheInvalidationHit>()
        for (cacheField in cacheFields) {
            for (method in psiClass.methods) {
                val saveCall = findSaveCall(method) ?: continue
                if (mentionsField(method, cacheField.name)) continue // touches/invalidates the cache somewhere -- assumed handled
                // Anchors on the method NAME identifier ("save"), never the
                // whole call expression -- descending blindly via firstChild
                // can land on an empty PsiReferenceParameterList node,
                // rejected by the platform (confirmed the hard way, more
                // than once, in this catalog's ReDoS/JWT/SSRF plugins).
                val anchor = saveCall.methodExpression.referenceNameElement ?: saveCall.methodExpression
                hits += CacheInvalidationHit(anchor, cacheField.name, method.name)
            }
        }
        return hits
    }

    /** True when [field]'s declared type is (a subtype of) `Map`, and some method of [owningClass] populates it via `computeIfAbsent` or the manual `containsKey`+`put` pattern. */
    private fun isMapCacheField(field: PsiField, owningClass: PsiClass): Boolean {
        val typeText = field.type.presentableText
        if (!typeText.startsWith("Map") && !typeText.contains("Map<")) return false
        return owningClass.methods.any { populatesCache(it, field.name) }
    }

    private fun populatesCache(method: PsiMethod, fieldName: String): Boolean {
        var sawComputeIfAbsent = false
        var sawContainsKey = false
        var sawPut = false
        method.body?.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                super.visitMethodCallExpression(call)
                val qualifierText = call.methodExpression.qualifierExpression?.text ?: return
                if (qualifierText != fieldName) return
                when (call.methodExpression.referenceName) {
                    "computeIfAbsent" -> sawComputeIfAbsent = true
                    "containsKey" -> sawContainsKey = true
                    "put" -> sawPut = true
                }
            }
        })
        return sawComputeIfAbsent || (sawContainsKey && sawPut)
    }

    /** The first `.save(...)` call found in [method]'s body -- the common Spring Data repository convention for "write to the real data source". */
    private fun findSaveCall(method: PsiMethod): PsiMethodCallExpression? {
        var found: PsiMethodCallExpression? = null
        method.body?.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                if (found != null) return
                super.visitMethodCallExpression(call)
                if (call.methodExpression.referenceName == "save") found = call
            }
        })
        return found
    }

    /** True when [fieldName] appears anywhere as a qualifier/reference inside [method]'s own body -- a simple, conservative "this method touches the cache somehow" signal. */
    private fun mentionsField(method: PsiMethod, fieldName: String): Boolean {
        val body = method.body ?: return false
        var found = false
        body.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                if (found) return
                super.visitReferenceExpression(expression)
                if (expression.referenceName == fieldName) found = true
            }
        })
        return found
    }
}
