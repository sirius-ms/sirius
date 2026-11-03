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

package de.unijena.bioinf.ms.gui.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MessageBanner#showTransient} backs the "preset saved" confirmation (#485): visible at once,
 * gone again without the user dismissing anything.
 */
public class MessageBannerTest {

    private static final int SHORT = 60;

    @Test
    public void transientMessageIsVisibleImmediately() {
        MessageBanner banner = new MessageBanner(false);

        banner.showTransient("Preset \"my preset\" saved.", MessageBanner.BannerType.GOOD, 10_000);

        assertTrue(banner.isVisible());
        assertEquals("Preset \"my preset\" saved.", banner.getText());
    }

    @Test
    public void transientMessageHidesItself() throws InterruptedException {
        MessageBanner banner = new MessageBanner(false);

        banner.showTransient("saved", MessageBanner.BannerType.GOOD, SHORT);

        assertTrue(banner.isVisible(), "still visible right after showing");
        assertTrue(awaitHidden(banner), "banner should hide itself once the delay elapsed");
    }

    /**
     * Saving twice in quick succession must not let the first timer hide the second message.
     */
    @Test
    public void asecondMessageRestartsTheTimer() throws InterruptedException {
        MessageBanner banner = new MessageBanner(false);

        banner.showTransient("first", MessageBanner.BannerType.GOOD, SHORT);
        Thread.sleep(SHORT / 2);
        banner.showTransient("second", MessageBanner.BannerType.GOOD, 10_000);

        Thread.sleep(SHORT * 2); //the first timer would have fired by now
        assertTrue(banner.isVisible(), "the first message's timer must not hide the second message");
        assertEquals("second", banner.getText());
    }

    @Test
    public void hideBannerCancelsAPendingAutoHide() throws InterruptedException {
        MessageBanner banner = new MessageBanner(false);

        banner.showTransient("saved", MessageBanner.BannerType.GOOD, SHORT);
        banner.hideBanner();
        assertFalse(banner.isVisible());

        //re-showing without a timer must survive the cancelled one firing
        banner.update("permanent", MessageBanner.BannerType.WARNING, true);
        Thread.sleep(SHORT * 3);
        assertTrue(banner.isVisible(), "a cancelled auto hide must not hide a later message");
    }

    private static boolean awaitHidden(MessageBanner banner) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (!banner.isVisible())
                return true;
            Thread.sleep(10);
        }
        return false;
    }
}
