/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.openam;

import java.io.*;
import java.net.*;
import javax.net.*;
import java.util.*;
import org.rsna.ui.ColorPane;


/**
 * A simple HTTP Server for testing the OpenAM system..
 */
public class Server extends Thread {

	final int port;
	ColorPane cp;
	final ServerSocket serverSocket;
	public Hashtable<String,String> headersTable = new Hashtable<String,String>();
	public Hashtable<String,String> cookiesTable = new Hashtable<String,String>();

	/**
	 * Class constructor; creates a new instance of
	 * the TestServer thread on the specified port.
	 * @param port the port on which this server listens for connections
	 * @throws Exception if the ServerSocket cannot be created.
	 */
    public Server(int port, ColorPane cp) throws Exception {
		super();
		this.port = port;
		this.cp = cp;
		ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
		serverSocket = serverSocketFactory.createServerSocket(port);
 	}

	/**
	 * Start the HttpServer and accept connections.
	 */
	public void run() {
		while (!this.isInterrupted()) {
			try {
				//Wait for a connection
				final Socket socket = serverSocket.accept();

				//Handle the connection in a separate thread
				Handler handler = new Handler(socket);
				handler.start();
			}
			catch (Exception ex) { break; }
		}
		try { serverSocket.close(); }
		catch (Exception ignore) { }
	}

	public String getHeader(String name) {
		return headersTable.get(name.toLowerCase());
	}

	public String getCookie(String name) {
		return cookiesTable.get(name.toLowerCase());
	}

	public void shutdown() {
		this.interrupt();
	}

	class Handler extends Thread {

		Socket socket;

		public Handler(Socket socket) {
			super("TestHandler");
			this.socket = socket;
		}

		public void run() {
			BufferedInputStream inStream = null;
			OutputStream outStream = null;
			Response response = new Response();
			try {
				//Get the socket streams
				inStream = new BufferedInputStream(socket.getInputStream());
				outStream = socket.getOutputStream();

				//Get the addresses
				String remoteAddress = getRemoteAddress(socket);

				//Get the data from the connection
				headersTable.clear();
				cookiesTable.clear();
				String headers = getHeaders(inStream);
				String content = getContent(inStream);

				//Make a report
				StringBuffer sb = new StringBuffer();
				sb.append("Connection received from "+remoteAddress+"\n");
				sb.append("Headers received by the server:\n");
				sb.append(headers);
				sb.append("Content received by the server:\n");
				sb.append(content);
				if (content.length() == 0) sb.append("[none]");
				String report = sb.toString();

				response.write("The server reported this information about the connection it received:\n\n");
				response.write(report);
				response.send(outStream);

				outStream.flush();
				outStream.close();
				inStream.close();

				cp.println(report+"\n");
			}

			catch (Exception ex) { ex.printStackTrace(); }
			try { socket.close(); }
			catch (Exception ignore) { }
		}

		public String getRemoteAddress(Socket socket) {
			SocketAddress rsa = socket.getRemoteSocketAddress();
			String rsaString = "unknown";
			if ((rsa != null) && (rsa instanceof InetSocketAddress)) {
				rsaString = ((InetSocketAddress)rsa).getAddress().getHostAddress();
			}
			return rsaString;
		}

		//Add the headers to the headersTable,
		//positioning the stream to the beginning of the data.
		private String getHeaders(BufferedInputStream in) {
			StringBuffer sb = new StringBuffer();
			String line;
			while (!((line=getLine(in)).equals(""))) {
				sb.append(line + "\n");
				int k = line.indexOf(":");
				if (k != -1) {
					String name = line.substring(0,k).trim().toLowerCase();
					String value = line.substring(k+1).trim();
					headersTable.put(name, value);
					if (name.equals("cookie")) addCookies(value);
				}
			}
			return sb.append("\n").toString();
		}

		//Add a set of cookies to the cookiesTable
		private void addCookies(String value) {
			String[] parts = value.split(";");
			for (int i=0; i<parts.length; i++) {
				String part = parts[i];
				int k = part.indexOf("=");
				if (k > 0) {
					String name = part.substring(0,k).trim().toLowerCase();
					if (!name.startsWith("$")) {
						String val = part.substring(k+1).trim();
						cookiesTable.put(name, val);
					}
				}
			}
		}

		//Get one header line from the stream,
		//using \r\n as the delimiter.
		private String getLine(BufferedInputStream in) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			boolean rFlag = false;
			int b;
			try {
				while ((b=in.read()) != -1) {
					baos.write(b);
					if (rFlag && (b == 10)) break;
					rFlag = (b == 13);
				}
			}
			catch (Exception ex) { }
			return baos.toString().trim();
		}

		//Get the content passed in the connection as a String.
		private String getContent(BufferedInputStream is) {
			String contentLength = headersTable.get("content-length");
			int count = 0;
			try { count = Integer.parseInt(contentLength); }
			catch (Exception ignore) { }

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int b;
			try {
				while ((count > 0) && (b=is.read()) != -1) {
					baos.write(b);
					count--;
				}
				return baos.toString();
			}
			catch (Exception e) {
				return
					"Error reading the input stream\nException message:\n"
					+ e.getMessage() + "\n\n"
					+ baos.toString();
			}
		}

	}

	class Response {

		StringBuffer response = null;

		public Response() {
			response = new StringBuffer();
		}

		public void write(String string) {
			response.append(string);
		}

		public void send(OutputStream stream) {
			String headers =
				"HTTP/1.0 200 OK\r\n" +
				"Content-Type: text/plain\r\n" +
				"Content-Length: " + response.length() + "\r\n\r\n";

			try {
				stream.write(headers.getBytes());
				stream.write(response.toString().getBytes());
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}
	}
}
