package hc.server.ui.video;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.JComboBox;
import javax.swing.JPanel;

class CapSizeControl extends JPanel implements ItemListener,
		ComponentListener {
	private JComboBox comboSize;
	private HashMap<String, CapSize> htSizes;
	private CapSize sizeVideoDefault;
	private ActionListener listener;
	static final String CUSTOM_STRING = "<custom>";

	public CapSizeControl() {
		this(null);
	}

	public CapSizeControl(CapSize sizeVideoDefault) {
		this.htSizes = new HashMap<String, CapSize>();
		this.sizeVideoDefault = null;

		this.sizeVideoDefault = sizeVideoDefault;
		init();
	}

	public void setEnabled(boolean boolEnable) {
		super.setEnabled(boolEnable);

		this.comboSize.setEnabled(boolEnable);

		if (boolEnable == true)
			updateFields();
	}

	public void addActionListener(ActionListener listener) {
		this.listener = listener;
	}

	public CapSize getVideoSize() {
		CapSize sizeVideo;
		String strItem = (String) this.comboSize.getSelectedItem();
		Object objSize = this.htSizes.get(strItem);
		if ((objSize == null) || (!(objSize instanceof CapSize))
				|| (strItem.equals(CUSTOM_STRING))) {
			sizeVideo = new CapSize(0, 0);
		} else {
			sizeVideo = (CapSize) objSize;
		}
		return sizeVideo;
	}

	public void addItem(CapSize sizeVideo) {
		String strItem;
		if (sizeVideo == null) {
			sizeVideo = new CapSize(-1, -1);
			strItem = CUSTOM_STRING;
		} else {
			strItem = sizeVideo.toString();
		}

		if (this.htSizes.containsKey(strItem))
			return;

		this.comboSize.addItem(strItem);
		this.htSizes.put(strItem, sizeVideo);

		if (this.comboSize.getItemCount() == 1)
			updateFields();
	}

	public void removeAll() {
		this.comboSize.removeAllItems();
		this.htSizes.clear();
		updateFields();
	}

	public void select(CapSize sizeVideo) {
		if (sizeVideo == null)
			this.comboSize.setSelectedItem(CUSTOM_STRING);
		else
			this.comboSize.setSelectedItem(sizeVideo.toString());
		updateFields();
	}

	public void select(int nIndex) {
		this.comboSize.setSelectedIndex(nIndex);
		updateFields();
	}

	public int getItemCount() {
		return this.comboSize.getItemCount();
	}

	private void init() {
		setLayout(new GridLayout(0, 1, 4, 4));

		this.comboSize = new JComboBox();
		this.comboSize.addItem(CUSTOM_STRING);
		this.comboSize.addItemListener(this);
		add(this.comboSize);

		updateFields();
	}

	private void updateFields() {
		boolean boolEnable;
		String strItem = (String) this.comboSize.getSelectedItem();
		if ((strItem == null) || (strItem.equals(CUSTOM_STRING))) {
			boolEnable = true;
		} else {
			CapSize sizeVideo = (CapSize) this.htSizes.get(strItem);
			boolEnable = false;
		}
	}

	public void itemStateChanged(ItemEvent event) {
		Object objectSource = event.getSource();
		if (objectSource != this.comboSize)
			return;
		updateFields();
		if (this.listener != null) {
			ActionEvent eventAction = new ActionEvent(this, 1001,
					"Size Changed");
			this.listener.actionPerformed(eventAction);
		}
	}


	public void componentMoved(ComponentEvent event) {
	}

	public void componentShown(ComponentEvent event) {
	}

	public void componentHidden(ComponentEvent event) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}
}