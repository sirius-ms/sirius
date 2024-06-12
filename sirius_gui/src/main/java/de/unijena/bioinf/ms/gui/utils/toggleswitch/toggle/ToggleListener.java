package de.unijena.bioinf.ms.gui.utils.toggleswitch.toggle;

@FunctionalInterface
public interface ToggleListener {

    void onSelected(boolean selected);

    default void onAnimated(float animated){

    }
}
