package me.jellysquid.mods.sodium.client.gui.options.control;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;

public class ControlElement<T> extends AbstractWidget {
    protected final Option<T> option;

    protected final Dim2i dim;

    protected boolean hovered;

    public ControlElement(Option<T> option, Dim2i dim) {
        this.option = option;
        this.dim = dim;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        String name = this.option.getName().getString();
        String label;

        if (this.hovered && this.font.getWidth(name) > (this.dim.width() - this.option.getControl().getMaxWidth())) {
            name = name.substring(0, Math.min(name.length(), 10)) + "...";
        }

        if (this.option.isAvailable()) {
            if (this.option.hasChanged()) {
                label = Formatting.ITALIC + name + " *";
            } else {
                label = Formatting.WHITE + name;
            }
        } else {
            label = String.valueOf(Formatting.GRAY) + Formatting.STRIKETHROUGH + name;
        }

        this.hovered = this.dim.containsCursor(mouseX, mouseY);

        SodiumGameOptions.ColorTheme colorTheme = SodiumClientMod.options().customization.colorTheme;
        this.drawRect(this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), this.hovered ? colorTheme.enabledHoverColor : colorTheme.enabledColor);
        this.drawString(matrixStack, label, this.dim.x() + 6, this.dim.getCenterY() - 4, colorTheme.enabledTextColor);
    }

    public Option<T> getOption() {
        return this.option;
    }

    public Dim2i getDimensions() {
        return this.dim;
    }
}
