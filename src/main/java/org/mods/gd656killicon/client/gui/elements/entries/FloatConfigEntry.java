package org.mods.gd656killicon.client.gui.elements.entries;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.TextInputDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FloatConfigEntry extends GDRowRenderer {
    private static final float VALUE_EPSILON = 0.0001f;
    private static final float SHIFT_DRAG_MULTIPLIER = 0.25f;
    private static final float CTRL_DRAG_MULTIPLIER = 0.10f;
    private static final int DEFAULT_SLIDER_WIDTH = 112;

    public record SliderSpec(float minValue, float maxValue, float step, int width) {
        public SliderSpec {
            if (maxValue <= minValue) {
                throw new IllegalArgumentException("Slider maxValue must be greater than minValue");
            }
            if (step <= 0.0f) {
                step = 0.01f;
            }
            if (width <= 0) {
                width = DEFAULT_SLIDER_WIDTH;
            }
        }
    }

    private float value;
    private final float defaultValue;
    private final Consumer<Float> onValueChange;
    private final String key;
    private final TextInputDialog textInputDialog;
    private final String configName;
    private final SliderSpec sliderSpec;

    private boolean sliderDragging = false;
    private double sliderDragStartMouseX = 0.0;
    private float sliderDragStartValue = 0.0f;
    private int sliderX1 = 0;
    private int sliderY1 = 0;
    private int sliderX2 = 0;
    private int sliderY2 = 0;

    public FloatConfigEntry(
        int x1,
        int y1,
        int x2,
        int y2,
        int bgColor,
        float bgAlpha,
        String configName,
        String configId,
        String description,
        float initialValue,
        float defaultValue,
        Consumer<Float> onValueChange,
        TextInputDialog textInputDialog
    ) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, () -> true, (SliderSpec) null);
    }

    public FloatConfigEntry(
        int x1,
        int y1,
        int x2,
        int y2,
        int bgColor,
        float bgAlpha,
        String configName,
        String configId,
        String description,
        float initialValue,
        float defaultValue,
        Consumer<Float> onValueChange,
        TextInputDialog textInputDialog,
        Supplier<Boolean> activeCondition
    ) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, activeCondition, (SliderSpec) null);
    }

    public FloatConfigEntry(
        int x1,
        int y1,
        int x2,
        int y2,
        int bgColor,
        float bgAlpha,
        String configName,
        String configId,
        String description,
        float initialValue,
        float defaultValue,
        Consumer<Float> onValueChange,
        TextInputDialog textInputDialog,
        Supplier<Boolean> activeCondition,
        NumericSliderSpec sliderSpec
    ) {
        this(
            x1,
            y1,
            x2,
            y2,
            bgColor,
            bgAlpha,
            configName,
            configId,
            description,
            initialValue,
            defaultValue,
            onValueChange,
            textInputDialog,
            activeCondition,
            sliderSpec == null ? null : new SliderSpec(sliderSpec.min(), sliderSpec.max(), sliderSpec.dragStep(), DEFAULT_SLIDER_WIDTH)
        );
    }

    public FloatConfigEntry(
        int x1,
        int y1,
        int x2,
        int y2,
        int bgColor,
        float bgAlpha,
        String configName,
        String configId,
        String description,
        float initialValue,
        float defaultValue,
        Consumer<Float> onValueChange,
        TextInputDialog textInputDialog,
        Supplier<Boolean> activeCondition,
        SliderSpec sliderSpec
    ) {
        super(x1, y1, x2, y2, bgColor, bgAlpha, false);
        this.key = configId;
        this.setActiveCondition(activeCondition);
        this.setSeparateFirstColumn(true);
        this.setHoverInfo(configName, "   " + description);
        this.value = initialValue;
        this.defaultValue = defaultValue;
        this.onValueChange = onValueChange;
        this.textInputDialog = textInputDialog;
        this.configName = configName;
        this.sliderSpec = sliderSpec;

        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        this.addColoredColumn(parseColoredText(this.value), 60, false, false, (btn) -> {
            if (this.textInputDialog != null) {
                String initialText = String.format(Locale.ROOT, "%.2f", this.value);
                this.textInputDialog.show(initialText, this.configName, (newValue) -> {
                    try {
                        applyManualValue(Float.parseFloat(newValue));
                    } catch (NumberFormatException ignored) {
                    }
                }, this::isValidFloat);
            }
        });

        if (this.sliderSpec != null) {
            this.addCustomColumn(this.sliderSpec.width(), null, this::renderSliderColumn);
        }

        this.addColumn("R", GuiConstants.ROW_HEADER_HEIGHT, getResetButtonColor(), true, true, (btn) -> {
            if (Math.abs(this.value - this.defaultValue) < VALUE_EPSILON) {
                return;
            }
            applyManualValue(this.defaultValue);
        });
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && getActiveCondition().get() && isSliderHovered(mouseX, mouseY)) {
            sliderDragging = true;
            sliderDragStartMouseX = mouseX;
            sliderDragStartValue = value;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!sliderDragging || button != 0) {
            return false;
        }
        sliderDragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!sliderDragging || button != 0 || sliderSpec == null) {
            return false;
        }

        float sliderWidth = Math.max(1.0f, sliderX2 - sliderX1);
        float sliderRange = sliderSpec.maxValue() - sliderSpec.minValue();
        float rawValue = sliderDragStartValue
            + (float) ((mouseX - sliderDragStartMouseX) / sliderWidth) * sliderRange * resolveDragSpeedMultiplier();
        applySliderValue(rawValue);
        return true;
    }

    private void renderSliderColumn(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        sliderX1 = x;
        sliderY1 = y;
        sliderX2 = x + width;
        sliderY2 = y + height;

        if (sliderSpec == null) {
            return;
        }

        boolean active = getActiveCondition().get();
        int trackPadding = 6;
        int trackX1 = x + trackPadding;
        int trackX2 = x + width - trackPadding;
        int trackY1 = y + height / 2 - 1;
        int trackY2 = trackY1 + 3;
        int trackColor = active ? 0x60FFFFFF : 0x40333333;
        int fillColor = active ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_DARK_GRAY;
        int thumbColor = active
            ? (sliderDragging ? GuiConstants.COLOR_GOLD_ORANGE : GuiConstants.COLOR_WHITE)
            : GuiConstants.COLOR_DARK_GRAY;

        guiGraphics.fill(trackX1, trackY1, trackX2, trackY2, trackColor);

        float normalized = resolveNormalizedValue();
        int thumbCenterX = trackX1 + Math.round((trackX2 - trackX1) * normalized);
        guiGraphics.fill(trackX1, trackY1, thumbCenterX, trackY2, fillColor);
        guiGraphics.fill(thumbCenterX - 2, trackY1 - 3, thumbCenterX + 2, trackY2 + 3, thumbColor);
    }

    private boolean isSliderHovered(double mouseX, double mouseY) {
        return sliderSpec != null
            && mouseX >= sliderX1
            && mouseX <= sliderX2
            && mouseY >= sliderY1
            && mouseY <= sliderY2;
    }

    private float resolveNormalizedValue() {
        float clamped = clamp(value, sliderSpec.minValue(), sliderSpec.maxValue());
        return (clamped - sliderSpec.minValue()) / (sliderSpec.maxValue() - sliderSpec.minValue());
    }

    private float resolveDragSpeedMultiplier() {
        float multiplier = 1.0f;
        if (Screen.hasShiftDown()) {
            multiplier *= SHIFT_DRAG_MULTIPLIER;
        }
        if (Screen.hasControlDown()) {
            multiplier *= CTRL_DRAG_MULTIPLIER;
        }
        return multiplier;
    }

    private float resolveSliderStep() {
        float step = sliderSpec.step();
        if (Screen.hasShiftDown()) {
            step *= 0.25f;
        }
        if (Screen.hasControlDown()) {
            step *= 0.10f;
        }
        return Math.max(0.01f, step);
    }

    private void applyManualValue(float newValue) {
        if (Math.abs(newValue - this.value) < VALUE_EPSILON) {
            return;
        }
        this.value = newValue;
        updateState();
        if (this.onValueChange != null) {
            this.onValueChange.accept(this.value);
        }
    }

    private void applySliderValue(float rawValue) {
        float clamped = clamp(rawValue, sliderSpec.minValue(), sliderSpec.maxValue());
        float snapped = snapToStep(clamped, sliderSpec.minValue(), resolveSliderStep());
        applyManualValue(snapped);
    }

    private boolean isValidFloat(String text) {
        if (text == null || text.isEmpty() || "-".equals(text)) {
            return false;
        }
        return text.matches("^-?\\d+(\\.\\d{0,2})?$") || text.matches("^-?\\.\\d{1,2}$");
    }

    private void updateState() {
        Column controlCol = getColumn(1);
        if (controlCol != null) {
            controlCol.coloredTexts = parseColoredText(this.value);
            controlCol.textRenderer = null;
        }

        int resetColumnIndex = sliderSpec != null ? 3 : 2;
        Column resetCol = getColumn(resetColumnIndex);
        if (resetCol != null) {
            resetCol.color = getResetButtonColor();
            resetCol.text = "R";
            if (resetCol.textRenderer != null) {
                resetCol.textRenderer.setColor(resetCol.color);
                resetCol.textRenderer.setText(resetCol.text);
            }
        }
    }

    private int getResetButtonColor() {
        return Math.abs(value - defaultValue) < VALUE_EPSILON ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
    }

    private List<GDTextRenderer.ColoredText> parseColoredText(float val) {
        List<GDTextRenderer.ColoredText> list = new ArrayList<>();
        String text = String.format(Locale.ROOT, "%.2f", val);
        int dotIndex = text.indexOf('.');
        if (dotIndex != -1) {
            String intPart = text.substring(0, dotIndex);
            list.add(new GDTextRenderer.ColoredText(" " + intPart, GuiConstants.COLOR_WHITE));
            list.add(new GDTextRenderer.ColoredText(".", GuiConstants.COLOR_WHITE));
            String decPart = text.substring(dotIndex + 1);
            list.add(new GDTextRenderer.ColoredText(decPart, GuiConstants.COLOR_GRAY));
        } else {
            list.add(new GDTextRenderer.ColoredText(" " + text, GuiConstants.COLOR_WHITE));
        }
        return list;
    }

    private float snapToStep(float value, float origin, float step) {
        float snapped = Math.round((value - origin) / step) * step + origin;
        return Math.round(snapped * 100.0f) / 100.0f;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
