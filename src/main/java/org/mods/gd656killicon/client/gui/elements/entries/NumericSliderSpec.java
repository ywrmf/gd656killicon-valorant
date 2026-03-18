package org.mods.gd656killicon.client.gui.elements.entries;

import net.minecraft.util.Mth;

public record NumericSliderSpec(float min, float max, float dragStep) {
    public NumericSliderSpec {
        if (max < min) {
            throw new IllegalArgumentException("Slider max must be greater than or equal to min");
        }
        if (dragStep <= 0.0f) {
            throw new IllegalArgumentException("Slider drag step must be positive");
        }
    }

    public float clamp(float value) {
        return Mth.clamp(value, min, max);
    }
}
