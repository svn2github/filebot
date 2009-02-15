
package net.sourceforge.filebot.ui.panel.sfv;


import static net.sourceforge.filebot.FileBotUtilities.EMBEDDED_CHECKSUM_PATTERN;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.sourceforge.tuned.ui.TunedUtilities.DragDropRowTableUI;


class ChecksumTable extends JTable {
	
	public ChecksumTable() {
		setFillsViewportHeight(true);
		setAutoCreateRowSorter(true);
		setAutoCreateColumnsFromModel(true);
		setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		setRowHeight(20);
		
		setDragEnabled(true);
		setUI(new DragDropRowTableUI());
		
		// highlight CRC32 patterns in filenames in green and with smaller font-size
		setDefaultRenderer(String.class, new HighlightPatternCellRenderer(EMBEDDED_CHECKSUM_PATTERN));
		setDefaultRenderer(ChecksumRow.State.class, new StateIconCellRenderer());
		setDefaultRenderer(ChecksumCell.class, new ChecksumCellRenderer());
	}
	

	@Override
	protected TableModel createDefaultDataModel() {
		return new ChecksumTableModel();
	}
	

	@Override
	public ChecksumTableModel getModel() {
		return (ChecksumTableModel) super.getModel();
	}
	

	@Override
	public void createDefaultColumnsFromModel() {
		super.createDefaultColumnsFromModel();
		
		for (int i = 0; i < getColumnCount(); i++) {
			TableColumn column = getColumnModel().getColumn(i);
			
			if (i == 0) {
				column.setPreferredWidth(45);
			} else if (i == 1) {
				column.setPreferredWidth(400);
			} else if (i >= 2) {
				column.setPreferredWidth(150);
			}
		}
	}
	
}
