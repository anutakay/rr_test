package rosrabota_test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.regex.Matcher;  
import java.util.regex.Pattern; 

public class Server {
	
	public static int PORT = 8080;
	
	public Socket socket;
	
	public static Map<String, String> bannerMap = new HashMap<String,String>();
	public static Map<String, Integer> counterMap = new HashMap<String, Integer>();
	
	public List<Route> routeList = new ArrayList<Route>();
	
	@FunctionalInterface
	public interface WorkerInterface {
	
	    public void sendResponse(OutputStream out, String msg);
	 
	}
	
	public class Route {
		
		String regex;
		
		WorkerInterface function;
		
		public Route(String regex, WorkerInterface function) {
			this.regex = regex;
			this.function = function;
		}
	};
	
	public Server(final Socket socket) {
		this.socket = socket;

		addRoutes();
		runThread();
	}
	
	public static void createCounters() {
		bannerMap.forEach((k, v) -> {
			counterMap.put(k, 0);
		});
	}

	public void addRoutes() {
		addRoute("^stats$", (OutputStream out, String path) -> sendStatsResponse(out));
		addRoute("^main$", (OutputStream out, String path) -> {
			String str = "<img src=\"http://localhost:8080/banner/1\" vspace=\"10\"/>"
					+ "<img src=\"http://localhost:8080/banner/2\" vspace=\"10\"/>"
					+ "<img src=\"http://localhost:8080/banner/3\" vspace=\"10\"/>";
			sendSimpleResponse(out, str);
		});
		addRoute("^banner/([a-z0-9_-]+)$", (OutputStream out, String path) -> {
			String id = extarctValueFromString("^banner/([a-z0-9_-]+)$", 1, path);
			String filename = getImageFileNameForId(id);
			if(filename == null) {
				sendNotFoundResponse(out);
			} else {
				counterMap.replace(id, counterMap.get(id) + 1);
				sendImageResponse(out, filename);
			}
		});
	}

	public String getImageFileNameForId(String id) {
		return bannerMap.get(id);
	}

	public void addRoute(final String regex, final WorkerInterface function) {		
		routeList.add(new Route(regex, function));
	}
	
	public void runThread() {	
		Thread thread = new Thread(runnable);
		thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
	}

	public Runnable runnable = new Runnable() {
		public void run() {
			try { 
				InputStream input = socket.getInputStream();
				OutputStream output = socket.getOutputStream();
			
				byte buf[] = new byte[64*1024];
				int size = input.read(buf);  
				String request = new String(buf, 0, size);
				String path = getPath(request);
                                
				route(output, path);
				socket.close(); 
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	
	public void route(OutputStream output, String path) throws IOException {
		
		if(path == null) {
			sendBadRequestResponse(output);
		}
		
		try {
			routeList.stream()
			.filter(r -> test(r.regex, path))
			.findFirst().get().function.sendResponse(output, path);
		} catch(NoSuchElementException e) {
			 sendNotFoundResponse(output);
		}
	}
	
	public static boolean test(String regex, String testString) {  
        Pattern p = Pattern.compile(regex);  
        Matcher m = p.matcher(testString);  
        return m.matches(); 
    }
	
	public static String extarctValueFromString(String regex, int group, String str) {
		Pattern p = Pattern.compile(regex);  
        Matcher m = p.matcher(str);  
        if( m.find()) {
        	return m.group(group);
        }
        return null; 
	}

	public void sendImageResponse(OutputStream output, String filename) {
		String response;
		File file = new File(filename);
		response = this.createImageResponseHead(file);
		try {
			output.write(response.getBytes());
			FileInputStream fis = new FileInputStream(file);
			
			int size = 1;
			byte buf[] = new byte[64*1024];
			
			while(size > 0) {
				size = fis.read(buf);
				if(size > 0) {
					output.write(buf, 0, size);
				}
			}
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getPath(final String request) {
		String path, URI = extract(request, "GET ", " ");
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

	public String extract(String str, String start, String end) {
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
	
	public void sendStatsResponse(OutputStream out) {
		String response = createSimpleResponse() + "<table>";
		response = response	+ "<tr>"
				+ "<td>id</td>"
				+ "<td>filename</td>"
				+ "<td>count</td>"
				+ "</tr>";
		for (Map.Entry<String, String> entry : bannerMap.entrySet()) {
			response = response	+ "<tr>"
					+ "<td>" + entry.getKey() + "</td>"
					+ "<td>" + entry.getValue() + "</td>"
					+ "<td>" + counterMap.get(entry.getKey())+ "</td>"
					+ "</tr>";
		}
		
		response = response + "</table>";
		try {
			out.write(response.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
	
	public String createImageResponseHead(File file) {
        String response = "HTTP/1.1 200 OK\n";
        DateFormat df = DateFormat.getTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        response = response + "Last-Modified: " + df.format(new Date(file.lastModified())) + "\n";
        response = response + "Content-Length: " + file.length() + "\n";
        response = response + "Content-Type: " + "image/gif" + "\n";
        response = response
        + "Connection: close\n"
        + "Server: SimpleWEBServer\n\n";
        return response;
	}
	
	public String createSimpleResponse() {
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
	
	public void sendSimpleResponse(OutputStream output, String msg){
		String response = createSimpleResponse() + msg;
		try {
			output.write(response.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}

	public void sendBadRequestResponse(OutputStream output) throws IOException {
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
        output.write(response.getBytes());      
	}

	public void sendNotFoundResponse(OutputStream output) {
		String response = "HTTP/1.1 404 Not Found\n";    
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
        try {
			output.write(response.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}      
	}
}
