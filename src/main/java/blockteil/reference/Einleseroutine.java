package blockteil.reference;

import java.util.Map;

public abstract class Einleseroutine {
    public Map<String, Gene> id2gene;

    public abstract Map<String, Gene> read();
}
