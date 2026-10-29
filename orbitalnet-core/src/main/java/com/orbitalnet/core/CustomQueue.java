package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Hand-written generic singly-linked queue (FIFO) for telemetry streams, chronological event logging,
 * and BFS frontiers.
 *
 * <p>All primary queue operations run in strict O(1) time.
 *
 * @param <E> the type of elements held in this queue
 */
public class CustomQueue<E> {

    private static class Node<E> {
        private final E data;
        private Node<E> next;

        private Node(E data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node<E> head;
    private Node<E> tail;
    private int size;

    public CustomQueue() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /**
     * Inserts the specified item at the tail of this queue in O(1) time.
     *
     * @param item the element to enqueue
     * @throws IllegalArgumentException if item is null
     */
    public void enqueue(E item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot enqueue null into the queue.");
        }
        Node<E> newNode = new Node<>(item);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    /**
     * Retrieves and removes the item at the head of this queue in O(1) time.
     *
     * @return the head item
     * @throws NoSuchElementException if the queue is empty
     */
    public E dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot dequeue from an empty queue.");
        }
        E item = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return item;
    }

    /**
     * Retrieves, but does not remove, the item at the head of this queue in O(1) time.
     *
     * @return the head item, or {@code null} if the queue is empty
     */
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return head.data;
    }

    /**
     * Retrieves, but does not remove, the item at the head of this queue in O(1) time.
     *
     * @return the head item
     * @throws NoSuchElementException if the queue is empty
     */
    public E peekExact() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty.");
        }
        return head.data;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Returns the elements in this queue from head to tail as a list.
     */
    public List<E> toList() {
        List<E> list = new ArrayList<>(size);
        Node<E> current = head;
        while (current != null) {
            list.add(current.data);
            current = current.next;
        }
        return list;
    }
}

