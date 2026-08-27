/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassDetailView.ClassifClass;
import io.sirius.ms.sdk.model.CompoundClass;
import org.junit.jupiter.api.Test;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A compound class box opens its ClassyFire page when clicked, but gave no sign of being clickable (#90).
 */
public class ClassifClassLinkTest {

    private static CompoundClass compoundClass() {
        CompoundClass cc = new CompoundClass();
        cc.setId(3025);
        cc.setName("Flavonoid glycosides");
        cc.setLevel("class");
        cc.setDescription("Compounds containing a flavonoid moiety glycosidically linked to a sugar.");
        cc.setProbability(0.87);
        return cc;
    }

    /** gui is only touched when a click is actually dispatched, which these tests do not do */
    private static ClassifClass box() {
        return new ClassifClass(compoundClass(), true, null);
    }

    @Test
    public void urlPointsAtTheClassyFireNodeZeroPadded() {
        assertEquals(URI.create("http://classyfire.wishartlab.com/tax_nodes/C0003025"),
                ClassifClass.classyFireUrl(compoundClass()));
    }

    @Test
    public void theBoxShowsAHandCursor() {
        assertEquals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), box().getCursor());
    }

    @Test
    public void theTooltipRevealsTheTarget() {
        String tooltip = box().getToolTipText();
        assertTrue(tooltip.contains("http://classyfire.wishartlab.com/tax_nodes/C0003025"), tooltip);
        assertTrue(tooltip.contains("87 %"), "the probability must survive: " + tooltip);
        assertTrue(tooltip.contains("glycosidically linked"), "the description must survive: " + tooltip);
    }

    @Test
    public void hoveringUnderlinesTheName() {
        ClassifClass box = box();
        assertFalse(box.isHovered(), "not underlined until the pointer arrives");

        box.mouseEntered(mouseEvent(box));
        assertTrue(box.isHovered());

        box.mouseExited(mouseEvent(box));
        assertFalse(box.isHovered());
    }

    private static MouseEvent mouseEvent(ClassifClass box) {
        return new MouseEvent(box, MouseEvent.MOUSE_ENTERED, 0L, 0, 1, 1, 0, false);
    }
}
