package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class CustomStackTest {

    @Test
    void emptyStackBehavior() {
        CustomStack<String> stack = new CustomStack<>();

        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
        assertNull(stack.peek());
        assertTrue(stack.toList().isEmpty());

        assertThrows(NoSuchElementException.class, stack::pop);
        assertThrows(NoSuchElementException.class, stack::peekExact);
    }

    @Test
    void pushAndPopSingleElement() {
        CustomStack<String> stack = new CustomStack<>();
        stack.push("ACTION_1");

        assertFalse(stack.isEmpty());
        assertEquals(1, stack.size());
        assertEquals("ACTION_1", stack.peek());
        assertEquals("ACTION_1", stack.peekExact());

        assertEquals("ACTION_1", stack.pop());
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
        assertNull(stack.peek());
    }

    @Test
    void maintainsLIFOOrder() {
        CustomStack<Integer> stack = new CustomStack<>();
        stack.push(10);
        stack.push(20);
        stack.push(30);

        assertEquals(3, stack.size());
        assertEquals(30, stack.peek());

        List<Integer> list = stack.toList();
        assertEquals(List.of(30, 20, 10), list);

        assertEquals(30, stack.pop());
        assertEquals(20, stack.pop());
        assertEquals(10, stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    void clearEmptiesStack() {
        CustomStack<String> stack = new CustomStack<>();
        stack.push("A");
        stack.push("B");
        assertEquals(2, stack.size());

        stack.clear();
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
        assertNull(stack.peek());
    }

    @Test
    void rejectsNullItem() {
        CustomStack<String> stack = new CustomStack<>();
        assertThrows(IllegalArgumentException.class, () -> stack.push(null));
    }

    @Test
    void maneuverHistoryUndoWorkflow() {
        // Demonstrating use case: maneuver command undo / replay stack
        CustomStack<String> maneuverHistory = new CustomStack<>();

        maneuverHistory.push("DELTA_V_+0.05_KM_S_PROGRADE");
        maneuverHistory.push("DELTA_V_-0.02_KM_S_RADIAL");
        maneuverHistory.push("DELTA_V_+0.01_KM_S_NORMAL");

        assertEquals(3, maneuverHistory.size());

        // Undo most recent action
        String undone = maneuverHistory.pop();
        assertEquals("DELTA_V_+0.01_KM_S_NORMAL", undone);
        assertEquals("DELTA_V_-0.02_KM_S_RADIAL", maneuverHistory.peek());
        assertEquals(2, maneuverHistory.size());
    }
}

