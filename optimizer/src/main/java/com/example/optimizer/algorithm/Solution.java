package com.example.optimizer.algorithm;

import com.example.optimizer.model.RunningOptimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Solution {
    private final List<Integer> sequence;

    public Solution(int orderCount, int groupCount) {
        // liczby od 0 do orderCount-1 to identyfikatory zleceń
        // od -1 do -groupCount to zmiana trasy (grupy zleceń)
        sequence = new ArrayList<>(orderCount + groupCount);
        for (int i = 0; i < orderCount; i++) {
            sequence.add(i);
        }
        for (int i = 0; i < groupCount-1; i++) {
            sequence.add(-1 - i);
        }
        // przetasowanie tablicy
        Collections.shuffle(sequence, ThreadLocalRandom.current());
    }

    public Solution(List<Integer> existingSequence) {
        this.sequence = new ArrayList<>(existingSequence);
    }

    public void swapRandom() {
        int index1 = (int) (Math.random() * sequence.size());
        int index2 = (int) (Math.random() * sequence.size());
        Collections.swap(sequence, index1, index2);
    }

    public List<Integer> getSequence() {
        return sequence;
    }

    public Solution copy() {
        return new Solution(this.sequence);
    }

    public double fitness(RunningOptimization optimization) {
        return Calculator.calculateFitness(this, optimization);
    }
}
