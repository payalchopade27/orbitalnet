package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Custom binary max-heap priority queue used to rank assessed encounters by risk for threat mitigation.
 *
 * <p>Heap Invariants:
 * <ul>
 *   <li>The root at index 0 always contains the highest-priority encounter.</li>
 *   <li>For every element at index {@code i > 0}, its parent at index {@code (i - 1) / 2} has higher or equal priority.</li>
 *   <li>Priority ordering:
 *     <ol>
 *       <li>Total risk score descending (higher risk score = higher priority).</li>
 *       <li>Deterministic tie-breaking: encounter ID ascending lexicographically
 *           (e.g., "ENC-01" takes priority over "ENC-02").</li>
 *     </ol>
 *   </li>
 * </ul>
 *
 * <p>Only assessed encounters (having a non-null {@link RiskScore}) may be added to the heap.
 */
public class CustomMaxHeap {

    private static final int DEFAULT_CAPACITY = 16;

    private Encounter[] heap;
    private int size;

    public CustomMaxHeap() {
        this(DEFAULT_CAPACITY);
    }

    public CustomMaxHeap(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException(
                    "Initial capacity must be positive, got: " + initialCapacity);
        }
        this.heap = new Encounter[initialCapacity];
        this.size = 0;
    }

    /**
     * Constructs and heapifies an array from a collection of assessed encounters in O(n) time.
     *
     * @param encounters collection of assessed encounters
     * @throws IllegalArgumentException if any encounter is null or unassessed
     * @throws NullPointerException if encounters collection is null
     */
    public CustomMaxHeap(Collection<Encounter> encounters) {
        Objects.requireNonNull(encounters, "Encounters collection cannot be null.");
        int initialCapacity = Math.max(DEFAULT_CAPACITY, encounters.size());
        this.heap = new Encounter[initialCapacity];
        this.size = 0;

        for (Encounter encounter : encounters) {
            validateEncounter(encounter);
            this.heap[this.size++] = encounter;
        }

        heapify();
    }

    private static void validateEncounter(Encounter encounter) {
        if (encounter == null) {
            throw new IllegalArgumentException("Encounter cannot be null.");
        }
        if (encounter.getRiskScore() == null) {
            throw new IllegalArgumentException(
                    "Encounter must be assessed with a RiskScore before being added to the heap. Encounter ID: "
                            + encounter.getEncounterId());
        }
    }

    /**
     * Determines whether encounter {@code a} has strictly higher priority than encounter {@code b}.
     *
     * <p>Ordering rules:
     * <ol>
     *   <li>Risk total score descending (higher score wins).</li>
     *   <li>Encounter ID ascending lexicographically (smaller ID wins).</li>
     * </ol>
     */
    public static boolean hasHigherPriority(Encounter a, Encounter b) {
        Objects.requireNonNull(a, "Encounter a cannot be null.");
        Objects.requireNonNull(b, "Encounter b cannot be null.");

        int scoreA = a.getRiskScore().getTotalScore();
        int scoreB = b.getRiskScore().getTotalScore();

        if (scoreA != scoreB) {
            return scoreA > scoreB;
        }

        return a.getEncounterId().compareTo(b.getEncounterId()) < 0;
    }

    /**
     * Inserts an assessed encounter into the heap in O(log n) time.
     */
    public void insert(Encounter encounter) {
        validateEncounter(encounter);

        if (size == heap.length) {
            ensureCapacity();
        }

        heap[size] = encounter;
        siftUp(size);
        size++;
    }

    /**
     * Retrieves and removes the highest-priority encounter in O(log n) time.
     *
     * @return the highest-priority encounter
     * @throws NoSuchElementException if the heap is empty
     */
    public Encounter extractMax() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot extract from an empty heap.");
        }

        Encounter max = heap[0];
        size--;

        if (size > 0) {
            heap[0] = heap[size];
            heap[size] = null;
            siftDown(0);
        } else {
            heap[0] = null;
        }

        return max;
    }

    /**
     * Retrieves, but does not remove, the highest-priority encounter in O(1) time.
     *
     * @return the highest-priority encounter, or {@code null} if the heap is empty
     */
    public Encounter peek() {
        if (isEmpty()) {
            return null;
        }
        return heap[0];
    }

    /**
     * Retrieves, but does not remove, the highest-priority encounter in O(1) time.
     *
     * @return the highest-priority encounter
     * @throws NoSuchElementException if the heap is empty
     */
    public Encounter peekMax() {
        if (isEmpty()) {
            throw new NoSuchElementException("Heap is empty.");
        }
        return heap[0];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            heap[i] = null;
        }
        size = 0;
    }

    /**
     * Returns all encounters in descending priority order without mutating this heap.
     */
    public List<Encounter> toSortedList() {
        CustomMaxHeap copy = new CustomMaxHeap(Math.max(DEFAULT_CAPACITY, this.size));
        System.arraycopy(this.heap, 0, copy.heap, 0, this.size);
        copy.size = this.size;

        List<Encounter> sorted = new ArrayList<>(this.size);
        while (!copy.isEmpty()) {
            sorted.add(copy.extractMax());
        }
        return sorted;
    }

    /**
     * Convenience method to rank a collection of encounters by priority order.
     */
    public static List<Encounter> rank(Collection<Encounter> encounters) {
        return new CustomMaxHeap(encounters).toSortedList();
    }

    /**
     * Verifies whether the heap invariant holds across all nodes.
     */
    public boolean isValidHeap() {
        for (int i = 0; i < size; i++) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;

            if (left < size && hasHigherPriority(heap[left], heap[i])) {
                return false;
            }
            if (right < size && hasHigherPriority(heap[right], heap[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Rebuilds the heap in O(n) time to restore heap invariants if an encounter's
     * risk score was updated externally while stored in the heap.
     */
    public void rebuild() {
        heapify();
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            if (hasHigherPriority(heap[index], heap[parentIndex])) {
                swap(index, parentIndex);
                index = parentIndex;
            } else {
                break;
            }
        }
    }

    private void siftDown(int index) {
        while (true) {
            int leftChild = 2 * index + 1;
            int rightChild = 2 * index + 2;
            int highestPriorityIndex = index;

            if (leftChild < size && hasHigherPriority(heap[leftChild], heap[highestPriorityIndex])) {
                highestPriorityIndex = leftChild;
            }

            if (rightChild < size && hasHigherPriority(heap[rightChild], heap[highestPriorityIndex])) {
                highestPriorityIndex = rightChild;
            }

            if (highestPriorityIndex != index) {
                swap(index, highestPriorityIndex);
                index = highestPriorityIndex;
            } else {
                break;
            }
        }
    }

    private void heapify() {
        for (int i = (size / 2) - 1; i >= 0; i--) {
            siftDown(i);
        }
    }

    private void swap(int i, int j) {
        Encounter temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    private void ensureCapacity() {
        int newCapacity = heap.length * 2;
        Encounter[] newHeap = new Encounter[newCapacity];
        System.arraycopy(heap, 0, newHeap, 0, size);
        heap = newHeap;
    }
}

