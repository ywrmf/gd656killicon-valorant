package org.mods.gd656killicon.client.gui.elements.entries;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.gui.elements.GDRowRenderer;
import org.mods.gd656killicon.client.gui.elements.GDTextRenderer;
import org.mods.gd656killicon.client.gui.elements.TextInputDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntegerConfigEntry extends GDRowRenderer {
    private static final String RESET_LABEL = "\u21BA";

    private int value;
    private final int defaultValue;
    private final Consumer<Integer> onValueChange;
    private final TextInputDialog textInputDialog;
    private final String configName;
    private final String key;
    private final Predicate<String> validator;
    private final NumericSliderSpec sliderSpec;

    private boolean sliderDragging = false;
    private double sliderDragStartMouseX = 0.0;
    private int sliderDragStartValue = 0;

    public IntegerConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, int initialValue, int defaultValue, Consumer<Integer> onValueChange, TextInputDialog textInputDialog) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, () -> true, null, null);
    }

    public IntegerConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, int initialValue, int defaultValue, Consumer<Integer> onValueChange, TextInputDialog textInputDialog, Supplier<Boolean> activeCondition) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, activeCondition, null, null);
    }

    public IntegerConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, int initialValue, int defaultValue, Consumer<Integer> onValueChange, TextInputDialog textInputDialog, Supplier<Boolean> activeCondition, Predicate<String> validator) {
        this(x1, y1, x2, y2, bgColor, bgAlpha, configName, configId, description, initialValue, defaultValue, onValueChange, textInputDialog, activeCondition, validator, null);
    }

    public IntegerConfigEntry(int x1, int y1, int x2, int y2, int bgColor, float bgAlpha, String configName, String configId, String description, int initialValue, int defaultValue, Consumer<Integer> onValueChange, TextInputDialog textInputDialog, Supplier<Boolean> activeCondition, Predicate<String> validator, NumericSliderSpec sliderSpec) {
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
        this.validator = validator == null ? this::isValidInteger : validator;
        this.sliderSpec = sliderSpec;

        this.addNameColumn(configName, configId, GuiConstants.COLOR_WHITE, GuiConstants.COLOR_GRAY, true, false);

        if (sliderSpec != null) {
            this.addCustomColumn(60, null, this::renderSliderColumn);
        } else {
            this.addColoredColumn(parseColoredText(String.valueOf(this.value)), 60, false, false, (btn) -> openTextInputDialog());
        }

        this.addColumn(RESET_LABEL, GuiConstants.ROW_HEADER_HEIGHT, getResetButtonColor(), true, true, (btn) -> {
            if (this.value == this.defaultValue) {
                return;
            }
            applyValue(this.defaultValue);
        });
    }

    public String getKey() {
        return key;
    }

    public void setValue(int value) {
        this.value = value;
        updateState();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sliderSpec != null && getActiveCondition().get()) {
            ColumnBounds valueBounds = getColumnBounds(1);
            if (valueBounds != null && valueBounds.contains(mouseX, mouseY)) {
                if (button == 1) {
                    openTextInputDialog();
                    return true;
                }
                if (button == 0) {
                    sliderDragging = true;
                    sliderDragStartMouseX = mouseX;
                    sliderDragStartValue = value;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = sliderDragging;
        sliderDragging = false;
        return wasDragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!sliderDragging || sliderSpec == null || button != 0) {
            return false;
        }

        float delta = (float) (mouseX - sliderDragStartMouseX);
        float nextValue = sliderDragStartValue + delta * resolveActiveDragStep();
        applyValue(Math.round(nextValue));
        return true;
    }

    private void openTextInputDialog() {
        if (this.textInputDialog == null) {
            return;
        }
        this.textInputDialog.show(String.valueOf(this.value), this.configName, (newValue) -> {
            try {
                if (this.validator != null && !this.validator.test(newValue)) {
                    return;
                }
                applyValue(Integer.parseInt(newValue));
            } catch (NumberFormatException ignored) {
            }
        }, this.validator);
    }

    private boolean isValidInteger(String text) {
        if (text == null || text.isEmpty() || "-".equals(text)) {
            return false;
        }
        return text.matches("^-?\\d+$");
    }

    private void applyValue(int nextValue) {
        int normalized = sliderSpec != null ? Math.round(sliderSpec.clamp(nextValue)) : nextValue;
        if (normalized == this.value) {
            return;
        }
        this.value = normalized;
        updateState();
        if (this.onValueChange != null) {
            this.onValueChange.accept(this.value);
        }
    }

    private float resolveActiveDragStep() {
        if (sliderSpec == null) {
            return 0.0f;
        }
        float factor = 1.0f;
        if (Screen.hasShiftDown() && Screen.hasControlDown()) {
            factor = 0.1f;
        } else if (Screen.hasControlDown()) {
            factor = 0.25f;
        } else if (Screen.hasShiftDown()) {
            factor = 0.5f;
        }
        return sliderSpec.dragStep() * factor;
    }

    private void updateState() {
        if (sliderSpec == null) {
            Column controlCol = getColumn(1);
            if (controlCol != null) {
                controlCol.coloredTexts = parseColoredText(String.valueOf(this.value));
                controlCol.textRenderer = null;
            }
        }

        Column resetCol = getColumn(2);
        if (resetCol != null) {
            resetCol.color = getResetButtonColor();
            resetCol.text = RESET_LABEL;
            if (resetCol.textRenderer != null) {
                resetCol.textRenderer.setColor(resetCol.color);
                resetCol.textRenderer.setText(resetCol.text);
            }
        }
    }

    private int getResetButtonColor() {
        return this.value == this.defaultValue ? GuiConstants.COLOR_GRAY : GuiConstants.COLOR_GOLD;
    }

    private List<GDTextRenderer.ColoredText> parseColoredText(String text) {
        List<GDTextRenderer.ColoredText> list = new ArrayList<>();
        String processingText = " " + (text == null ? "" : text);

        Pattern pattern = Pattern.compile("<.*?>");
        Matcher matcher = pattern.matcher(processingText);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                list.add(new GDTextRenderer.ColoredText(processingText.substring(lastEnd, matcher.start()), GuiConstants.COLOR_WHITE));
            }
            list.add(new GDTextRenderer.ColoredText(matcher.group(), GuiConstants.COLOR_GOLD));
            lastEnd = matcher.end();
        }
        if (lastEnd < processingText.length()) {
            list.add(new GDTextRenderer.ColoredText(processingText.substring(lastEnd), GuiConstants.COLOR_WHITE));
        }

        return list;
    }

    private void renderSliderColumn(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int trackLeft = x + 4;
        int trackRight = x + width - 4;
        int trackTop = y + height - 7;
        int trackBottom = trackTop + 3;
        float range = sliderSpec.max() - sliderSpec.min();
        float normalized = range <= 0.0001f ? 0.0f : (value - sliderSpec.min()) / range;
        normalized = Math.max(0.0f, Math.min(1.0f, normalized));
        int trackWidth = Math.max(1, trackRight - trackLeft);
        int knobX = trackLeft + Math.round(trackWidth * normalized);
        int fillColor = getActiveCondition().get() ? GuiConstants.COLOR_GOLD : GuiConstants.COLOR_GRAY;
        int textColor = getActiveCondition().get() ? GuiConstants.COLOR_WHITE : GuiConstants.COLOR_GRAY;

        guiGraphics.fill(trackLeft, trackTop, trackRight, trackBottom, 0x60303030);
        guiGraphics.fill(trackLeft, trackTop, Math.max(trackLeft + 1, knobX), trackBottom, fillColor);
        guiGraphics.fill(knobX - 1, trackTop - 2, knobX + 1, trackBottom + 2, sliderDragging ? GuiConstants.COLOR_WHITE : 0xFFE6D18A);

        String text = Integer.toString(value);
        Minecraft minecraft = Minecraft.getInstance();
        int textWidth = minecraft.font.width(text);
        guiGraphics.drawString(minecraft.font, text, x + Math.max(2, (width - textWidth) / 2), y + 3, textColor, false);
    }
}
