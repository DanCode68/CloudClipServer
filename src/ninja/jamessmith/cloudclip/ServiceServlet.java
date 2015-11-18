package ninja.jamessmith.cloudclip;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

/**
 * Servlet implementation class ServiceServlet
 */
@WebServlet(asyncSupported = true, urlPatterns = { "/Service" })
public class ServiceServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final String PARAMETER_METHOD = "method";
	private static final String ADD_METHOD = "add";
	private static final String CONNECT_METHOD = "connect";
	private static final String DISCONNECT_METHOD = "disconnect";
	private static final String FETCH_METHOD = "fetch";
	private static final String REMOVE_METHOD = "remove";
	
	static final String UUID_HEADER = "uuid";
	static final String SESSION_HEADER = "session";
	static final String CLIP_HEADER = "clip";
	
	private ContextManager manager;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServiceServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		manager = new ContextManager();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String method = request.getParameter(PARAMETER_METHOD);
		
		if (method == null) {
			response.setStatus(401);
		}
		else if (method.equals(CONNECT_METHOD)) {
			doConnect(request, response);
		}
		else if (method.equals(DISCONNECT_METHOD)) {
			doDisconnect(request, response);
		}
		else if (method.equals(FETCH_METHOD)) {
			doFetch(request, response);
		}
		else {
			response.setStatus(401);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String method = request.getParameter(PARAMETER_METHOD);
		
		if (method == null) {
			response.setStatus(401);
		}
		else if (method.equals(ADD_METHOD)) {
			doAdd(request, response);
		}
		else if (method.equals(REMOVE_METHOD)) {
			doRemove(request, response);
		}
		else {
			response.setStatus(401);
		}
	}

	private void doAdd(HttpServletRequest req, HttpServletResponse resp) {
		String session = req.getHeader(SESSION_HEADER);
		String uuid = req.getHeader(UUID_HEADER);
		String clip = req.getHeader(CLIP_HEADER);
		
		manager.addClip(session, uuid, clip);
	}
	
	private void doConnect(HttpServletRequest req, HttpServletResponse resp) {
		String session = req.getHeader(SESSION_HEADER);
		
		if (session == null) {
			resp.setStatus(401);
		}
		else {
			try {
				manager.addContext(session, req.startAsync());
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void doDisconnect(HttpServletRequest req, HttpServletResponse resp) {
		String session = req.getHeader(SESSION_HEADER);
		String uuid = req.getHeader(UUID_HEADER);
		
		if (session == null || uuid == null) {
			resp.setStatus(401);
		}
		else {
			manager.removeContext(session, uuid);
		}
	}
	
	private void doFetch(HttpServletRequest req, HttpServletResponse resp) {
		
	}
	
	private void doRemove(HttpServletRequest req, HttpServletResponse resp) {
		String session = req.getHeader(SESSION_HEADER);
		String uuid = req.getHeader(UUID_HEADER);
		String clip = req.getHeader(CLIP_HEADER);
		
		manager.removeClip(session, uuid, clip);
	}
}
