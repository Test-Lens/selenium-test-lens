package io.github.testlens.studio;

/** Descendant process used to prove process-tree cleanup. */
public final class ForkedChildSleeper {
    private ForkedChildSleeper() { }
    public static void main(String[] args){while(true)Thread.onSpinWait();}
}
