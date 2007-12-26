
package net.sourceforge.filebot.ui.panel.sfv;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.sourceforge.filebot.ui.FileFormat;
import net.sourceforge.filebot.ui.panel.sfv.renderer.ChecksumTableCellRenderer;
import net.sourceforge.filebot.ui.panel.sfv.renderer.StateIconTableCellRenderer;
import net.sourceforge.filebot.ui.panel.sfv.renderer.TextTableCellRenderer;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.filebot.ui.transfer.ExportHandler;
import net.sourceforge.filebot.ui.transfer.ImportHandler;
import net.sourceforge.filebot.ui.transfer.Saveable;
import net.sourceforge.filebot.ui.transfer.SaveableExportHandler;
import net.sourceforge.filebot.ui.transfer.TransferablePolicyImportHandler;
import net.sourceforge.filebot.ui.transferablepolicies.NullTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicySupport;


public class SfvTable extends JTable implements TransferablePolicySupport, Saveable {
	
	private TransferablePolicy transferablePolicy = new NullTransferablePolicy();
	
	
	public SfvTable() {
		final SfvTableModel model = (SfvTableModel) getModel();
		model.addPropertyChangeListener(repaintListener);
		
		setFillsViewportHeight(true);
		
		transferablePolicy = new SfvTransferablePolicy(model);
		
		setModel(model);
		setAutoCreateRowSorter(true);
		setAutoCreateColumnsFromModel(true);
		setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		setRowHeight(20);
		
		ImportHandler importHandler = new TransferablePolicyImportHandler(this);
		ExportHandler exportHandler = new SaveableExportHandler(this);
		
		setTransferHandler(new DefaultTransferHandler(importHandler, exportHandler));
		setDragEnabled(true);
		
		setDefaultRenderer(ChecksumRow.State.class, new StateIconTableCellRenderer());
		setDefaultRenderer(String.class, new TextTableCellRenderer());
		setDefaultRenderer(Checksum.class, new ChecksumTableCellRenderer());
	}
	

	@Override
	protected TableModel createDefaultDataModel() {
		return new SfvTableModel();
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
	

	public void clear() {
		((SfvTableModel) getModel()).clear();
	}
	
	private PropertyChangeListener repaintListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			repaint();
		}
	};
	
	
	public TransferablePolicy getTransferablePolicy() {
		return transferablePolicy;
	}
	

	public void setTransferablePolicy(TransferablePolicy transferablePolicy) {
		this.transferablePolicy = transferablePolicy;
	}
	

	public String getDefaultFileName() {
		SfvTableModel model = (SfvTableModel) getModel();
		File columnRoot = model.getChecksumColumnRoot(0);
		
		String name = "";
		
		if (columnRoot != null)
			name = FileFormat.getNameWithoutSuffix(columnRoot);
		
		if (name.isEmpty())
			name = "name";
		
		return name + ".sfv";
	}
	

	public boolean isSaveable() {
		return getModel().getRowCount() > 0;
	}
	

	public void removeRows(int... rowIndices) {
		SfvTableModel model = (SfvTableModel) getModel();
		model.removeRows(rowIndices);
	}
	

	public void save(File file, int checksumColumnIndex) {
		try {
			PrintStream out = new PrintStream(file);
			
			SfvTableModel model = (SfvTableModel) getModel();
			File columnRoot = model.getChecksumColumnRoot(checksumColumnIndex);
			
			if (columnRoot != null) {
				SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
				
				Date now = new Date();
				out.println("; Generated by FileBot on " + date.format(now) + " at " + time.format(now));
				out.println(";");
				out.println(";");
				
				Map<String, Checksum> checksumMap = model.getChecksumColumn(columnRoot);
				
				for (String name : checksumMap.keySet()) {
					out.println(name + " " + checksumMap.get(name).getChecksumString());
				}
			}
			
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	

	public void save(File file) {
		save(file, 0);
	}
	
}
