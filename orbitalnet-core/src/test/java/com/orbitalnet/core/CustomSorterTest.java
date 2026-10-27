package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomSorterTest {

    private Encounter createAssessedEncounter(String id, int score, double separation) {
        Encounter enc = new Encounter(
                id, "RUN-01", "SAT-01", "DEB-01",
                separation, 500.0, 5.0, 1
        );
        RiskScore rs = new RiskScore(
                score, 0.80, 0.33, 0.86,
                RiskScore.computeLabel(score),
                "Explanation for " + id
        );
        enc.assess(rs);
        return enc;
    }

    @Test
    void sortsDoubleArrayIncludingNegativesAndDuplicates() {
        double[] array = {42.5, -10.2, 0.0, 100.0, -10.2, 5.5, -50.0};
        CustomSorter.sort(array);

        double[] expected = {-50.0, -10.2, -10.2, 0.0, 5.5, 42.5, 100.0};
        assertArrayEquals(expected, array, 0.000001);

        // Edge cases: null, empty, single element
        CustomSorter.sort((double[]) null);
        double[] empty = {};
        CustomSorter.sort(empty);
        assertEquals(0, empty.length);

        double[] single = {3.14};
        CustomSorter.sort(single);
        assertEquals(3.14, single[0], 0.000001);
    }

    @Test
    void sortsIntArrayIncludingNegativesAndDuplicates() {
        int[] array = {9, -3, 5, 0, -3, 8, -10, 15};
        CustomSorter.sort(array);

        int[] expected = {-10, -3, -3, 0, 5, 8, 9, 15};
        assertArrayEquals(expected, array);

        // Edge cases: null, empty, single element
        CustomSorter.sort((int[]) null);
        int[] empty = {};
        CustomSorter.sort(empty);
        assertEquals(0, empty.length);

        int[] single = {42};
        CustomSorter.sort(single);
        assertEquals(42, single[0]);
    }

    @Test
    void sortsGenericListWithComparator() {
        List<String> words = new ArrayList<>(Arrays.asList("banana", "apple", "cherry", "date"));
        CustomSorter.mergeSort(words, String::compareTo);

        assertEquals(List.of("apple", "banana", "cherry", "date"), words);

        // Edge cases
        CustomSorter.mergeSort(null, String::compareTo);
        List<String> single = new ArrayList<>(List.of("only"));
        CustomSorter.mergeSort(single, String::compareTo);
        assertEquals("only", single.get(0));

        assertThrows(NullPointerException.class, () -> CustomSorter.mergeSort(words, null));
    }

    @Test
    void sortsEncountersByRiskScoreDescendingWithDeterministicTieBreak() {
        Encounter e1 = createAssessedEncounter("ENC-03", 80, 20.0);
        Encounter e2 = createAssessedEncounter("ENC-01", 80, 25.0);
        Encounter e3 = createAssessedEncounter("ENC-CRIT", 95, 5.0);
        Encounter e4 = createAssessedEncounter("ENC-LOW", 20, 80.0);
        Encounter e5 = createAssessedEncounter("ENC-02", 80, 15.0);

        List<Encounter> sorted = CustomSorter.sortByRiskScoreDescending(
                Arrays.asList(e1, e2, e3, e4, e5));

        assertEquals(5, sorted.size());
        assertEquals("ENC-CRIT", sorted.get(0).getEncounterId()); // 95
        assertEquals("ENC-01", sorted.get(1).getEncounterId());   // 80, id ENC-01
        assertEquals("ENC-02", sorted.get(2).getEncounterId());   // 80, id ENC-02
        assertEquals("ENC-03", sorted.get(3).getEncounterId());   // 80, id ENC-03
        assertEquals("ENC-LOW", sorted.get(4).getEncounterId());  // 20
    }

    @Test
    void sortsEncountersBySeparationAscending() {
        Encounter e1 = createAssessedEncounter("E1", 50, 45.0);
        Encounter e2 = createAssessedEncounter("E2", 90, 5.0);
        Encounter e3 = createAssessedEncounter("E3", 70, 22.0);

        List<Encounter> sorted = CustomSorter.sortBySeparationAscending(
                Arrays.asList(e1, e2, e3));

        assertEquals("E2", sorted.get(0).getEncounterId()); // 5.0 km
        assertEquals("E3", sorted.get(1).getEncounterId()); // 22.0 km
        assertEquals("E1", sorted.get(2).getEncounterId()); // 45.0 km
    }

    @Test
    void preservesStabilityOnEqualKeys() {
        class Record {
            final int key;
            final String tag;
            Record(int key, String tag) { this.key = key; this.tag = tag; }
        }

        Record r1 = new Record(1, "first-1");
        Record r2 = new Record(2, "first-2");
        Record r3 = new Record(1, "second-1");
        Record r4 = new Record(1, "third-1");
        Record r5 = new Record(2, "second-2");

        List<Record> records = new ArrayList<>(Arrays.asList(r1, r2, r3, r4, r5));
        CustomSorter.mergeSort(records, (a, b) -> Integer.compare(a.key, b.key));

        // For key=1, order should be first-1, second-1, third-1
        assertEquals("first-1", records.get(0).tag);
        assertEquals("second-1", records.get(1).tag);
        assertEquals("third-1", records.get(2).tag);

        // For key=2, order should be first-2, second-2
        assertEquals("first-2", records.get(3).tag);
        assertEquals("second-2", records.get(4).tag);
    }
}

