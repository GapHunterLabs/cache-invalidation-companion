import java.util.HashMap;
import java.util.Map;

class UserService {

    private final Map<Long, String> cache = new HashMap<>();
    private final Repository repository = new Repository();

    String getUser(Long id) {
        return cache.computeIfAbsent(id, this::loadFromDb);
    }

    // Flagged: writes to the real data source but never touches the cache.
    void updateUserUnsafe(Long id, String name) {
        repository.save(id);
    }

    // Not flagged: invalidates the cache after the write.
    void updateUserSafe(Long id, String name) {
        repository.save(id);
        cache.remove(id);
    }

    String loadFromDb(Long id) {
        return "loaded-" + id;
    }
}

class Repository {
    void save(Long id) {}
}
