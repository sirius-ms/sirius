package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.export.ExportDBOptions;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.export.Format;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.sirius.ms.sdk.model.SearchableDatabase;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseExportConfigPanel extends SubToolConfigPanel<ExportDBOptions> {

    public DatabaseExportConfigPanel(SearchableDatabase db) {
        super(ExportDBOptions.class);
        parameterBindings.put("db", db::getDatabaseId);

        final TwoColumnPanel params = new TwoColumnPanel();

        String defaultOutputLocation = PropertyManager.getProperty(SiriusProperties.DEFAULT_SAVE_CUSTOM_DB_PATH, null, "");

        FileChooserPanel outputLocation = new FileChooserPanel(
                defaultOutputLocation, defaultOutputLocation,
                JFileChooser.FILES_AND_DIRECTORIES, JFileChooser.SAVE_DIALOG);
        parameterBindings.put("output", outputLocation::getFilePath);
        getOptionTooltip("output").ifPresent(outputLocation::setToolTipText);
        outputLocation.field.setPlaceholder("Pick a directory");
        outputLocation.field.setPreferredSize(new Dimension(300, outputLocation.field.getPreferredSize().height));
        params.addNamed("Destination", outputLocation);

        JComboBox<Format> formatComboBox = makeGenericOptionComboBox("format", Format.class);
        params.addNamed("Format", formatComboBox);

        add(params);
    }

    @Override
    public String toolCommand() {
        return PicoUtils.getCommand(CustomDBOptions.class).name();
    }

    @Override
    public List<String> asParameterList() {
        List<String> params = new ArrayList<>();
        params.add(super.toolCommand());
        params.addAll(super.asParameterList());
        return params;
    }
}
