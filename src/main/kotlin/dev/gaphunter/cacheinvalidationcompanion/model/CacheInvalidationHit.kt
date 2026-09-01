package dev.gaphunter.cacheinvalidationcompanion.model

import com.intellij.psi.PsiElement

/** One mutator method that writes to the real data source without touching the same class's hand-rolled `Map` cache field. */
data class CacheInvalidationHit(val anchor: PsiElement, val cacheFieldName: String, val mutatorMethodName: String)
