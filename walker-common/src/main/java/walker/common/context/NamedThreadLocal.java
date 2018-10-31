package walker.common.context;

public class NamedThreadLocal<T> extends InheritableThreadLocal<T> {

    private String name;

    public NamedThreadLocal(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
