package com.orbitalnet.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class CustomMaxHeapTest {

    private Encounter createAssessedEncounter(String encounterId, int totalScore) {
        Encounter encounter = new Encounter(
                encounterId,
                "RUN-01",
                "SAT-01",
                "DEB-01",
                20.0,
                500.0,
                5.0,
                1
        );
        RiskScore score = new RiskScore(
                totalScore,
                0.80,
                0.33,
                0.86,
                RiskScore.computeLabel(totalScore),
                "Risk explanation for " + encounterId
        );
        encounter.assess(score);
        return encounter;
    }

    @Test
    void emptyHeapReportsEmptyStateAndHandlesEdgeCases() {
        CustomMaxHeap heap = new CustomMaxHeap();

        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
        assertNull(heap.peek());
        assertTrue(heap.isValidHeap());

        assertThrows(NoSuchElementException.class, heap::peekMax);
        assertThrows(NoSuchElementException.class, heap::extractMax);

        List<Encounter> sorted = heap.toSortedList();
        assertTrue(sorted.isEmpty());
    }

    @Test
    void singleElementHeapMaintainsInvariants() {
        CustomMaxHeap heap = new CustomMaxHeap();
        Encounter encounter = createAssessedEncounter("ENC-01", 50);

        heap.insert(encounter);

        assertFalse(heap.isEmpty());
        assertEquals(1, heap.size());
        assertSame(encounter, heap.peek());
        assertSame(encounter, heap.peekMax());
        assertTrue(heap.isValidHeap());

        Encounter extracted = heap.extractMax();
        assertSame(encounter, extracted);
        assertTrue(heap.isEmpty());
        assertEquals(0, heap.size());
        assertNull(heap.peek());
        assertTrue(heap.isValidHeap());
    }

    @Test
    void ranksByDescendingRiskScore() {
        CustomMaxHeap heap = new CustomMaxHeap();
        Encounter low = createAssessedEncounter("ENC-LOW", 15);
        Encounter mod = createAssessedEncounter("ENC-MOD", 40);
        Encounter high = createAssessedEncounter("ENC-HIGH", 70);
        Encounter crit = createAssessedEncounter("ENC-CRIT", 95);

        // Insert in arbitrary order
        heap.insert(mod);
        heap.insert(crit);
        heap.insert(low);
        heap.insert(high);

        assertEquals(4, heap.size());
        assertTrue(heap.isValidHeap());
        assertSame(crit, heap.peek());

        assertSame(crit, heap.extractMax());
        assertTrue(heap.isValidHeap());

        assertSame(high, heap.extractMax());
        assertTrue(heap.isValidHeap());

        assertSame(mod, heap.extractMax());
        assertTrue(heap.isValidHeap());

        assertSame(low, heap.extractMax());
        assertTrue(heap.isValidHeap());

        assertTrue(heap.isEmpty());
    }

    @Test
    void deterministicTieBreakingUsesAscendingEncounterId() {
        CustomMaxHeap heap = new CustomMaxHeap();

        // Encounters with identical scores
        Encounter e3 = createAssessedEncounter("ENC-03", 80);
        Encounter e1 = createAssessedEncounter("ENC-01", 80);
        Encounter e4 = createAssessedEncounter("ENC-04", 80);
        Encounter e2 = createAssessedEncounter("ENC-02", 80);

        heap.insert(e3);
        heap.insert(e1);
        heap.insert(e4);
        heap.insert(e2);

        assertEquals(4, heap.size());
        assertTrue(heap.isValidHeap());

        // Lexicographically smaller ID must be extracted first
        assertEquals("ENC-01", heap.extractMax().getEncounterId());
        assertEquals("ENC-02", heap.extractMax().getEncounterId());
        assertEquals("ENC-03", heap.extractMax().getEncounterId());
        assertEquals("ENC-04", heap.extractMax().getEncounterId());
    }

    @Test
    void higherRiskScoreAlwaysOutranksSmallerId() {
        CustomMaxHeap heap = new CustomMaxHeap();

        // Even though "ENC-01" has smaller ID, "ENC-99" has higher risk score
        Encounter lowerScore = createAssessedEncounter("ENC-01", 70);
        Encounter higherScore = createAssessedEncounter("ENC-99", 75);

        heap.insert(lowerScore);
        heap.insert(higherScore);

        assertSame(higherScore, heap.extractMax());
        assertSame(lowerScore, heap.extractMax());
    }

    @Test
    void handlesIncreasingAndDecreasingSequences() {
        // Increasing insertions
        CustomMaxHeap incHeap = new CustomMaxHeap();
        for (int i = 10; i <= 90; i += 10) {
            incHeap.insert(createAssessedEncounter("ENC-" + i, i));
            assertTrue(incHeap.isValidHeap());
        }
        for (int i = 90; i >= 10; i -= 10) {
            assertEquals(i, incHeap.extractMax().getRiskScore().getTotalScore());
            assertTrue(incHeap.isValidHeap());
        }

        // Decreasing insertions
        CustomMaxHeap decHeap = new CustomMaxHeap();
        for (int i = 90; i >= 10; i -= 10) {
            decHeap.insert(createAssessedEncounter("ENC-" + i, i));
            assertTrue(decHeap.isValidHeap());
        }
        for (int i = 90; i >= 10; i -= 10) {
            assertEquals(i, decHeap.extractMax().getRiskScore().getTotalScore());
            assertTrue(decHeap.isValidHeap());
        }
    }

    @Test
    void dynamicResizingAndManyElementsMaintainHeapOrder() {
        CustomMaxHeap heap = new CustomMaxHeap(4); // Small initial capacity to force resizing

        int n = 100;
        // Pseudo-random deterministic distribution of scores
        for (int i = 0; i < n; i++) {
            int score = (i * 37) % 101; // Scores 0 to 100
            String id = String.format("ENC-%03d", i);
            heap.insert(createAssessedEncounter(id, score));
            assertTrue(heap.isValidHeap());
        }

        assertEquals(n, heap.size());

        Encounter prev = heap.extractMax();
        while (!heap.isEmpty()) {
            assertTrue(heap.isValidHeap());
            Encounter curr = heap.extractMax();

            int prevScore = prev.getRiskScore().getTotalScore();
            int currScore = curr.getRiskScore().getTotalScore();

            assertTrue(prevScore >= currScore,
                    "Expected descending scores, but got " + prevScore + " followed by " + currScore);

            if (prevScore == currScore) {
                assertTrue(prev.getEncounterId().compareTo(curr.getEncounterId()) <= 0,
                        "Expected tie-break ascending IDs, but got " + prev.getEncounterId()
                                + " followed by " + curr.getEncounterId());
            }

            prev = curr;
        }

        assertTrue(heap.isEmpty());
    }

    @Test
    void toSortedListDrainsWithoutMutatingOriginalHeap() {
        CustomMaxHeap heap = new CustomMaxHeap();
        Encounter e1 = createAssessedEncounter("ENC-A", 50);
        Encounter e2 = createAssessedEncounter("ENC-B", 90);
        Encounter e3 = createAssessedEncounter("ENC-C", 70);

        heap.insert(e1);
        heap.insert(e2);
        heap.insert(e3);

        List<Encounter> sorted = heap.toSortedList();

        assertEquals(3, sorted.size());
        assertEquals("ENC-B", sorted.get(0).getEncounterId());
        assertEquals("ENC-C", sorted.get(1).getEncounterId());
        assertEquals("ENC-A", sorted.get(2).getEncounterId());

        // Original heap remains intact
        assertEquals(3, heap.size());
        assertEquals("ENC-B", heap.peek().getEncounterId());
    }

    @Test
    void heapifyConstructorBuildsValidHeapInLinearTime() {
        List<Encounter> list = Arrays.asList(
                createAssessedEncounter("ENC-03", 30),
                createAssessedEncounter("ENC-09", 90),
                createAssessedEncounter("ENC-01", 10),
                createAssessedEncounter("ENC-07", 70),
                createAssessedEncounter("ENC-05", 50)
        );

        CustomMaxHeap heap = new CustomMaxHeap(list);
        assertEquals(5, heap.size());
        assertTrue(heap.isValidHeap());

        List<Encounter> ranked = CustomMaxHeap.rank(list);
        assertEquals(5, ranked.size());
        assertEquals(90, ranked.get(0).getRiskScore().getTotalScore());
        assertEquals(70, ranked.get(1).getRiskScore().getTotalScore());
        assertEquals(50, ranked.get(2).getRiskScore().getTotalScore());
        assertEquals(30, ranked.get(3).getRiskScore().getTotalScore());
        assertEquals(10, ranked.get(4).getRiskScore().getTotalScore());
    }

    @Test
    void clearEmptiesHeap() {
        CustomMaxHeap heap = new CustomMaxHeap();
        heap.insert(createAssessedEncounter("ENC-01", 50));
        heap.insert(createAssessedEncounter("ENC-02", 60));

        assertEquals(2, heap.size());
        heap.clear();

        assertEquals(0, heap.size());
        assertTrue(heap.isEmpty());
        assertNull(heap.peek());
        assertTrue(heap.isValidHeap());
    }

    @Test
    void rejectsNullAndUnassessedEncounters() {
        CustomMaxHeap heap = new CustomMaxHeap();

        assertThrows(IllegalArgumentException.class, () -> heap.insert(null));

        Encounter unassessed = new Encounter(
                "ENC-RAW", "RUN-01", "SAT-01", "DEB-01",
                20.0, 500.0, 5.0, 1
        );
        // Risk score is null before assessment
        assertNull(unassessed.getRiskScore());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> heap.insert(unassessed)
        );
        assertTrue(ex.getMessage().contains("must be assessed"));
    }

    @Test
    void rejectsInvalidInitialCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new CustomMaxHeap(0));
        assertThrows(IllegalArgumentException.class, () -> new CustomMaxHeap(-5));
        assertThrows(NullPointerException.class, () -> new CustomMaxHeap((List<Encounter>) null));
    }

    @Test
    void rebuildRestoresHeapInvariantsAfterExternalScoreMutation() {
        CustomMaxHeap heap = new CustomMaxHeap();

        Encounter e1 = createAssessedEncounter("ENC-01", 90);
        Encounter e2 = createAssessedEncounter("ENC-02", 70);
        Encounter e3 = createAssessedEncounter("ENC-03", 50);

        heap.insert(e1);
        heap.insert(e2);
        heap.insert(e3);

        assertEquals("ENC-01", heap.peek().getEncounterId());
        assertTrue(heap.isValidHeap());

        // External update: e3's risk score changes in-place to 99 (now highest threat)
        RiskScore newScore = new RiskScore(
                99, 1.0, 1.0, 1.0, RiskLabel.CRITICAL, "New critical assessment");
        e3.updateRiskScore(newScore);

        // Before rebuild, the heap array has not adjusted e3 upwards
        // Invoking rebuild() restores the heap order in O(n)
        heap.rebuild();

        assertTrue(heap.isValidHeap());
        assertEquals("ENC-03", heap.peek().getEncounterId());
        assertEquals("ENC-03", heap.extractMax().getEncounterId());
        assertEquals("ENC-01", heap.extractMax().getEncounterId());
        assertEquals("ENC-02", heap.extractMax().getEncounterId());
    }
}

