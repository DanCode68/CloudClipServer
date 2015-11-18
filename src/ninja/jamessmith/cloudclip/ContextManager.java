package ninja.jamessmith.cloudclip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;

import org.json.simple.JSONObject;

public class ContextManager {

	private Map<String, List<String>> clipboards;
	private Map<String, Map<String, AsyncContext>> contexts;
	private BlockingQueue<String[]> messages;
	
	public ContextManager() {
		clipboards = new HashMap<String, List<String>>();
		contexts = new HashMap<String, Map<String, AsyncContext>>();
		messages = new LinkedBlockingQueue<String[]>();
	}
	
	public void addContext(String session, AsyncContext context) throws IOException {
		String uuid = UUID.randomUUID().toString();
		JSONObject json = new JSONObject();
		
		if (!contexts.containsKey(session)) {
			contexts.put(session, new HashMap<String, AsyncContext>());
			clipboards.put(session, new ArrayList<String>());
		}
		
		contexts.get(session).put(uuid, context);
		json.put(ServiceServlet.UUID_HEADER, uuid);
		context.getResponse().getWriter().print(json.toJSONString());
		
		for (String s : clipboards.get(session)) {
			JSONObject clip = new JSONObject();
			clip.put("method", "add");
			clip.put("string", s);
			context.getResponse().getWriter().print(clip.toJSONString());
		}
		
		context.complete();
	}
	
	public void removeContext(String session, String uuid) {
		if (contexts.containsKey(session)) {
			Map<String, AsyncContext> map = contexts.get(session);
			if (map.containsKey(uuid)) {
				map.get(uuid).complete();
				map.remove(uuid);
			}
			if (map.isEmpty()) {
				contexts.remove(session);
				clipboards.remove(session);
			}
		}
	}
	
	public void addClip(String session, String uuid, String clip) {
		List<String> clipboard = clipboards.get(session);
		if (!clipboard.contains(clip)) {
			clipboard.add(clip);
		}
	}
	
	public void removeClip(String session, String uuid, String clip) {
		List<String> clipboard = clipboards.get(session);
		clipboard.remove(clip);
	}
}