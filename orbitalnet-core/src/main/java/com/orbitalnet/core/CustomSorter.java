package com.orbitalnet.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Custom hand-written sorting implementation using MergeSort.
 *
 * <p>Properties:
 * <ul>
 *   <li>Time Complexity: O(n log n) across best, average, and worst cases.</li>
 *   <li>Space Complexity: O(n) auxiliary storage.</li>
 *   <li>Stability: Preserves the relative order of records with equal keys.</li>
 * </ul>
 *
 * <p>Used in OrbitalNet to sort assessed threats, benchmark latency arrays, and telemetry data.
 */
public final class CustomSorter {

    private CustomSorter() {
    }

    /**
     * Sorts the specified list in-place using a stable MergeSort algorithm.
     *
     * @param list the list to sort
     * @param comparator the comparator determining order
     * @param <T> element type
     */
    public static <T> void mergeSort(List<T> list, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "Comparator cannot be null.");
        if (list == null || list.size() <= 1) {
            return;
        }

        List<T> auxiliary = new ArrayList<>(list);
        mergeSortInternal(list, auxiliary, 0, list.size() - 1, comparator);
    }

    private static <T> void mergeSortInternal(
            List<T> list,
            List<T> aux,
            int low,
            int high,
            Comparator<? super T> comparator) {

        if (low >= high) {
            return;
        }

        int mid = low + (high - low) / 2;
        mergeSortInternal(list, aux, low, mid, comparator);
        mergeSortInternal(list, aux, mid + 1, high, comparator);
        merge(list, aux, low, mid, high, comparator);
    }

    private static <T> void merge(
            List<T> list,
            List<T> aux,
            int low,
            int mid,
            int high,
            Comparator<? super T> comparator) {

        for (int k = low; k <= high; k++) {
            aux.set(k, list.get(k));
        }

        int i = low;
        int j = mid + 1;

        for (int k = low; k <= high; k++) {
            if (i > mid) {
                list.set(k, aux.get(j++));
            } else if (j > high) {
                list.set(k, aux.get(i++));
            } else if (comparator.compare(aux.get(j), aux.get(i)) < 0) {
                list.set(k, aux.get(j++));
            } else {
                // Stable choice when equal: pick left element
                list.set(k, aux.get(i++));
            }
        }
    }

    /**
     * Sorts an array of doubles in-place in ascending order.
     */
    public static void sort(double[] array) {
        if (array == null || array.length <= 1) {
            return;
        }
        double[] aux = new double[array.length];
        mergeSortDouble(array, aux, 0, array.length - 1);
    }

    private static void mergeSortDouble(double[] a, double[] aux, int low, int high) {
        if (low >= high) {
            return;
        }
        int mid = low + (high - low) / 2;
        mergeSortDouble(a, aux, low, mid);
        mergeSortDouble(a, aux, mid + 1, high);
        mergeDouble(a, aux, low, mid, high);
    }

    private static void mergeDouble(double[] a, double[] aux, int low, int mid, int high) {
        System.arraycopy(a, low, aux, low, high - low + 1);

        int i = low;
        int j = mid + 1;

        for (int k = low; k <= high; k++) {
            if (i > mid) {
                a[k] = aux[j++];
            } else if (j > high) {
                a[k] = aux[i++];
            } else if (aux[j] < aux[i]) {
                a[k] = aux[j++];
            } else {
                a[k] = aux[i++];
            }
        }
    }

    /**
     * Sorts an array of ints in-place in ascending order.
     */
    public static void sort(int[] array) {
        if (array == null || array.length <= 1) {
            return;
        }
        int[] aux = new int[array.length];
        mergeSortInt(array, aux, 0, array.length - 1);
    }

    private static void mergeSortInt(int[] a, int[] aux, int low, int high) {
        if (low >= high) {
            return;
        }
        int mid = low + (high - low) / 2;
        mergeSortInt(a, aux, low, mid);
        mergeSortInt(a, aux, mid + 1, high);
        mergeInt(a, aux, low, mid, high);
    }

    private static void mergeInt(int[] a, int[] aux, int low, int mid, int high) {
        System.arraycopy(a, low, aux, low, high - low + 1);

        int i = low;
        int j = mid + 1;

        for (int k = low; k <= high; k++) {
            if (i > mid) {
                a[k] = aux[j++];
            } else if (j > high) {
                a[k] = aux[i++];
            } else if (aux[j] < aux[i]) {
                a[k] = aux[j++];
            } else {
                a[k] = aux[i++];
            }
        }
    }

    /**
     * Returns a copy of the encounters sorted in descending order of risk score,
     * breaking ties by ascending encounter ID.
     */
    public static List<Encounter> sortByRiskScoreDescending(List<Encounter> encounters) {
        if (encounters == null) {
            return new ArrayList<>();
        }
        List<Encounter> copy = new ArrayList<>(encounters);
        mergeSort(copy, (a, b) -> {
            int scoreA = a.getRiskScore() != null ? a.getRiskScore().getTotalScore() : -1;
            int scoreB = b.getRiskScore() != null ? b.getRiskScore().getTotalScore() : -1;

            if (scoreA != scoreB) {
                return Integer.compare(scoreB, scoreA); // descending
            }
            return a.getEncounterId().compareTo(b.getEncounterId()); // ascending
        });
        return copy;
    }

    /**
     * Returns a copy of the encounters sorted in ascending order of predicted minimum separation distance.
     */
    public static List<Encounter> sortBySeparationAscending(List<Encounter> encounters) {
        if (encounters == null) {
            return new ArrayList<>();
        }
        List<Encounter> copy = new ArrayList<>(encounters);
        mergeSort(copy, Comparator.comparingDouble(Encounter::getMinSeparationKm));
        return copy;
    }
}

