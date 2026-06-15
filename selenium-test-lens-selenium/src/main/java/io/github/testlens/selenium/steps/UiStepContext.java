package io.github.testlens.selenium.steps;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class UiStepContext {
    private final Deque<String> stack = new ArrayDeque<>();

    public void push(String name) {
        stack.push(UiStepResult.validateName(name));
    }

    public void pop() {
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public List<String> currentPath() {
        return List.copyOf(stack);
    }
}
