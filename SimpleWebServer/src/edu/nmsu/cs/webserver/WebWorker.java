package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 *
 * @author Jon Cook, Ph.D.
 *
 **/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

	private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			String getFileName = readHTTPRequest(is);
			File getFile = null;

			if (getFileName != null)
			{
				getFile = new File(getFileName);
			}


			if (getFile.exists())
			{
				writeHTTPHeader(os, "text/html", "200 OK");
			}
			else {
				writeHTTPHeader(os, "text/html", "404 Not Found");
			}
			writeContent(os, getFile);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private String readHTTPRequest(InputStream is)
	{
		String line;
		String getFile = null;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				System.err.println("Request line: (" + line + ")");

				if (line.startsWith("GET"))
				{
					getFile = line.substring(5, line.length() - 9);
				}
				if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return getFile;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 *
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType, String response) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		String r = "HTTP/1.1" + response;
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		os.write(r.getBytes());
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 *
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, File getFile) throws Exception
	{
		if (getFile.exists())
		{
			BufferedReader f = new BufferedReader(new FileReader(getFile));
			String line = f.readLine();
			line = tagReplacer(line);

			os.write("<html><head></head><body>\n".getBytes());
			os.write(new String("<h1>localfile:" + getFile.getAbsolutePath() + "</h1>").getBytes());

			while (line != null)
			{
				line = tagReplacer(line);
				os.write(line.getBytes());
				line = f.readLine();

			}
		}
		else
		{
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h1>404 Not Found</h1>\n".getBytes());
			os.write("</body></html>\n".getBytes());
		}
	}

	private String tagReplacer(String line)
	{
		String answer = line;

		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();

		answer = answer.replace("<cs371date>", df.format(d));
		answer = answer.replace("<cs371server>", "Justin's Server");
		return answer;
	}

} // end class
