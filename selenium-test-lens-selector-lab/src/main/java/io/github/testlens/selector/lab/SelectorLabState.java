package io.github.testlens.selector.lab;

/** Internal Lab lifecycle; transitions are validated by SelectorLabSession. */
public enum SelectorLabState { CLOSED,IDLE,PICKING,TARGET_SELECTED,ANALYZING,READY,TARGET_STALE,ERROR }
