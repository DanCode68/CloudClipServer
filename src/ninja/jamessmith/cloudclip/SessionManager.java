package ninja.jamessmith.cloudclip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import org.json.simple.JSONObject;

public class SessionManager {

	private Map<String, List<String>> clipboards;
	private Map<String, Map<String, ByteArrayOutputStream>> sessions;
	private BlockingQueue<String[]> messages; // 0=session, 1=uuid, 2=method, 3=clip	
	final private Thread notifier = new Thread(new Runnable() {

		@Override
		public void run() {
			while (true) {
				try {
					String[] message = messages.take();
					JSONObject json = new JSONObject();					
					Map<String, ByteArrayOutputStream> session = sessions.get(message[0]);
					ByteArrayOutputStream originStream = session.get(message[1]);

					json.put("method", message[2]);
					json.put("clip", message[3]);
					
					for (ByteArrayOutputStream stream : session.values()) {
						if (originStream != stream) {
							synchronized(stream) {
								PrintWriter writer = new PrintWriter(stream);
								writer.println(json.toJSONString());
								writer.flush();
							}
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	});
	
	public SessionManager() {
		clipboards = new HashMap<String, List<String>>();
		sessions = new HashMap<String, Map<String, ByteArrayOutputStream>>();
		messages = new LinkedBlockingQueue<String[]>();
		
		notifier.start();
	}
	
	public void addSession(final String sessionKey, AsyncContext context) throws IOException {
		final String uuid = UUID.randomUUID().toString();
		JSONObject json = new JSONObject();
		
		if (!sessions.containsKey(sessionKey)) {
			sessions.put(sessionKey, new HashMap<String, ByteArrayOutputStream>());
			clipboards.put(sessionKey, new ArrayList<String>());
		}
		
		sessions.get(sessionKey).put(uuid, new ByteArrayOutputStream());
		json.put(ServiceServlet.UUID_HEADER, uuid);
		context.getResponse().getWriter().println(json.toJSONString());
		
		for (String s : clipboards.get(sessionKey)) {
			JSONObject clip = new JSONObject();
			clip.put("method", "add");
			clip.put("clip", s);
			context.getResponse().getWriter().println(clip.toJSONString());
		}
		
		context.addListener(new AsyncListener() {

			@Override
			public void onComplete(AsyncEvent arg0) throws IOException {
				
			}

			@Override
			public void onError(AsyncEvent arg0) throws IOException {
				removeSession(sessionKey, uuid);
			}

			@Override
			public void onStartAsync(AsyncEvent arg0) throws IOException {
				
			}

			@Override
			public void onTimeout(AsyncEvent arg0) throws IOException {
				removeSession(sessionKey, uuid);
			}			
		});
		
		context.complete();
	}
	
	public void removeSession(String sessionKey, String uuid) {
		if (sessions.containsKey(sessionKey)) {
			Map<String, ByteArrayOutputStream> session = sessions.get(sessionKey);
			if (session.containsKey(uuid)) {
				session.remove(uuid);
			}
			if (session.isEmpty()) {
				sessions.remove(sessionKey);
				clipboards.remove(sessionKey);
			}
		}
	}
	
	public void addClip(String sessionKey, String uuid, String clip) {
		List<String> clipboard = clipboards.get(sessionKey);
		if (!clipboard.contains(clip)) {
			clipboard.add(clip);
			
			String[] message = new String[4];
			message[0] = sessionKey;
			message[1] = uuid;
			message[2] = "add";
			message[3] = clip;
			
			messages.add(message);
		}
	}
	
	public void removeClip(String sessionKey, String uuid, String clip) {
		List<String> clipboard = clipboards.get(sessionKey);
		clipboard.remove(clip);
		
		String[] message = new String[4];
		message[0] = sessionKey;
		message[1] = uuid;
		message[2] = "remove";
		message[3] = clip;
		
		messages.add(message);
	}
	
	public void fetch(final String sessionKey, final String uuid, final AsyncContext context) throws IOException {
		final ByteArrayOutputStream stream = sessions.get(sessionKey).get(uuid);
		
		if (stream.size() > 0) {
			synchronized (stream) {
				stream.writeTo(context.getResponse().getOutputStream());
				stream.reset();
			}
			context.complete();
		}
		else {
			new Thread(new Runnable() {

				@Override
				public void run() {
					boolean isDone = false;
					
					while (sessions.get(sessionKey).get(uuid) != null && !isDone) {
						if (stream.size() > 0) {
							try {
								synchronized (stream) {
									stream.writeTo(context.getResponse().getOutputStream());
									stream.reset();
								}
								
								isDone = true;
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} 
						} 
					}
					
					context.complete();
				}
				
			}).start();
		}
	}
}