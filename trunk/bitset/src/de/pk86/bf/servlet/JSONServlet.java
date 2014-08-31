package de.pk86.bf.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import de.jdataset.DataSetFactory;
import de.jdataset.JDataSet;
import de.pk86.bf.ExpressionResult;
import de.pk86.bf.ObjectItemServiceIF;
import de.pk86.bf.client.ServiceFactory;



public class JSONServlet extends HttpServlet {
   private static final long serialVersionUID = 1L;
   private ObjectItemServiceIF sv;
   
   public JSONServlet() {
   	sv = ServiceFactory.getLocalService();   	
   }

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
//		Enumeration<String> enu = request.getParameterNames();
//		while(enu.hasMoreElements()) {
//			String paraname = enu.nextElement();
//			String value = request.getParameter(paraname);
//			JSONObject req = new JSONObject(value);
//			System.out.println(paraname + ":" + value);
//		}
		String expression = request.getParameter("expr");
		String next = request.getParameter("next");
		String prev = request.getParameter("prev");
		String add = request.getParameter("add");
		if (expression != null) {
			ExpressionResult res = sv.execute(expression);
			JSONObject jo = res.getJSONResult();
			doResponse(response, jo);
		} else if (next != null) {
			JSONObject jn = new JSONObject(next);
			int sessionId = jn.getInt("SessionId");
			JDataSet ds = sv.getNextPage(sessionId);
			JSONArray ja = DataSetFactory.toJSONArray(ds);
			doResponse(response, ja);
		} else if (prev != null) {
			JSONObject jp = new JSONObject(prev);
			int sessionId = jp.getInt("SessionId");
			JDataSet ds = sv.getNextPage(sessionId);
			JSONArray ja = DataSetFactory.toJSONArray(ds);
			doResponse(response, ja);
		} else if (add != null) {
			JSONObject jp = new JSONObject(add);
			String content = jp.getString("content");
			int oid = sv.createObject(content);
			JSONObject jo = new JSONObject();
			jo.put("ObjectId", oid);
			doResponse(response, jo);
		}
	}
	
	private void doResponse(HttpServletResponse response, Object o) throws IOException {
//		jo.put("vorname", "Max");
//		jo.put("nachname", "Mustermann");
//		jo.put("geburtsdatum", "11.11.1955");
//		jo.put("strasse", "Milchstraße 13");
//		jo.put("ort", "10115 Berlin");
		response.setContentType("application/json");
		// Get the printwriter object from response to write the required json
		// object to the output stream
		PrintWriter out = response.getWriter();
		// Assuming your json object is **jsonObject**, perform the following, it
		// will return your json object
		out.print(o);
		out.flush();
		
	}
}