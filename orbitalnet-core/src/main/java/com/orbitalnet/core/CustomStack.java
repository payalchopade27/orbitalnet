package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Hand-written generic singly-linked stack (LIFO) for maneuver history, replay, and undo workflows.
 *
 * <p>All primary stack operations run in strict O(1) time.
 *
 * @param <E> the type of elements held in this stack
 */
public class CustomStack<E> {

    private static class Node<E> {
        private final E data;
        private final Node<E> next;

        private Node(E data, Node<E> next) {
            this.data = data;
            this.next = next;
        }
    }

    private Node<E> top;
    private int size;

    public CustomStack() {
        this.top = null;
        this.size = 0;
    }

    /**
     * Pushes an item onto the top of this stack in O(1) time.
     *
     * @param item the element to push
     * @throws IllegalArgumentException if item is null
     */
    public void push(E item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot push null onto the stack.");
        }
        top = new Node<>(item, top);
        size++;
    }

    /**
     * Removes and returns the item at the top of this stack in O(1) time.
     *
     * @return the item popped from the top of the stack
     * @throws NoSuchElementException if the stack is empty
     */
    public E pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot pop from an empty stack.");
        }
        E item = top.data;
        top = top.next;
        size--;
        return item;
    }

    /**
     * Returns the item at the top of this stack without removing it in O(1) time.
     *
     * @return the top item, or {@code null} if the stack is empty
     */
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return top.data;
    }

    /**
     * Returns the item at the top of this stack without removing it in O(1) time.
     *
     * @return the top item
     * @throws NoSuchElementException if the stack is empty
     */
    public E peekExact() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty.");
        }
        return top.data;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        top = null;
        size = 0;
    }

    /**
     * Returns the elements in the stack from top to bottom as a list.
     */
    public List<E> toList() {
        List<E> list = new ArrayList<>(size);
        Node<E> current = top;
        while (current != null) {
            list.add(current.data);
            current = current.next;
        }
        return list;
    }
}

