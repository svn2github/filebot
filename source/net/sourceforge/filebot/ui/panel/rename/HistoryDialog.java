
package net.sourceforge.filebot.ui.panel.rename;


import static java.awt.Font.*;
import static java.util.Collections.*;
import static javax.swing.JOptionPane.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.RowSorter.SortKey;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.panel.rename.History.Element;
import net.sourceforge.filebot.ui.panel.rename.History.Sequence;
import net.sourceforge.filebot.ui.transfer.FileExportHandler;
import net.sourceforge.filebot.ui.transfer.FileTransferablePolicy;
import net.sourceforge.filebot.ui.transfer.LoadAction;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.filebot.ui.transfer.TransferablePolicy.TransferAction;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.FileUtilities.ExtensionFileFilter;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LazyDocumentListener;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;
import net.sourceforge.tuned.ui.notification.SeparatorBorder.Position;


class HistoryDialog extends JDialog {
	
	private final JLabel infoLabel = new JLabel();
	
	private final JTextField filterEditor = new JTextField();
	
	private final SequenceTableModel sequenceModel = new SequenceTableModel();
	
	private final ElementTableModel elementModel = new ElementTableModel();
	
	private final JTable sequenceTable = createTable(sequenceModel);
	
	private final JTable elementTable = createTable(elementModel);
	
	
	public HistoryDialog(Window owner) {
		super(owner, "Rename History", ModalityType.DOCUMENT_MODAL);
		
		// bold title label in header
		JLabel title = new JLabel(this.getTitle());
		title.setFont(title.getFont().deriveFont(BOLD));
		
		JPanel header = new JPanel(new MigLayout("insets dialog, nogrid, fillx"));
		
		header.setBackground(Color.white);
		header.setBorder(new SeparatorBorder(1, new Color(0xB4B4B4), new Color(0xACACAC), GradientStyle.LEFT_TO_RIGHT, Position.BOTTOM));
		
		header.add(title, "wrap");
		header.add(infoLabel, "gap indent*2, wrap paragraph:push");
		
		JPanel content = new JPanel(new MigLayout("fill, insets dialog, nogrid", "", "[pref!][150px:pref:200px][200px:pref:max, grow][pref!]"));
		
		content.add(new JLabel("Filter:"), "gap indent:push");
		content.add(filterEditor, "wmin 120px, gap rel");
		content.add(new JButton(clearFilterAction), "w 24px!, h 24px!, gap right indent, wrap");
		
		content.add(createScrollPaneGroup("Sequences", sequenceTable), "growx, wrap paragraph");
		content.add(createScrollPaneGroup("Elements", elementTable), "growx, wrap paragraph");
		
		// use ADD by default
		Action importAction = new LoadAction("Import", null, importHandler) {
			
			@Override
			public TransferAction getTransferAction(ActionEvent evt) {
				// if SHIFT was pressed when the button was clicked, assume PUT action, use ADD by default
				return ((evt.getModifiers() & ActionEvent.SHIFT_MASK) != 0) ? TransferAction.PUT : TransferAction.ADD;
			}
		};
		
		content.add(new JButton(importAction), "wmin button, hmin 25px, gap indent, sg button");
		content.add(new JButton(new SaveAction("Export", null, exportHandler)), "gap rel, sg button");
		content.add(new JButton(closeAction), "gap left unrel:push, gap right indent, sg button");
		
		JComponent pane = (JComponent) getContentPane();
		pane.setLayout(new MigLayout("fill, insets 0, nogrid"));
		
		pane.add(header, "hmin 60px, growx, dock north");
		pane.add(content, "grow");
		
		// initialize selection modes
		sequenceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		elementTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		// bind element model to selected sequence
		sequenceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				
				if (sequenceTable.getSelectedRow() >= 0) {
					int index = sequenceTable.convertRowIndexToModel(sequenceTable.getSelectedRow());
					
					elementModel.setData(sequenceModel.getRow(index).elements());
				}
			}
		});
		
		// clear sequence selection when elements are selected
		elementTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (elementTable.getSelectedRow() >= 0) {
					// allow  selected rows only in one of the two tables
					sequenceTable.getSelectionModel().clearSelection();
				}
			}
		});
		
		// sort by number descending
		sequenceTable.getRowSorter().setSortKeys(singletonList(new SortKey(0, SortOrder.DESCENDING)));
		
		// change date format
		sequenceTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
			
			private final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
			
			
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return super.getTableCellRendererComponent(table, format.format(value), isSelected, hasFocus, row, column);
			}
		});
		
		// display broken status in second column
		elementTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				
				// reset icon
				setIcon(null);
				
				if (column == 1) {
					if (elementModel.isBroken(table.convertRowIndexToModel(row))) {
						setIcon(ResourceManager.getIcon("status.link.broken"));
					} else {
						setIcon(ResourceManager.getIcon("status.link.ok"));
					}
				}
				
				return this;
			}
		});
		
		// update sequence and element filter on change
		filterEditor.getDocument().addDocumentListener(new LazyDocumentListener() {
			
			@Override
			public void update(DocumentEvent e) {
				List<HistoryFilter> filterList = new ArrayList<HistoryFilter>();
				
				// filter by all words
				for (String word : filterEditor.getText().split("\\s+")) {
					filterList.add(new HistoryFilter(word));
				}
				
				// use filter on both tables
				for (JTable table : Arrays.asList(sequenceTable, elementTable)) {
					TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
					sorter.setRowFilter(RowFilter.andFilter(filterList));
				}
				
				if (sequenceTable.getSelectedRow() < 0 && sequenceTable.getRowCount() > 0) {
					// selection lost, maybe due to filtering, auto-select next row
					sequenceTable.getSelectionModel().addSelectionInterval(0, 0);
				}
			}
		});
		
		// install context menu
		sequenceTable.addMouseListener(contextMenuProvider);
		elementTable.addMouseListener(contextMenuProvider);
		
		// initialize window properties
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationByPlatform(true);
		setResizable(true);
		setSize(580, 640);
	}
	

	public void setModel(History history) {
		// update table model
		sequenceModel.setData(history.sequences());
		
		if (sequenceTable.getRowCount() > 0) {
			// auto-select first element and update element table
			sequenceTable.getSelectionModel().addSelectionInterval(0, 0);
		} else {
			// clear element table
			elementModel.setData(new ArrayList<Element>(0));
		}
		
		// display basic statistics
		initializeInfoLabel();
	}
	

	public History getModel() {
		return new History(sequenceModel.getData());
	}
	

	public JLabel getInfoLabel() {
		return infoLabel;
	}
	

	private void initializeInfoLabel() {
		int count = 0;
		Date since = new Date();
		
		for (Sequence sequence : sequenceModel.getData()) {
			count += sequence.elements().size();
			
			if (sequence.date().before(since))
				since = sequence.date();
		}
		
		infoLabel.setText(String.format("A total of %,d files have been renamed since %s.", count, DateFormat.getDateInstance().format(since)));
	}
	

	private JScrollPane createScrollPaneGroup(String title, JComponent component) {
		JScrollPane scrollPane = new JScrollPane(component);
		scrollPane.setBorder(new CompoundBorder(new TitledBorder(title), scrollPane.getBorder()));
		
		return scrollPane;
	}
	

	private JTable createTable(TableModel model) {
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);
		
		// hide grid
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		
		// decrease column width for the row number columns
		DefaultTableColumnModel m = ((DefaultTableColumnModel) table.getColumnModel());
		m.getColumn(0).setMaxWidth(50);
		
		return table;
	}
	
	private final Action closeAction = new AbstractAction("Close") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
			dispose();
		}
	};
	
	private final Action clearFilterAction = new AbstractAction(null, ResourceManager.getIcon("edit.clear")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			filterEditor.setText("");
		}
	};
	
	private final MouseListener contextMenuProvider = new MouseAdapter() {
		
		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}
		

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
		

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JTable table = (JTable) e.getSource();
				
				int clickedRow = table.rowAtPoint(e.getPoint());
				
				if (clickedRow < 0) {
					// no row was clicked
					return;
				}
				
				if (!table.getSelectionModel().isSelectedIndex(clickedRow)) {
					// if clicked row is not selected, set selection to this row (and deselect all other currently selected row)
					table.getSelectionModel().setSelectionInterval(clickedRow, clickedRow);
				}
				
				List<Element> selection = new ArrayList<Element>();
				
				for (int i : table.getSelectedRows()) {
					int index = table.convertRowIndexToModel(i);
					
					if (sequenceModel == table.getModel()) {
						selection.addAll(sequenceModel.getRow(index).elements());
					} else if (elementModel == table.getModel()) {
						selection.add(elementModel.getRow(index));
					}
				}
				
				if (selection.size() > 0) {
					JPopupMenu menu = new JPopupMenu();
					menu.add(new RevertAction(selection, HistoryDialog.this));
					
					// display popup
					menu.show(table, e.getX(), e.getY());
				}
			}
		}
	};
	
	
	private static class RevertAction extends AbstractAction {
		
		public static final String ELEMENTS = "elements";
		public static final String PARENT = "parent";
		
		
		public RevertAction(Collection<Element> elements, HistoryDialog parent) {
			putValue(NAME, "Revert...");
			putValue(ELEMENTS, elements.toArray(new Element[0]));
			putValue(PARENT, parent);
		}
		

		public Element[] elements() {
			return (Element[]) getValue(ELEMENTS);
		}
		

		public HistoryDialog parent() {
			return (HistoryDialog) getValue(PARENT);
		}
		
		
		private enum Option {
			Rename {
				@Override
				public String toString() {
					return "Rename";
				}
			},
			ChangeDirectory {
				@Override
				public String toString() {
					return "Change Directory";
				}
			},
			Cancel {
				@Override
				public String toString() {
					return "Cancel";
				}
			}
		}
		
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// use default directory
			File directory = null;
			
			Option selectedOption = Option.ChangeDirectory;
			
			// change directory option
			while (selectedOption == Option.ChangeDirectory) {
				List<File> missingFiles = getMissingFiles(directory);
				
				Object message;
				int type;
				Set<Option> options;
				
				if (missingFiles.isEmpty()) {
					message = String.format("Are you sure you want to rename %d file(s)?", elements().length);
					type = QUESTION_MESSAGE;
					options = EnumSet.of(Option.Rename, Option.ChangeDirectory, Option.Cancel);
				} else {
					String text = String.format("Some files are missing. Please select a different directory.");
					JList missingFilesComponent = new JList(missingFiles.toArray()) {
						@Override
						public Dimension getPreferredScrollableViewportSize() {
							// adjust component size
							return new Dimension(80, 80);
						}
					};
					
					missingFilesComponent.setCellRenderer(new DefaultListCellRenderer() {
						@Override
						public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
							return super.getListCellRendererComponent(list, ((File) value).getName(), index, isSelected, false);
						}
					});
					
					message = new Object[] { text, new JScrollPane(missingFilesComponent) };
					type = PLAIN_MESSAGE;
					options = EnumSet.of(Option.ChangeDirectory, Option.Cancel);
				}
				
				JOptionPane pane = new JOptionPane(message, type, YES_NO_CANCEL_OPTION, null, options.toArray(), Option.Cancel);
				
				// display option dialog
				pane.createDialog(parent(), "Revert").setVisible(true);
				
				// update selected option
				selectedOption = (Option) pane.getValue();
				
				// change directory option
				if (selectedOption == Option.ChangeDirectory) {
					JFileChooser chooser = new JFileChooser(directory);
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					chooser.setMultiSelectionEnabled(false);
					
					if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						directory = chooser.getSelectedFile();
					}
				}
			}
			
			// rename files
			if (selectedOption == Option.Rename) {
				rename(directory);
			}
		}
		

		private void rename(File directory) {
			int count = 0;
			
			for (Entry<File, File> entry : getRenameMap(directory).entrySet()) {
				if (entry.getKey().renameTo(entry.getValue())) {
					count++;
				}
			}
			
			JLabel status = parent().getInfoLabel();
			
			if (count == elements().length) {
				status.setText(String.format("%d file(s) have been renamed.", count));
				status.setIcon(ResourceManager.getIcon("status.ok"));
			} else {
				status.setText(String.format("Failed to revert %d file(s).", elements().length - count, elements().length));
				status.setIcon(ResourceManager.getIcon("status.error"));
			}
			
			// update view
			parent().repaint();
		}
		

		private Map<File, File> getRenameMap(File directory) {
			Map<File, File> renameMap = new LinkedHashMap<File, File>();
			
			for (Element element : elements()) {
				// use given directory or default directory
				File dir = directory != null ? directory : element.dir();
				
				// reverse
				File from = new File(dir, element.to());
				File to = new File(dir, element.from());
				
				renameMap.put(from, to);
			}
			
			return renameMap;
		}
		

		private List<File> getMissingFiles(File directory) {
			List<File> missingFiles = new ArrayList<File>();
			
			for (File file : getRenameMap(directory).keySet()) {
				if (!file.exists())
					missingFiles.add(file);
			}
			
			return missingFiles;
		}
	}
	
	private final FileTransferablePolicy importHandler = new FileTransferablePolicy() {
		
		@Override
		protected boolean accept(List<File> files) {
			return FileUtilities.containsOnly(files, new ExtensionFileFilter("xml"));
		}
		

		@Override
		protected void clear() {
			setModel(new History());
		}
		

		@Override
		protected void load(List<File> files) throws IOException {
			History history = getModel();
			
			try {
				for (File file : files) {
					history.merge(History.importHistory(file));
				}
			} finally {
				// update view
				setModel(history);
			}
		}
		

		@Override
		public String getFileFilterDescription() {
			return "history files (.xml)";
		}
	};
	
	private final FileExportHandler exportHandler = new FileExportHandler() {
		
		@Override
		public boolean canExport() {
			// allow export of empty history
			return true;
		}
		

		@Override
		public void export(File file) throws IOException {
			History.exportHistory(getModel(), file);
		}
		

		@Override
		public String getDefaultFileName() {
			return "history.xml";
		}
	};
	
	
	private static class HistoryFilter extends RowFilter<Object, Integer> {
		
		private final String filter;
		
		
		public HistoryFilter(String filter) {
			this.filter = filter.toLowerCase();
		}
		

		@Override
		public boolean include(Entry<?, ? extends Integer> entry) {
			// sequence model
			if (entry.getModel() instanceof SequenceTableModel) {
				SequenceTableModel model = (SequenceTableModel) entry.getModel();
				
				for (Element element : model.getRow(entry.getIdentifier()).elements()) {
					if (include(element))
						return true;
				}
				
				return false;
			}
			
			// element model
			if (entry.getModel() instanceof ElementTableModel) {
				ElementTableModel model = (ElementTableModel) entry.getModel();
				
				return include(model.getRow(entry.getIdentifier()));
			}
			
			// will not happen
			throw new IllegalArgumentException("Illegal model: " + entry.getModel());
		}
		

		private boolean include(Element element) {
			return include(element.to()) || include(element.from()) || include(element.dir().getPath());
		}
		

		private boolean include(String value) {
			return value.toLowerCase().contains(filter);
		}
	}
	

	private static class SequenceTableModel extends AbstractTableModel {
		
		private List<Sequence> data = emptyList();
		
		
		public void setData(List<Sequence> data) {
			this.data = new ArrayList<Sequence>(data);
			
			// update view
			fireTableDataChanged();
		}
		

		public List<Sequence> getData() {
			return unmodifiableList(data);
		}
		

		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "#";
				case 1:
					return "Name";
				case 2:
					return "Count";
				case 3:
					return "Date";
				default:
					return null;
			}
		}
		

		@Override
		public int getColumnCount() {
			return 4;
		}
		

		@Override
		public int getRowCount() {
			return data.size();
		}
		

		@Override
		public Class<?> getColumnClass(int column) {
			switch (column) {
				case 0:
					return Integer.class;
				case 1:
					return String.class;
				case 2:
					return Integer.class;
				case 3:
					return Date.class;
				default:
					return null;
			}
		}
		

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
				case 0:
					return row + 1;
				case 1:
					return getName(data.get(row));
				case 2:
					return data.get(row).elements().size();
				case 3:
					return data.get(row).date();
				default:
					return null;
			}
		}
		

		public Sequence getRow(int row) {
			return data.get(row);
		}
		

		private String getName(Sequence sequence) {
			StringBuilder sb = new StringBuilder();
			
			for (Element element : sequence.elements()) {
				String name = element.dir().getName();
				
				// append name only, if it isn't listed already
				if (sb.indexOf(name) < 0) {
					if (sb.length() > 0)
						sb.append(", ");
					
					sb.append(name);
				}
			}
			
			return sb.toString();
		}
	}
	

	private static class ElementTableModel extends AbstractTableModel {
		
		private List<Element> data = emptyList();
		
		
		public void setData(List<Element> data) {
			this.data = new ArrayList<Element>(data);
			
			// update view
			fireTableDataChanged();
		}
		

		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "#";
				case 1:
					return "New Name";
				case 2:
					return "Original Name";
				case 3:
					return "Directory";
				default:
					return null;
			}
		}
		

		@Override
		public int getColumnCount() {
			return 4;
		}
		

		@Override
		public int getRowCount() {
			return data.size();
		}
		

		@Override
		public Class<?> getColumnClass(int column) {
			switch (column) {
				case 0:
					return Integer.class;
				case 1:
					return String.class;
				case 2:
					return String.class;
				case 3:
					return File.class;
				default:
					return null;
			}
		}
		

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
				case 0:
					return row + 1;
				case 1:
					return data.get(row).to();
				case 2:
					return data.get(row).from();
				case 3:
					return data.get(row).dir();
				default:
					return null;
			}
		}
		

		public Element getRow(int row) {
			return data.get(row);
		}
		

		public boolean isBroken(int row) {
			Element element = data.get(row);
			File file = new File(element.dir(), element.to());
			
			return !file.exists();
		}
	}
	
}
