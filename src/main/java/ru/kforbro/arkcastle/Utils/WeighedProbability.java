package ru.kforbro.arkcastle.Utils;

import java.util.Map;

public class WeighedProbability {
    public static Integer pickWeighedProbability(Map<Integer, Double> weighedValues) {
        double totalWeight = 0.0;
        for (Integer integer : weighedValues.keySet()) {
            totalWeight += weighedValues.get(integer).doubleValue();
        }
        Integer selectedInteger = null;
        double random = Math.random() * totalWeight;
        for (Integer integer : weighedValues.keySet()) {
            if (!((random -= weighedValues.get(integer).doubleValue()) <= 0.0)) continue;
            selectedInteger = integer;
            break;
        }
        return selectedInteger;
    }
}