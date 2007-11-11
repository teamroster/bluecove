/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
 * 
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */ 
package net.sf.bluecove.awt;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Vector;

import javax.bluetooth.UUID;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.Logger;
import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.BooleanVar;
import net.sf.bluecove.util.IntVar;
import net.sf.bluecove.util.StringVar;

/**
 * @author vlads
 *
 */
public class ConfigurationDialog extends OkCancelDialog {

	private static final long serialVersionUID = 1L;
	
	private Panel panelItems;
	
	private Vector configItems = new Vector(); 
	
	private Button btnPagePrev, btnPageNext; 
	
	private int page = 0;
	
	private class ConfigurationComponent {
		
		String name;
		
		String guiName;
		
		Component guiComponent;
		
		Field configField;
		
	}

	
	public ConfigurationDialog(Frame owner) {
		super(owner, "Configuration", true);
		
		panelBtns.add(btnPagePrev = new Button("<<"));
		btnPagePrev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onPage(-1);
			}
		});
		
		panelBtns.add(btnPageNext = new Button(">>"));
		btnPageNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onPage(1);
			}
		});
		
		if (Configuration.screenSizeSmall) {
			this.setFont(new Font("Default", Font.PLAIN, 9));
		}
//		if (Configuration.screenSizeSmall) {
//			Scrollbar slider =new Scrollbar(Scrollbar.VERTICAL, 0, 1, 0, 255);
//			add(slider, BorderLayout.EAST);
//			 
//			//ScrollPane scrollPane = new ScrollPane();
//			//this.add(scrollPane, BorderLayout.NORTH);
//			//scrollPane.add(panelItems, BorderLayout.NORTH);
//		}
//		 
		panelItems = new BorderPanel();
		this.add(panelItems, BorderLayout.NORTH);
		
		addConfig("deviceClassFilter");
		addConfig("discoverDevicesComputers");
		addConfig("discoverDevicesPhones");
		addConfig("discoveryUUID");
		
		addConfig("useShortUUID");
	
		addConfig("testRFCOMM");
		addConfig("TEST_CASE_FIRST");
		addConfig("TEST_CASE_LAST");
		addConfig("STERSS_TEST_CASE");
		addConfig("testL2CAP");
		addConfig("TEST_CASE_L2CAP_FIRST");
		addConfig("TEST_CASE_L2CAP_LAST");
		addConfig("testServerOBEX_TCP");
		addConfig("authenticateOBEX");
		
		addConfig(null);
		
		addConfig("authenticate");
		addConfig("encrypt");
		addConfig("authorize");
		
		addConfig("clientSleepBetweenConnections");
		addConfig("clientTestTimeOutSec");
		addConfig("serverSleepB4ClosingConnection");
		addConfig("testServiceAttributes");
		addConfig("testIgnoreNotWorkingServiceAttributes", "ignoreNotWorkingServAttr");
		addConfig("testAllServiceAttributes");

		addConfig("clientContinuous");
		addConfig("clientContinuousDiscovery");
		addConfig("clientContinuousDiscoveryDevices");
		addConfig("clientContinuousServicesSearch");
		addConfig("clientTestConnections");
		
		panelItems.setLayout(new GridLayout(configItems.size(), 2));
		
		buildUIItems();
		updateGUI();

		this.pack();
		OkCancelDialog.centerParent(this);
	}
	
	protected void onClose(boolean isCancel) {
		if (!isCancel) {
			updateConfig();
		}
		setVisible(false);
	}
	
	private void onPage(int i) {
		page += i;
		updateConfig();
		buildUIItems();
		updateGUI();
	}
	
	private void updateGUI() {
		for(Enumeration en = configItems.elements(); en.hasMoreElements(); ) {
			ConfigurationComponent cc = (ConfigurationComponent)en.nextElement();
			if (cc.guiComponent == null) {
				continue;
			}
			Class type = cc.configField.getType();
			try {
				if (type.equals(boolean.class)) {
					Checkbox c = (Checkbox) cc.guiComponent;
					c.setState(cc.configField.getBoolean(Configuration.class));
				} else if (type.equals(BooleanVar.class)) {
					Checkbox c = (Checkbox) cc.guiComponent;
					c.setState(((BooleanVar)cc.configField.get(Configuration.class)).booleanValue());
				} else if (type.equals(UUID.class)) {
					TextField tf = (TextField) cc.guiComponent;
					tf.setText(cc.configField.get(Configuration.class).toString());
				} else if ((type.equals(String.class)) || (type.equals(StringVar.class))) {
					TextField tf = (TextField) cc.guiComponent;
					tf.setText(cc.configField.get(Configuration.class).toString());
				} else if (type.equals(int.class)) {
					TextField tf = (TextField) cc.guiComponent;
					tf.setText(String.valueOf(cc.configField.getInt(Configuration.class)));
				} else if (type.equals(IntVar.class)) {
					TextField tf = (TextField) cc.guiComponent;
					tf.setText(cc.configField.get(Configuration.class).toString());
				}
			} catch (Throwable e) {
				Logger.error("internal error for " + cc.name, e);
			}
		}
		this.pack();
	}
	
	private void buildUIItems() {
		
		panelItems.removeAll();
		
		int cPage = 0;
		int lineCount = 0;
		for(Enumeration en = configItems.elements(); en.hasMoreElements(); ) {
			final  ConfigurationComponent cc = (ConfigurationComponent)en.nextElement();
			
			if (cc.name == null) {
				cPage ++;
				continue;
			}
			if (cPage != page) {
				cc.guiComponent = null;
				continue;
			}
			
			Class type = cc.configField.getType();
			
			if ((type.equals(boolean.class)) || (type.equals(BooleanVar.class))) {
				Checkbox c = new Checkbox();
				cc.guiComponent = c;
			} else if ((type.equals(String.class)) || (type.equals(StringVar.class)) || (type.equals(UUID.class)) || (type.equals(int.class))|| (type.equals(IntVar.class))) {
				TextField tf = new TextField(); 
				cc.guiComponent = tf; 
			} else {
				Logger.error("internal error for " + cc.name + " unsupported class " + type.getName());
				return;
			}

			Label l = new Label((cc.guiName == null)?cc.name:cc.guiName);
			panelItems.add(l);
			panelItems.add(cc.guiComponent);
			
			lineCount ++;
			
			l.addMouseListener(new MouseAdapter() {
				Component guiComponent = cc.guiComponent;
				public void mouseClicked(MouseEvent e) {
					guiComponent.requestFocus();
				}
			});
		}
		panelItems.setLayout(new GridLayout(lineCount, 2));
		btnPagePrev.setEnabled((page > 0));
		btnPageNext.setEnabled((cPage > page));
	}
	
	private void updateConfig() {
		for(Enumeration en = configItems.elements(); en.hasMoreElements(); ) {
			ConfigurationComponent cc = (ConfigurationComponent)en.nextElement();
			if (cc.guiComponent == null) {
				continue;
			}
			Class type = cc.configField.getType();
			try {
				if (type.equals(boolean.class)) {
					Checkbox c = (Checkbox) cc.guiComponent;
					cc.configField.setBoolean(Configuration.class, c.getState());
				} else if (type.equals(BooleanVar.class)) {
					Checkbox c = (Checkbox) cc.guiComponent;
					((BooleanVar)cc.configField.get(Configuration.class)).setValue(c.getState());
				} else if (type.equals(String.class)) {
					TextField tf = (TextField) cc.guiComponent;
					cc.configField.set(Configuration.class, tf.getText().trim());
				} else if (type.equals(StringVar.class)) {
					TextField tf = (TextField) cc.guiComponent;
					((StringVar)cc.configField.get(Configuration.class)).setValue(tf.getText().trim());
				} else if (type.equals(int.class)) {
					TextField tf = (TextField) cc.guiComponent;
					cc.configField.setInt(Configuration.class, Integer.valueOf(tf.getText().trim()).intValue());
				} else if (type.equals(IntVar.class)) {
					TextField tf = (TextField) cc.guiComponent;
					((IntVar)cc.configField.get(Configuration.class)).setValue(tf.getText());
				} else if (type.equals(UUID.class)) {
					TextField tf = (TextField) cc.guiComponent;
					cc.configField.set(Configuration.class, BluetoothTypesInfo.UUIDConsts.getUUID(tf.getText().trim())); 
				}
			} catch (Throwable e) {
				Logger.error("internal error for " + cc.name, e);
			}
		}
	}
	
	private void addConfig(String name) {
		addConfig(name, null);
	}
	
	private void addConfig(String name, String guiName) {
		ConfigurationComponent cc = new ConfigurationComponent();
		cc.name = name;
		if (cc.name != null) {
			try {
				cc.configField = Configuration.class.getDeclaredField(name);
			} catch (Throwable e) {
				Logger.error("internal error for " + name, e);
				return;
			}
		}
		cc.guiName = guiName;
		cc.guiComponent = null;
		configItems.addElement(cc);
	}


}