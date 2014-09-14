package hc.server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

public class HCTablePanel {
	public final JTable table;
	int size;
	final Object[][] body;
	boolean isFirstColLineNo = false;
	final Object[] defaultRow;
	final int colNumber;
	private final JButton upBut, editBut, downBut, removeBut;
	public Object[][] getTabelData(){
		return body;
	}
	
	/**
	 * 获取指定列的全部对象
	 * @param idx
	 * @return
	 */
	public Object[] getColumnAt(int idx){
		Object[] out = new Object[size];
		
		for (int i = 0; i < out.length; i++) {
			out[i] = body[i][idx];
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
	 * @param dr 当增加一行时，需要赋予的初始值
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
	 */
	public HCTablePanel(final AbstractTableModel tableModel, final Object[][] libs, 
			final Object[] colName, final Object[] dr, final int initRows, 
			final JButton upBut, final JButton downBut, 
			final JButton removeBut, final JButton importBut, final JButton editBut,
			final AbstractDelayBiz upOrDownMovingBiz,
			final AbstractDelayBiz removeBiz, final AbstractDelayBiz importBiz, 
			final boolean isFirstColLineIdx){
		body = libs;
		this.isFirstColLineNo = isFirstColLineIdx;
		this.upBut = upBut;
		this.downBut = downBut;
		this.removeBut = removeBut;
		this.editBut = editBut;
		
		this.defaultRow = dr;
		final HCTablePanel self = this;
		size = initRows;
		colNumber = body[0].length;
		
		table = new JTable(tableModel);
		
		
		upBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int currRow = table.getSelectedRow();
				final int toIdx = currRow - 1;
				swapRow(currRow, toIdx);
				
				table.setRowSelectionInterval(toIdx, toIdx);
				table.updateUI();
				
				if(upOrDownMovingBiz != null){
					upOrDownMovingBiz.setPara(self);
					upOrDownMovingBiz.doBiz();
				}
			}
		});
		
		downBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int currRow = table.getSelectedRow();
				final int toIdx = currRow + 1;
				swapRow(currRow, toIdx);
				
				table.setRowSelectionInterval(toIdx, toIdx);
				table.updateUI();
				
				if(upOrDownMovingBiz != null){
					upOrDownMovingBiz.setPara(self);
					upOrDownMovingBiz.doBiz();
				}
			}
		});
		
		removeBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int currRow = table.getSelectedRow();
				
				Object[] rowValue = new Object[colNumber];
				for (int i = 0; i < colNumber; i++) {
					rowValue[i] = body[currRow][i];
				}
				
				while(removeBiz != null){
					removeBiz.setPara(rowValue);
					removeBiz.doBiz();
					Object back = removeBiz.getPara();
					if(back instanceof boolean[]){
						boolean[] bb = (boolean[])back;
						if(bb[0]){
							break;
						}
					}
					return;
				}
				
				for (int i = currRow; i < size; i++) {
					for (int l = 0; l < colNumber; l++) {
						Object v1;
						final int nextRowNum = i + 1;
						if(nextRowNum != size){
							v1 = body[nextRowNum][l];
						}else{
							v1 = defaultRow[l];
						}
						body[i][l] = v1;
					}
				}
				
				size--;
				table.clearSelection();
				try{
					if(currRow < size){
						table.setRowSelectionInterval(currRow, currRow);
					}else{
						if(size == 0){
							table.setRowSelectionInterval(0, 0);
						}else{
							table.setRowSelectionInterval(size - 1, size - 1);
						}
					}
				}catch (Exception ex) {
				}
				table.updateUI();
				
				refreshButton();
			}
		});
		
		importBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(importBiz != null){
					//由于添加har工程，所以另开线程
					new Thread(){
						public void run(){
							importBiz.doBiz();
							
							Object[] row = (Object[])importBiz.getPara();
							if(row != null){
								for (int i = 0; i < colNumber; i++) {
									if(isFirstColLineNo && (i == 0)){
										body[size][0] = size + 1;
									}else{
										body[size][i] = row[i];
									}
								}
								size++;
								int idx = table.getSelectedRow();
								table.setRowSelectionInterval(idx, idx);
								refreshButton();
								table.updateUI();
							}
						}
					}.start();
				}
			}
		});
		
		ListSelectionModel rowSM = table.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				refreshButton();
			}
		});

		table.setRowSelectionInterval(0, 0);
		
	}
	
	private void swapRow(final int fromIdx, final int toIdx){
		for (int i = (isFirstColLineNo?1:0); i < colNumber; i++) {
			Object v1 = body[toIdx][i];
			body[toIdx][i] = body[fromIdx][i];
			body[fromIdx][i] = v1;
		}
	}

	public void refresh(int newSize){
		size = newSize;
		table.setRowSelectionInterval(0, 0);
		table.updateUI();
		
		refreshButton();
	}
	
	private void refreshButton() {
		int selectedRow = table.getSelectedRow();
		int editRowNum = selectedRow + 1;
		
		if(editRowNum > size){
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
			if(editRowNum == size){
				downBut.setEnabled(false);
			}else{
				downBut.setEnabled(true);
			}
			
			if(editRowNum == 1){
				upBut.setEnabled(false);
			}else{
				upBut.setEnabled(true);
			}
		}
	}
}
