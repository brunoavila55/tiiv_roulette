package br.com.nasorte;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Random;

/** All eligible entries have exactly one ticket, including entries with equal names. */
public final class DrawPool {
    private DrawPool() {}
    public static List<String> eligible(List<String> ids, Set<String> drawn, boolean noRepeat) {
        List<String> result = new ArrayList<>();
        for (String id : ids) if (!noRepeat || !drawn.contains(id)) result.add(id);
        return result;
    }
    public static String pick(List<String> eligible, Random random) {
        if (eligible.isEmpty()) throw new IllegalStateException("Nenhum item disponível");
        return eligible.get(random.nextInt(eligible.size()));
    }
}
