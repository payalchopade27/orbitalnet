package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class CustomQueueTest {

    @Test
    void emptyQueueBehavior() {
        CustomQueue<String> queue = new CustomQueue<>();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.peek());
        assertTrue(queue.toList().isEmpty());

        assertThrows(NoSuchElementException.class, queue::dequeue);
        assertThrows(NoSuchElementException.class, queue::peekExact);
    }

    @Test
    void enqueueAndDequeueSingleElement() {
        CustomQueue<String> queue = new CustomQueue<>();
        queue.enqueue("EVENT_01");

        assertFalse(queue.isEmpty());
        assertEquals(1, queue.size());
        assertEquals("EVENT_01", queue.peek());
        assertEquals("EVENT_01", queue.peekExact());

        assertEquals("EVENT_01", queue.dequeue());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.peek());
    }

    @Test
    void maintainsFIFOOrder() {
        CustomQueue<Integer> queue = new CustomQueue<>();
        queue.enqueue(100);
        queue.enqueue(200);
        queue.enqueue(300);

        assertEquals(3, queue.size());
        assertEquals(100, queue.peek());

        List<Integer> list = queue.toList();
        assertEquals(List.of(100, 200, 300), list);

        assertEquals(100, queue.dequeue());
        assertEquals(200, queue.dequeue());
        assertEquals(300, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void clearEmptiesQueue() {
        CustomQueue<String> queue = new CustomQueue<>();
        queue.enqueue("MSG_1");
        queue.enqueue("MSG_2");
        assertEquals(2, queue.size());

        queue.clear();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.peek());
    }

    @Test
    void rejectsNullItem() {
        CustomQueue<String> queue = new CustomQueue<>();
        assertThrows(IllegalArgumentException.class, () -> queue.enqueue(null));
    }

    @Test
    void telemetryEventStreamWorkflow() {
        // Demonstrating use case: Chronological simulation event log / telemetry FIFO queue
        CustomQueue<String> telemetryQueue = new CustomQueue<>();

        telemetryQueue.enqueue("TICK_0: SIMULATION_STARTED");
        telemetryQueue.enqueue("TICK_1: OBJECTS_PROPAGATED");
        telemetryQueue.enqueue("TICK_2: CLOSE_APPROACH_DETECTED_SAT-01_DEB-02");

        assertEquals(3, telemetryQueue.size());

        assertEquals("TICK_0: SIMULATION_STARTED", telemetryQueue.dequeue());
        assertEquals("TICK_1: OBJECTS_PROPAGATED", telemetryQueue.dequeue());
        assertEquals("TICK_2: CLOSE_APPROACH_DETECTED_SAT-01_DEB-02", telemetryQueue.peek());
        assertEquals(1, telemetryQueue.size());
    }
}

