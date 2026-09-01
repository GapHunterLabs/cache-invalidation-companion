package dev.gaphunter.cacheinvalidationcompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CacheInvalidationInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(CacheInvalidationInspection::class.java)
    }

    fun `test save without touching a computeIfAbsent cache is flagged`() {
        myFixture.configureByText(
            "UserService.java",
            """
            import java.util.Map;
            import java.util.HashMap;

            class UserService {
                private final Map<Long, String> cache = new HashMap<>();

                String getUser(Long id) {
                    return cache.computeIfAbsent(id, k -> loadFromDb(k));
                }

                void updateUser(Long id, String name) {
                    repository.save(id);
                }

                String loadFromDb(Long id) { return "x"; }
                Repository repository;
            }

            class Repository {
                void save(Long id) {}
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("stale data") == true })
    }

    fun `test save without touching a manual containsKey put cache is flagged`() {
        myFixture.configureByText(
            "UserService2.java",
            """
            import java.util.Map;
            import java.util.HashMap;

            class UserService2 {
                private final Map<Long, String> cache = new HashMap<>();

                String getUser(Long id) {
                    if (!cache.containsKey(id)) {
                        cache.put(id, loadFromDb(id));
                    }
                    return cache.get(id);
                }

                void updateUser(Long id, String name) {
                    repository.save(id);
                }

                String loadFromDb(Long id) { return "x"; }
                Repository2 repository;
            }

            class Repository2 {
                void save(Long id) {}
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("stale data") == true })
    }

    fun `test save that also invalidates the cache is not flagged`() {
        myFixture.configureByText(
            "UserService3.java",
            """
            import java.util.Map;
            import java.util.HashMap;

            class UserService3 {
                private final Map<Long, String> cache = new HashMap<>();

                String getUser(Long id) {
                    return cache.computeIfAbsent(id, k -> loadFromDb(k));
                }

                void updateUser(Long id, String name) {
                    repository.save(id);
                    cache.remove(id);
                }

                String loadFromDb(Long id) { return "x"; }
                Repository3 repository;
            }

            class Repository3 {
                void save(Long id) {}
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("stale data") == true })
    }

    fun `test a class with no cache field at all is never flagged`() {
        myFixture.configureByText(
            "PlainService.java",
            """
            class PlainService {
                void updateUser(Long id, String name) {
                    repository.save(id);
                }
                Repository4 repository;
            }

            class Repository4 {
                void save(Long id) {}
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("stale data") == true })
    }

    fun `test a Map field never populated via get-or-compute is never treated as a cache`() {
        myFixture.configureByText(
            "PlainMapHolder.java",
            """
            import java.util.Map;
            import java.util.HashMap;

            class PlainMapHolder {
                private final Map<Long, String> data = new HashMap<>();

                void updateUser(Long id, String name) {
                    repository.save(id);
                }
                Repository5 repository;
            }

            class Repository5 {
                void save(Long id) {}
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("stale data") == true })
    }
}
