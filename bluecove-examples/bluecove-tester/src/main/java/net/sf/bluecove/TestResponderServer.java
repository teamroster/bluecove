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
package net.sf.bluecove;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import junit.framework.Assert;

public class TestResponderServer implements CanShutdown, Runnable {
	
	public static int countSuccess = 0; 
	
	public static int countFailure = 0;
	
	public static int countConnection = 0;
	
	private boolean stoped = false;
	
	boolean isRunning = false;
	
	private StreamConnectionNotifier server;
	
	private TestTimeOutMonitor monitor;
	
	private class ConnectionTread extends Thread {
		
		StreamConnection conn;
		
		ConnectionTread(StreamConnection conn) {
			super("ConnectionTread" + (++countConnection));
			this.conn = conn;
		}
		
		public void run() {
			InputStream is = null;
			OutputStream os = null;
			int testType = 0;
			try {
				is = conn.openInputStream();
				os = conn.openOutputStream();
				testType = is.read();

				if (testType == Consts.TEST_TERMINATE) {
					Logger.info("Stop requested");
					shutdown();
					return;
				}
				CommunicationTester.runTest(testType, true, is, os);
				os.write(Consts.TEST_REPLY_OK);
				os.write(testType);
				os.flush();
				countSuccess++;
				Logger.debug("Test# " + testType + " ok");
			} catch (Throwable e) {
				countFailure++;
				Logger.error("Test# " + testType + " error", e);
			} finally {
				IOUtils.closeQuietly(os);
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(conn);
			}
			Logger.info("*Test Success:" + countSuccess + " Failure:" + countFailure);
		}
		
	}
	
	public TestResponderServer() throws BluetoothStateException {
		
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		Logger.info("address:" + localDevice.getBluetoothAddress());
		Logger.info("name:" + localDevice.getFriendlyName());
 	    
		Assert.assertNotNull("BT Address", localDevice.getBluetoothAddress());
		Assert.assertNotNull("BT Name", localDevice.getFriendlyName());
		
		localDevice.setDiscoverable(DiscoveryAgent.GIAC);

	}
	
	public void run() {
		stoped = false;
		isRunning = true;
		if (!CommunicationTester.continuous) {
			monitor = new TestTimeOutMonitor(this, Consts.serverTimeOutMin);
		}
		try {
			server = (StreamConnectionNotifier) Connector
					.open("btspp://localhost:"
							+ CommunicationTester.uuid
							+ ";name="
							+ Consts.RESPONDER_SERVERNAME
							+ ";authorize=false;authenticate=false;encrypt=false");

			Logger.info("ResponderServer started");
			
			while (!stoped) {
				Logger.info("Accepting connection");
				StreamConnection conn = server.acceptAndOpen();

				Logger.info("Received connection");
				(new ConnectionTread(conn)).start();
			}

			server.close();
			server = null;
		} catch (IOException e) {
			if (!stoped) {
				Logger.error("Server start error", e);
			}
		} finally {
			Logger.info("Server finished");
			isRunning = false;
		}
		if (monitor != null) {
			monitor.finish();
		}
	}

	public void shutdown() {
		Logger.info("shutdownServer");
		stoped = true;
		if (server != null) {
			try {
				server.close();
			} catch (IOException e) {
			}
		}
	}
	
	public static void main(String[] args) {
		JavaSECommon.initOnce();
		try {
			(new TestResponderServer()).run();
			if (TestResponderServer.countFailure > 0) {
				System.exit(1);
			} else {
				System.exit(0);
			}
		} catch (Throwable e) {
			Logger.error("start error ", e);
			System.exit(1);
		}
	}

}
