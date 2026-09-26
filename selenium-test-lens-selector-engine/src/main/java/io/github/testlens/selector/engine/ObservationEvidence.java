package io.github.testlens.selector.engine;

import java.util.List;
import java.util.Objects;

/** Evidence is supplied by tooling; this module does not collect cross-run history. */
public record ObservationEvidence(State state, List<Comparison> comparisons) {
    public ObservationEvidence {
        Objects.requireNonNull(state); comparisons=List.copyOf(Objects.requireNonNull(comparisons));
        State derived=derive(comparisons);
        if(!comparisons.isEmpty() && state!=derived) throw new IllegalArgumentException("Evidence state does not match comparison entries");
    }
    public static ObservationEvidence unavailable(){return new ObservationEvidence(State.NOT_AVAILABLE,List.of());}
    public static ObservationEvidence of(List<Comparison> comparisons){return new ObservationEvidence(derive(comparisons),comparisons);}
    private static State derive(List<Comparison> c){
        if(c==null||c.isEmpty())return State.NOT_AVAILABLE;
        if(c.stream().anyMatch(x->x.comparable()&&x.changed()))return State.CHANGED;
        if(c.stream().anyMatch(Comparison::comparable))return State.UNCHANGED_IN_SCOPE;
        return State.INSUFFICIENT;
    }
    public enum State { NOT_AVAILABLE, INSUFFICIENT, UNCHANGED_IN_SCOPE, CHANGED }
    public enum Domain { SAME_DOCUMENT, REMOUNT, RELOAD, NEW_SESSION, NEW_RUN, DIFFERENT_DATASET, DIFFERENT_BUILD }
    public record Comparison(String evidenceRef, Domain domain, boolean comparable, boolean changed) {
        public Comparison { Objects.requireNonNull(evidenceRef); Objects.requireNonNull(domain); }
    }
}
