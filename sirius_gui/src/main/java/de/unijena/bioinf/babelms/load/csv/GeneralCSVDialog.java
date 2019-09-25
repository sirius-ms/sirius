package de.unijena.bioinf.babelms.load.csv;

import com.google.common.base.Predicate;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

public class GeneralCSVDialog extends JPanel {

    public static void main(String[] args) {

        final JFrame frame = new JFrame();
        final GeneralCSVDialog d = new GeneralCSVDialog();
        frame.add(d);

        d.addField(new Field("InChI", 1, 1));
        d.addField(new Field("SMILES", 0, 1));
        d.addField(new Field("ID", 0, 1));
        d.addField(new Field("DB link", false));
        d.loadPreview(Arrays.asList("a,b,c,d", "e,f,g,h", "i,j,k,l"), true);
        frame.pack();
        frame.setVisible(true);

    }

    protected static final String[] SEP_NAMES  = new String[]{"TAB", ",", ";", ":", "whitespace"},
                                    QUOT_NAMES = new String[]{"\"double\"", "'single'"};
    protected static final char[] SEP_CHARS = new char[]{'\t', ',', ';', ':', ' '},
                                    QUOT_CHARS = new char[]{'"', '\''};

    protected JTable table;
    protected Vector<Field> fields;
    protected HashMap<String, Field> fieldMap;

    protected JComboBox<String> columnSeparator, quotSeparator;
    protected JButton autoDetect;
    protected CSVImporterModel model;
    protected Predicate<GeneralCSVDialog> predicate;
    protected JButton importButton, cancelButton;

    public static GeneralCSVDialog makeCsvImporterDialog(Window owner, List<String> lines, Field... fields) {
        return makeCsvImporterDialog(owner,lines,null,fields);
    }

    public static GeneralCSVDialog makeCsvImporterDialog(Window owner, List<String> lines, Predicate<GeneralCSVDialog> isValid, Field... fields) {
        final JDialog dia = owner instanceof JFrame ? new JDialog((JFrame)owner, "Import CSV", true) : new JDialog(owner, "Import CSV", Dialog.ModalityType.DOCUMENT_MODAL);
        final GeneralCSVDialog gn = new GeneralCSVDialog();
        gn.predicate = isValid;
        for (Field f : fields) gn.addField(f);
        gn.loadPreview(lines, true);
        final SimpleCsvParser[] parserBox = new SimpleCsvParser[1];
        dia.add(gn);
        gn.getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parserBox[0] = null;
                dia.dispose();
            }
        });
        gn.getImportButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gn.isValidInput()) {
                    parserBox[0] = gn.model.parser;
                    dia.dispose();
                }
            }
        });
        dia.pack();
        dia.setVisible(true);
        return parserBox[0]!=null ? gn : null;
    }

    public GeneralCSVDialog() {
        super();

        this.fields = new Vector<>();
        this.fieldMap = new HashMap<>();
        fields.add(new Field("unknown", 0, Integer.MAX_VALUE, new FieldCheck() {
            @Override
            public int check(List<String> values, int col) {
                return 0;
            }
        }));
        fieldMap.put("unknown", fields.get(0));

        setLayout(new BorderLayout());
        this.model = new CSVImporterModel(fields);
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());
        this.table = new JTable(model);
        tablePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Preview"));
        tablePanel.add(table, BorderLayout.CENTER);
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
        Box parserPanel = Box.createHorizontalBox();
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        parserPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"CSV settings"));

        Box Sep = Box.createHorizontalBox();
        Sep.add(new JLabel("column separator "));
        columnSeparator = new JComboBox<>(SEP_NAMES);
        Sep.add(columnSeparator);

        Box Quot = Box.createHorizontalBox();
        Quot.add(new JLabel("quotation mark "));
        quotSeparator = new JComboBox<>(QUOT_NAMES);
        Quot.add(quotSeparator);

        parserPanel.add(Sep);
        parserPanel.add(Box.createHorizontalStrut(12));
        parserPanel.add(Quot);
        parserPanel.add(Box.createHorizontalStrut(12));
        autoDetect = new JButton("auto detect");
        parserPanel.add(autoDetect);
        parserPanel.add(Box.createHorizontalGlue());
        add(parserPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);

        final ItemListener il = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                model.setParser(makeParser());
            }
        };
        quotSeparator.addItemListener(il);
        columnSeparator.addItemListener(il);
        autoDetect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setPreview(model.preview, true);
                enforceParser(model.parser);
                table.repaint();
            }
        });

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDragEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);
        final JComboBox<Field> combo = new JComboBox<Field>(fields);
        //combo.setEditable(false);
        table.setCellEditor(new DefaultCellEditor(combo));
        table.setDefaultEditor(String.class, new DefaultCellEditor(combo));

        importButton = new JButton("Confirm");
        cancelButton = new JButton("Cancel");
        Box b = Box.createHorizontalBox();
        b.add(importButton);
        b.add(Box.createHorizontalStrut(16));
        b.add(cancelButton);
        add(b, BorderLayout.SOUTH);
        updateImportButton();
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateImportButton();
            }
        });

    }

    public int getFirstColumnFor(Field f) {
        for (int j=0; j < model.assignments.length; ++j) {
            if (model.assignments[j]==f) return j;
        }
        return -1;
    }

    public boolean isValidInput() {
        return model.isValid();
    }

    public JButton getImportButton() {
        return importButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    private void updateImportButton() {
        final Field f = model.getInvalidField();
        if (f==null) {
            if (predicate!=null) {
                importButton.setEnabled(predicate.apply(this));
            } else importButton.setEnabled(true);
        }
        else {
            importButton.setEnabled(false);
            importButton.setToolTipText(f.validationString());
        }
    }

    public void addField(Field field) {
        fields.add(field);
        fieldMap.put(field.name, field);
        updateImportButton();
    }

    public void enforceParser(SimpleCsvParser parser) {
        for (int i=0; i < QUOT_CHARS.length; ++i) {
            if (parser.quotationChar==QUOT_CHARS[i]) {
                quotSeparator.setSelectedIndex(i); break;
            }
        }
        for (int i=0; i < SEP_CHARS.length; ++i) {
            if (parser.separator==SEP_CHARS[i]) {
                columnSeparator.setSelectedIndex(i); break;
            }
        }
        model.setParser(parser);
    }

    public SimpleCsvParser makeParser() {
        return new SimpleCsvParser(SEP_CHARS[columnSeparator.getSelectedIndex()], QUOT_CHARS[quotSeparator.getSelectedIndex()]);
    }

    public void loadPreview(List<String> preview, boolean guessParser) {
        model.setPreview(preview, guessParser);
        if (guessParser) enforceParser(model.parser);
    }

    public SimpleCsvParser getParser() {
        return model.parser;
    }

    protected static class TableColumRenderer implements TableCellRenderer {


        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return null;
        }
    }

    public static class CSVImporterModel implements TableModel{
        protected List<String> preview;
        protected List<String[]> content;
        protected Vector<Field> fields;
        protected Field[] assignments;
        protected SimpleCsvParser parser;
        protected List<TableModelListener> listeners = new ArrayList<>();
        public CSVImporterModel(Vector<Field> fields) {
            this.preview = new ArrayList<>();
            this.content = new ArrayList<>();
            this.parser = new SimpleCsvParser();
            this.listeners = new ArrayList<>();
            this.fields = fields;
            this.assignments = new Field[0];
        }

        protected Field getInvalidField() {
            int[] counts = new int[fields.size()];
            for (int i=0,n=getColumnCount(); i < n; ++i) {
                for (int j=0; j < fields.size(); ++j) {
                    if (fields.get(j).equals(assignments[i])) {
                        ++counts[j];
                        break;
                    }
                }
            }
            for (int i=0; i < counts.length; ++i) {
                if (counts[i] < fields.get(i).minNumber || counts[i] > fields.get(i).maxNumber) return fields.get(i);
            }
            return null;
        }

        protected boolean isValid() {
            return getInvalidField()==null;
        }

        protected void assign(int column, Field f) {
            assign(column,f,false,true);
        }

        protected void assign(int column, Field f, boolean autocheck, boolean enforceAssign) {

            if (f.maxNumber < getColumnCount()) {
                int found=0;
                for (int c=0, n = getColumnCount(); c < n; ++c) {
                    if (assignments[c]==f) {
                        ++found;
                    }
                }
                if (!autocheck && !enforceAssign) return;
                int lowestCheck = -1;
                int torem = -1;
                if (found >= f.maxNumber) {
                    for (int c=0, n = getColumnCount(); c < n; ++c) {
                        if (assignments[c]==f) {

                            if (!autocheck) {
                                assignments[c] = fields.get(0);
                                break;
                            } else if (torem<0) {
                                torem = c;
                                lowestCheck = f.check.check(getCol(c), c);
                            } else {
                                int lc = f.check.check(getCol(c), c);
                                if (lc < lowestCheck) {
                                    torem = c;
                                    lowestCheck = lc;
                                }
                            }
                        }
                    }
                }
                if (autocheck) {
                    if (enforceAssign) {
                        assignments[torem] = fields.get(0);
                    } else if (f.check.check(getCol(column),column) < lowestCheck) {
                        return;
                    }
                }
            }

            assignments[column] = f;
            for (TableModelListener l : listeners) {
                l.tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
            }
        }

        protected void setPreview(List<String> preview, boolean guessParser) {
            this.preview = preview;
            if (guessParser ) this.parser = guessSeparator(preview);
            this.content = parseContent();
            autoassign();
            for (TableModelListener l : listeners) {
                l.tableChanged(new TableModelEvent(this));
                l.tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
            }
        }

        private List<String> getCol(int col) {
            final List<String> lines = new ArrayList<>();
            for (String[] l : content) lines.add(l[col]);
            return lines;
        }

        private void autoassign() {
            for (int i=0; i < assignments.length; ++i) {
                final List<String> lines = getCol(i);
                Field bestField = fields.get(0);
                int best = fields.get(0).check.check(lines, i);
                for (int k=1; k < fields.size(); ++k) {
                    int n = fields.get(k).check.check(lines, i);
                    if (n>best) {
                        best=n;
                        bestField = fields.get(k);
                    }
                }
                assign(i, bestField, true,false);
            }
        }

        protected void setParser(SimpleCsvParser parser) {
            if (!parser.equals(this.parser)) {
                this.parser = parser;
                this.content = parseContent();
                autoassign();
                for (TableModelListener l : listeners) {
                    l.tableChanged(new TableModelEvent(this));
                    l.tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
                }
            }
        }

        private List<String[]> parseContent() {
            List<String[]> c = new ArrayList<>();
            for (String line : preview) {
                c.add(parser.parseLine(line));
            }
            int cols = c.size()==0 ? 0 : c.get(0).length;
            this.assignments = Arrays.copyOf(assignments, cols);
            for (int i=0; i<assignments.length; ++i) if (assignments[i]==null) assignments[i] = fields.get(0);
            return c;
        }

        private SimpleCsvParser guessSeparator(List<String> preview) {
            return SimpleCsvParser.guessSeparator(preview);
        }

        @Override
        public int getRowCount() {
            return content.size();
        }

        @Override
        public int getColumnCount() {
            if (content.size()==0) return 0;
            return content.get(0).length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return assignments[columnIndex].name;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return content.get(rowIndex)[columnIndex];
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            // we just change fields
            for (Field f : fields) {
                if (f.equals(aValue)) {
                    assign(columnIndex, f);
                    return;
                }
            }
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }
    }

    public static class Field {

        protected String name;
        protected int minNumber, maxNumber;
        protected FieldCheck check;

        public Field(String name, int minNumber, int maxNumber, FieldCheck check) {
            this.name = name;
            this.minNumber = minNumber;
            this.maxNumber = maxNumber;
            this.check = check;
        }

        public Field(String name, int minNumber, int maxNumber) {
            this(name, minNumber, maxNumber, new FieldCheck() {
                @Override
                public int check(List<String> values, int column) {
                    return -1;
                }
            });
        }

        public Field(String name, boolean mandatory) {
            this(name, mandatory ? 1 : 0, Integer.MAX_VALUE);
        }

        public String toString(){
            return name;
        }

        public String validationString() {
            if (minNumber>0) {
                if (maxNumber >= Integer.MAX_VALUE) {
                    return "You have to specify " + (minNumber==1 ? "a" : "at least " + minNumber) + " " + name + " column";
                } else if (maxNumber==1) {
                    return "You have to specify exactly one " + name + " column";
                } else {
                    return "You have to specify between " + minNumber  + " and " + maxNumber + " of " + name + " columns";
                }
            } else {
                if (maxNumber >= Integer.MAX_VALUE) {
                    return "";
                } else {
                    return "You have to specify not more than " + maxNumber + " of "+ name + " columns";
                }
            }
        }
    }

    public interface FieldCheck {
        /**
         * returns a number representing how likely the given values belong the this field
         * -1 means - unlikely. Everything above 0 is likely.
         * 0 is reserved for the unknown field
         */
        int check(List<String> values, int column);
    }

}
