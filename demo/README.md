# Demo data for screenshots

`UserService.java` — `updateUserUnsafe` flagged; `updateUserSafe` not
flagged (invalidates the cache after the write).

## How to get the screenshot

1. `./gradlew runIde` from `cache-invalidation-companion`, open this
   `demo/` folder as the project.
2. Full Screen, open `UserService.java` — a warning should appear on
   `updateUserUnsafe`'s `repository.save(id)` call but not on
   `updateUserSafe`'s.
3. Screenshot with both methods visible, save into
   `cache-invalidation-companion/docs/screenshots/`. Close the
   sandbox.
