package rosrabota_test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SimpleWEBServer extends Thread {
	
	private static int PORT = 8080;
	
	Socket socket;
	
	public SimpleWEBServer(final Socket socket) {
		this.socket = socket;
		setDaemon(true);
        setPriority(NORM_PRIORITY);
        start();
	}

	public static void main(final String... args) {
		ServerSocket server;
		try {
			server = new ServerSocket(PORT, 0, InetAddress.getByName("localhost"));
			System.out.println("server listen port " + PORT + "\n");

			while(true)	{		
				new SimpleWEBServer(server.accept());
			}
        
		} catch (IOException e) {
			System.out.println("init error: " + e);
		}
	}
	
	public void run() {
		try { 
			InputStream input = socket.getInputStream();
			OutputStream output = socket.getOutputStream();
			
			byte buf[] = new byte[64*1024];
            int size = input.read(buf);
            
            String request = new String(buf, 0, size);
            String path = getPath(request);
            System.out.println("path: " + path);
            
            {
            	String response = null;
            	if(path == null) {
            		response = this.createBadRequestResponse();
            		output.write(response.getBytes());
                    socket.close();
                    return;
            	} else if (path.equals("")) {
            		response = this.createSimpleResponse("<img src=\"http://localhost:8080/banner/1\" />");
            		output.write(response.getBytes());
                    socket.close();
                    return;
            	} else {
            		String filepath =  "b1.gif";
            		File file = new File(filepath);
            		response = this.createImageResponseHead(file);
            		output.write(response.getBytes());
            		FileInputStream fis = new FileInputStream(file);
            		size = 1;
                    while(size > 0)
                    {
                    	size = fis.read(buf);
                        if(size > 0) output.write(buf, 0, size);
                    }
                    fis.close();
                    socket.close();
            	}
                
            }     
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String getPath(final String request) {
		String path, URI = extract(request, "GET ", " ");
        /*if(URI == null) {
        	URI = extract(request, "POST ", " ");
        }*/
        if(URI == null) {
        	return null;
        }
        
        path = URI.toLowerCase();
        if(path.indexOf("http://", 0) == 0) {
            URI = URI.substring(7);
            URI = URI.substring(URI.indexOf("/", 0));
        } else if(path.indexOf("/", 0) == 0) {
            URI = URI.substring(1); 
        }

        int i = URI.indexOf("?");
        if(i > 0) {
        	URI = URI.substring(0, i);
        }
        i = URI.indexOf("#");
        if(i > 0) {
        	URI = URI.substring(0, i);
        }
		return URI;
	}

	protected String extract(String str, String start, String end) {
        int s = str.indexOf("\n\n", 0), e;
        if(s < 0) {
        	s = str.indexOf("\r\n\r\n", 0);
        }
        if(s > 0) {
        	str = str.substring(0, s);
        }
        s = str.indexOf(start, 0) + start.length();
        if(s < start.length()) {
        	return null;
        }
        e = str.indexOf(end, s);
        if(e < 0) {
        	e = str.length();
        }
        return (str.substring(s, e)).trim();
    }
	
	private String createImageResponseHead(File file) {
        String response = "HTTP/1.1 200 OK\n";
        DateFormat df = DateFormat.getTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        response = response + "Last-Modified: " + df.format(new Date(file.lastModified())) + "\n";
        response = response + "Content-Length: " + file.length() + "\n";

        // строка с MIME кодировкой
        response = response + "Content-Type: " + "image/gif" + "\n";

        // остальные заголовки
        response = response
        + "Connection: close\n"
        + "Server: SimpleWEBServer\n\n";
        return response;
	}
	
	private String createSimpleResponse() {
		String response = "HTTP/1.1 200 OK\n";
		DateFormat df = DateFormat.getTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        response = response 
        		+ "Date: " 
        		+ df.format(new Date()) 
        		+ "\n";
        response = response
        		+ "Connection: close\n"
        		+ "Server: SimpleWEBServer\n"
        		+ "Pragma: no-cache\n\n";
        return response;
	}
	
	
	private String createSimpleResponse(String msg) {
		String response = createSimpleResponse() + msg;
        return response;
	}

	private String createBadRequestResponse() {
		String response = "HTTP/1.1 400 Bad Request\n";    
        DateFormat df = DateFormat.getTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        response = response 
        		+ "Date: " 
        		+ df.format(new Date()) 
        		+ "\n";
        response = response
        		+ "Connection: close\n"
        		+ "Server: SimpleWEBServer\n"
        		+ "Pragma: no-cache\n\n";
        return response;
	}

}
