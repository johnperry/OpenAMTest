/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.openam;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import org.rsna.util.*;
import org.rsna.ui.*;
import org.w3c.dom.*;

/**
 * A test program for trying to connect to an OpenAM server.
 */
public class OpenAMTest extends JFrame {

	MainPanel mainPanel;
	ColorPane cp;
	Color bgColor = new Color(0xc6d8f9);

	Row openAMURL;
	Row redirectURL;
	String ssoCookieName = "";

	public static void main(String args[]) {
		new OpenAMTest();
	}

	/**
	 * Class constructor.
	 */
	public OpenAMTest() {
		super();

		setTitle("OpenAM Test Utility");

		mainPanel = new MainPanel();
		cp = new ColorPane();
		cp.setScrollableTracksViewportWidth(false);

		JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT);
		splitPane.setContinuousLayout(true);
		splitPane.setTopComponent(mainPanel);
		JScrollPane jsp = new JScrollPane();
		jsp.getViewport().setBackground(Color.white);
		jsp.setViewportView(cp);
		splitPane.setBottomComponent(jsp);

		this.getContentPane().add( splitPane, BorderLayout.CENTER );
		this.addWindowListener(new WindowCloser(this));

		pack();
		positionFrame();
		setVisible(true);
		splitPane.setDividerLocation(-1);
	}

	private void positionFrame() {
		Toolkit tk = getToolkit();
		Dimension scr = tk.getScreenSize ();
		setSize( 600, 700 );
		int x = (scr.width - getSize().width)/2;
		int y = (scr.height - getSize().height)/2;
		setLocation( new Point(x,y) );
	}

    class WindowCloser extends WindowAdapter {
		public WindowCloser(JFrame parent) { }
		public void windowClosing(WindowEvent evt) {
			System.exit(0);
		}
    }

	class MainPanel extends JPanel implements ActionListener {

		JButton browse;
		JLabel tomcat;
		JButton cookieButton;
		JButton loginButton;
		JButton validateButton;
		JButton attributesButton;
		JButton logoutButton;
		Server server = null;

		public MainPanel() {
			super();
			setLayout(new BorderLayout());
			setBackground(bgColor);

			add( new TitlePanel(), BorderLayout.NORTH );

			JPanel centerPanel = new CenterPanel();
			JPanel centerLR = new JPanel();
			centerLR.add(centerPanel);
			centerLR.setBackground(bgColor);
			add( centerLR, BorderLayout.CENTER );

			JPanel footer = new FooterPanel();
			cookieButton = new JButton("SSO Cookie");
			cookieButton.setEnabled(true);
			cookieButton.addActionListener(this);
			footer.add(cookieButton);
			footer.add(Box.createHorizontalStrut(5));

			loginButton = new JButton("Login");
			loginButton.setEnabled(true);
			loginButton.addActionListener(this);
			footer.add(loginButton);
			footer.add(Box.createHorizontalStrut(5));

			validateButton = new JButton("Validate");
			validateButton.setEnabled(true);
			validateButton.addActionListener(this);
			footer.add(validateButton);
			footer.add(Box.createHorizontalStrut(5));

			attributesButton = new JButton("Attributes");
			attributesButton.setEnabled(true);
			attributesButton.addActionListener(this);
			footer.add(attributesButton);
			footer.add(Box.createHorizontalStrut(5));

			logoutButton = new JButton("Logout");
			logoutButton.setEnabled(true);
			logoutButton.addActionListener(this);
			footer.add(logoutButton);

			add( footer, BorderLayout.SOUTH );
		}

		public void actionPerformed(ActionEvent event) {
			try {
				String baseURL = openAMURL.tf.getText();
				String serverURL = redirectURL.tf.getText();
				if (server == null) {
					URL url = new URL(serverURL);
					int serverPort = url.getPort();
					cp.println("Starting the server on port "+serverPort);
					server = new Server(serverPort, cp);
					server.start();
					cp.println("OK\n");
				}
				if (event.getSource().equals(cookieButton)) {
					ssoCookieName = OpenAMUtil.getCookieName(baseURL);
					cp.println("Cookie name: "+ssoCookieName+"\n");
				}
				else if (event.getSource().equals(loginButton)) {
					cp.println("Launching the browser: redirect URL: "+serverURL);
					cp.println(OpenAMUtil.login(baseURL, serverURL)+"\n");
				}
				else if (event.getSource().equals(validateButton)) {
					String token = server.getCookie(ssoCookieName);
					cp.println("Validating token: "+token);
					cp.println("...result = "+OpenAMUtil.validate(baseURL, token)+"\n");
				}
				else if (event.getSource().equals(attributesButton)) {
					String token = server.getCookie(ssoCookieName);
					cp.println("Attributes:");
					String result = OpenAMUtil.getAttributes(baseURL, token);
					cp.println(Color.black, result);
					cp.println("\nParsed Table:");
					Hashtable<String,LinkedList<String>> attrs = OpenAMUtil.parseAttributes(result);
					for (String key : attrs.keySet()) {
						LinkedList<String> values = attrs.get(key);
						cp.println(Color.black, "  "+key);
						if (values != null) {
							for (String value : values) {
								cp.println(Color.blue, "      "+value);
							}
						}
					}
					cp.println(Color.black, "\n");
				}
				else if (event.getSource().equals(logoutButton)) {
					cp.println(OpenAMUtil.logout(baseURL)+"\n");
				}
			}
			catch (Exception ex) {
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				cp.println(Color.red, "\n"+sw.toString());
				cp.println(Color.black,"");
			}
		}
	}

	class TitlePanel extends JPanel {
		public TitlePanel() {
			super();
			setBackground(bgColor);
			JLabel title = new JLabel("OpenAM Test Utility");
			title.setFont( new Font( "SansSerif", Font.BOLD, 24 ) );
			title.setForeground( Color.BLUE );
			add(title);
			setBorder(BorderFactory.createEmptyBorder(20,0,20,0));
		}
	}

	class CenterPanel extends RowPanel {
		public CenterPanel() {
			super("OpenAM Parameters");
			setBackground(bgColor);
			addRow( openAMURL = new Row("OpenAM Server:", "http://nibib-3.wustl.edu:80") );
			addRow( redirectURL = new Row("Test Server:", "http://nibib-3.wustl.edu:9999") );
		}
	}

	class FooterPanel extends JPanel {
		public FooterPanel() {
			super();
			setBackground(bgColor);
			setBorder(BorderFactory.createEmptyBorder(10,0,25,0));
		}
	}

	class RowPanel extends JPanel {
		public RowPanel() {
			super();
			setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
			setLayout(new RowLayout());
		}
		public RowPanel(String name) {
			super();
			Border empty = BorderFactory.createEmptyBorder(10,10,10,10);
			Border line = BorderFactory.createLineBorder(Color.GRAY);
			Border title = BorderFactory.createTitledBorder(line, name);
			Border compound = BorderFactory.createCompoundBorder(title, empty);
			setBorder(compound);
			setLayout(new RowLayout());
		}
		public void addRow(Row row) {
			add(row.label);
			add(row.tf);
			add(RowLayout.crlf());
		}
		public void addRow(int height) {
			add(Box.createVerticalStrut(height));
			add(RowLayout.crlf());
		}
		public void addRow(JLabel label) {
			add(label);
			add(RowLayout.crlf());
		}
	}

	class Row {
		public RowLabel label;
		public JTextField tf;
		public Row(String name) {
			label = new RowLabel(name);
			tf = new RowTextField(50);
		}
		public Row(String name, String value) {
			label = new RowLabel(name);
			tf = new RowTextField(50);
			tf.setText(value);
		}
	}

	class RowLabel extends JLabel {
		public RowLabel(String s) {
			super(s);
			Dimension d = this.getPreferredSize();
			d.width = 140;
			this.setPreferredSize(d);
		}
	}

	class RowTextField extends JTextField {
		public RowTextField(int size) {
			super(size);
			setFont( new Font("Monospaced",Font.PLAIN,12) );
		}
	}

}
