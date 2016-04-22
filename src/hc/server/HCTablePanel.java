package hc.server;

import hc.App;
import hc.core.IWatcher;
import hc.core.util.ExceptionReporter;

import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

public class HCTablePanel {
	final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	public final JTable table;
	int size;
	final Vector<Object[]> body;
	boolean isFirstColLineNo = false;
	final int colNumber;
	private final JButton upBut, editBut, downBut, removeBut;
	public Vector<Object[]> getTabelData(){
		return body;
	}
	
	/**
	 * 获取指定列的全部对象
	 * @param idx
	 * @return
	 */
	public Object[] getColumnAt(final int idx){
		final Object[] out = new Object[size];
		
		for (int i = 0; i < out.length; i++) {
			out[i] = body.elementAt(i)[idx];
		}
		
		return out;
	}
	
	public int getRowNumber(){
		return size;
	}
	
	/**
	 * 
	 * @param tableModel
	 * @param libs
	 * @param colName
	 * @param initRows
	 * @param upBut
	 * @param downBut
	 * @param removeBut
	 * @param importBut
	 * @param editBut
	 * @param upOrDownMovingBiz
	 * @param removeBiz
	 * @param importBiz
	 * @param isFirstColLineIdx
	 * @param colNumber 列数。注意：有可能本值<>colName.length()。因为数据区可能使用复合对象
	 */
	public HCTablePanel(final AbstractTableModel tableModel, final Vector<Object[]> libs, 
			final Object[] colName, final int initRows, 
			final JButton upBut, final JButton downBut, 
			final JButton removeBut, final JButton importBut, final JButton editBut,
			final AbstractDelayBiz upOrDownMovingBiz,
			final AbstractDelayBiz removeBiz, final AbstractDelayBiz importBiz, 
			final boolean isFirstColLineIdx, final int colNumber){
//		{
//			System.out.println("print table value");
//			final int size = tableModel.getRowCount();
//			for (int i = 0; i < size; i++) {
//				final int len = tableModel.getColumnCount();
//				for (int j = 0; j < len; j++) {
//					System.out.print(tableModel.getValueAt(i, j));
//					System.out.print("------");
//				}
//				System.out.print("\n");
//			}
//		}
		
		body = libs;
		this.isFirstColLineNo = isFirstColLineIdx;
		this.upBut = upBut;
		this.downBut = downBut;
		this.removeBut = removeBut;
		this.editBut = editBut;
		
		final HCTablePanel self = this;
		size = initRows;
		this.colNumber = colNumber;
		
		table = new JTable(tableModel);
		
		
		final IWatcher checkUpOrDown = new IWatcher() {
			@Override
			public boolean watch() {
				if(upOrDownMovingBiz != null){
					upOrDownMovingBiz.setPara(self);
					upOrDownMovingBiz.doBiz();
				}
				return false;
			}
			@Override
			public void setPara(final Object p) {
			}
			@Override
			public boolean isCancelable() {
				return false;
			}
			@Override
			public void cancel() {
			}
		};
		upBut.addActionListener(new HCButtonEnabledActionListener(upBut, new Runnable() {
			@Override
			public void run() {
				final int currRow = table.getSelectedRow();
				final int toIdx = currRow - 1;
				swapRow(currRow, toIdx);
				
				App.invokeLaterUI(new Runnable() {
					@Override
					public void run() {
						table.setRowSelectionInterval(toIdx, toIdx);
						table.updateUI();
					}
				});
			}
		}, threadPoolToken, checkUpOrDown));
		
		downBut.addActionListener(new HCButtonEnabledActionListener(downBut, new Runnable() {
			@Override
			public void run() {
				final int currRow = table.getSelectedRow();
				final int toIdx = currRow + 1;
				swapRow(currRow, toIdx);
				
				App.invokeLaterUI(new Runnable() {
					@Override
					public void run() {
						table.setRowSelectionInterval(toIdx, toIdx);
						table.updateUI();
					}
				});
			}
		}, threadPoolToken, checkUpOrDown));
		
		final IWatcher noDoneWatcher = new IWatcher() {
			@Override
			public boolean watch() {
				refreshButton();
				return false;
			}
			@Override
			public void setPara(final Object p) {
			}
			@Override
			public boolean isCancelable() {
				return false;
			}
			@Override
			public void cancel() {
			}
		};
		removeBut.addActionListener(new HCButtonEnabledActionListener(removeBut, new Runnable() {
			int currRow;
			@Override
			public void run() {
				currRow = table.getSelectedRow();
				
				final Object[] rowValue = new Object[colNumber];
				for (int i = 0; i < colNumber; i++) {
					rowValue[i] = body.elementAt(currRow)[i];
				}
				
				while(removeBiz != null){
					removeBiz.setPara(rowValue);
					removeBiz.doBiz();
					boolean isBreak = false;
					
					while(true){
						final Object back = removeBiz.getPara();
						if(back instanceof boolean[]){
							final boolean[] bb = (boolean[])back;
							if(bb[0]){
								isBreak = true;
							}
							break;
						}
						try{
							Thread.sleep(200);
						}catch (final Exception e) {
						}
					}
					if(isBreak){
						break;
					}
					return;
				}
				
				for (int i = currRow; i < size - 1; i++) {
					final int nextRowNum = i + 1;
					for (int col = 0; col < colNumber; col++) {
						if(nextRowNum != size){
							if(isFirstColLineNo && (col == 0)){
								body.elementAt(i)[col] = i + 1;
							}else{
								body.elementAt(i)[col] = body.elementAt(nextRowNum)[col];
							}
						}
					}
				}
				body.remove(size - 1);
				
				size--;
				
				App.invokeLaterUI(new Runnable() {
					@Override
					public void run() {
						table.clearSelection();
						table.updateUI();
						try{
							if(currRow < size){
								table.setRowSelectionInterval(currRow, currRow);
							}else{
								if(size == 0){
								}else{
									table.setRowSelectionInterval(size - 1, size - 1);
								}
							}
						}catch (final Exception e) {
							ExceptionReporter.printStackTrace(e);
						}
						
						refreshButton();
					}
				});
			}
		}, threadPoolToken, noDoneWatcher));
		
		importBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				if(importBiz != null){
					//由于添加har工程，所以另开线程
					new Thread(){
						@Override
						public void run(){
							importBiz.doBiz();
							
							final Object[] row = (Object[])importBiz.getPara();
							if(row != null){
								final Object[] newRow = new Object[colNumber];
								for (int i = 0; i < colNumber; i++) {
									if(isFirstColLineNo && (i == 0)){
										newRow[0] = size + 1;
									}else{
										newRow[i] = row[i];
									}
								}
								body.insertElementAt(newRow, size);
								size++;
								
								App.invokeLaterUI(new Runnable() {
									@Override
									public void run() {
										final int idx = size - 1;
										table.setRowSelectionInterval(idx, idx);
										refreshButton();
										table.updateUI();
									}
								});
							}
						}
					}.start();
				}
			}
		}, threadPoolToken));
		
		final ListSelectionModel rowSM = table.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				refreshButton();
			}
		});
		if(initRows > 0){
			table.setRowSelectionInterval(0, 0);
		}else{
			refreshButton();
		}
	}
	
	private void swapRow(final int fromIdx, final int toIdx){
		for (int col = (isFirstColLineNo?1:0); col < colNumber; col++) {
			final Object v1 = body.elementAt(toIdx)[col];
			body.elementAt(toIdx)[col] = body.elementAt(fromIdx)[col];
			body.elementAt(fromIdx)[col] = v1;
		}
	}

	public void refresh(final int newSize){
		size = newSize;
		if(newSize > 0){
			table.setRowSelectionInterval(0, 0);
		}
		table.updateUI();
		
		refreshButton();
	}
	
	private void refreshButton() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final int selectedRow = table.getSelectedRow();
				
				if(selectedRow == -1){
					downBut.setEnabled(false);
					upBut.setEnabled(false);
					removeBut.setEnabled(false);
					if(editBut != null){
						editBut.setEnabled(false);
					}
				}else{
					removeBut.setEnabled(true);
					if(editBut != null){
						editBut.setEnabled(true);
					}
					if(selectedRow < (size - 1)){
						downBut.setEnabled(true);
					}else{
						downBut.setEnabled(false);
					}
					
					if(selectedRow == 0){
						upBut.setEnabled(false);
					}else{
						upBut.setEnabled(true);
					}
				}
			}
		});
	}
}
