import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class EntityBag<T extends Entity> {
    private final ArrayList<T> data = new ArrayList<>();

    public void add(T t) {
        data.add(t);
    }

    public boolean remove(T t) {
        return data.remove(t);
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public void clear() {
        data.clear();
    }

    public Optional<T> at(Cell c) {
        for (T t : data) {
            if (t.getCell() == c) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
    public List<T> asList() {
        return Collections.unmodifiableList(data);
    }
}




