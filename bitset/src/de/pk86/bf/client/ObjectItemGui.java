package de.pk86.bf.client;

import java.awt.Component;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JOptionPane;

import de.guibuilder.framework.GDLParseException;
import de.guibuilder.framework.GuiFactory;
import de.guibuilder.framework.GuiList;
import de.guibuilder.framework.GuiUtil;
import de.guibuilder.framework.GuiWindow;
import de.guibuilder.framework.event.GuiChangeEvent;
import de.guibuilder.framework.event.GuiUserEvent;
import de.jdataset.JDataSet;
import de.pk86.bf.ExpressionResult;
import de.pk86.bf.ObjectItemServiceIF;
import de.pk86.bf.Selection;
import de.pkjs.util.Convert;

/**
 * Experimentelle Oberfläche zum Testen.
 * <p>
 * Voraussetzung ist der Java GuiBuilder (http://www.guibuilder.de).
 */
public class ObjectItemGui {
	private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ObjectItemGui.class);
	private ObjectItemServiceIF sv;
	private String[] sOper = { "", "and", "or", "xor", "not" };
	private String sessionName = "~internal~";

	public static void main(String[] args) {
		new ObjectItemGui();
	}
	
	private ObjectItemGui() {
		sv = ServiceFactory.getDirectService();
		//sv = ServiceFactory.getSOAP_Service("http://pk86.de:8004/bitdemo.wsdl");		
		//sv = ServiceFactory.getSpringService();
		//sv = ServiceFactory.getSOAP_Service("http://localhost:8004/bf.wsdl");
		GuiFactory fact = GuiFactory.getInstance();
		GuiUtil.setUiManager("Nimbus");
		try {
			GuiWindow win = fact.createWindow("gui/Expression.xml");
			win.setController(this);
			win.show();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			GuiUtil.showEx(ex);
		}
	}

	public void startTrans(GuiUserEvent event) {
		sv.startSession("test");
	}
	
	public void getService(GuiUserEvent event) {
		GuiChangeEvent ce = (GuiChangeEvent)event;
		if (ce.value.equals("native")) {
			sv = ServiceFactory.getDirectService();					
		} else if (ce.value.equals("rmi")) {
			sv = ServiceFactory.getSpringService();		
		} else if (ce.value.equals("soap")) {
			sv = ServiceFactory.getSOAP_Service();		
		} else if (ce.value.equals("remoteSOAP")) {
			sv = ServiceFactory.getSOAP_Service("http://pk86.de:8004/bitdemo.wsdl");		
		} else if (ce.value.equals("remoteRMI")) {
			sv = ServiceFactory.getSpringService("pk86.de:1098");		
		}
	}
	
	public void doExit(GuiUserEvent event) {
		System.exit(1);
	}
	
	/**
	 * @deprecated schrittweise nicht mehr unterstützen
	 * @param event
	 */
	public void execute(GuiUserEvent event) {
		String itemname = (String) event.window.getValue("listItems");
		String oper = (String) event.window.getValue("oper");
		if (oper == null)
			oper = "0";
		int iOper = Integer.parseInt(oper);
		long start = System.currentTimeMillis();
		int cnt = sv.performOper("test", itemname, Selection.toOper(iOper));
		long end = System.currentTimeMillis();
		event.window.setValue("exeTime",  end - start);
		event.window.setValue("size", Integer.toString(cnt));
		String newItem = sOper[iOper] + " " + itemname;
		GuiList list = (GuiList)event.window.getGuiMember("listHistory");
		list.addItem(newItem);
		if (Convert.toBoolean(event.window.getValue("show"))) {
			showResult(event);
		}
	}

	public void showResult(GuiUserEvent event) {
		ExpressionResult erg = sv.getResultSet("test");
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < erg.objekts.length; i++) {
			b.append(Long.toString(erg.objekts[i]));
			b.append(" ");
		}
		event.window.setValue("memResult", b.toString());
	}

	public void showResultChanged(GuiUserEvent event) {
		if (((GuiChangeEvent) event).bValue) {
			try {
				showResult(event);
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
				GuiUtil.showMessage(event.window, "Error", "Error", ex.getMessage());
			}
		} else {
			event.window.setValue("memResult", null);
		}
	}

	public void reset(GuiUserEvent event) {
		sv.endSession("test");
		GuiList list = (GuiList)event.window.getGuiMember("listHistory");
		list.setItems((Vector)null);
		event.window.setValue("size", null);
		event.window.setValue("memResult", null);
		this.startTrans(event);
	}

	public void editObjects(GuiUserEvent event) {
		GuiWindow win;
		try {
			win = GuiFactory.getInstance().createWindow("gui/Object.xml");
			win.setController(this);
			win.show();
		} catch (GDLParseException e) {
			e.printStackTrace();
		}
	}

	public void editExpr(GuiUserEvent event) {
		GuiWindow win;
		try {
			win = GuiFactory.getInstance().createWindow("gui/Expression.xml");
			win.setController(this);
			win.show();
		} catch (GDLParseException ex) {
			logger.error(ex.getMessage(), ex);
			GuiUtil.showMessage(event.window, "Error", "Error", ex.getMessage());
		}
	}

	public void getOtherItems(GuiUserEvent event) {
		ArrayList<String> items = sv.getOtherItems("test");
		for (int i = 0; i < items.size(); i++) {
			System.out.println(items.get(i));
		}
	}

	public void validate(GuiUserEvent event) {
		event.window.getComponent().setCursor(new Cursor(Cursor.WAIT_CURSOR));
		sv.validate();
		event.window.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public void spider(GuiUserEvent event) {
		sv.startSpider();
	}
   public static boolean yesNoMessage(GuiWindow parent, String title,
         String message) {
      Component pc = null;
      if (parent != null) {
         pc = parent.getComponent();
      }
      if (JOptionPane.showConfirmDialog(pc, message, title,
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
         return true;
      }
      return false;
   }
   public void resetAllSessions(GuiUserEvent event) {
   	sv.resetAllSessions();
   	this.startTrans(event);
   }
   // Object
   public void readObject(GuiUserEvent event) {
   	long oid = Convert.toLong(event.window.getValue("oid"));
   	String[] items = sv.getObjectItems(oid);
   	/*
   	for (int i = 0; i < items.length; i++) {
   		System.out.println(items[i]);
   	}
   	*/
   	GuiList list = (GuiList)event.window.getGuiMember("listItems");
   	list.setItems(items);
   }
   public void createObject(GuiUserEvent event) {
   	long oid = Convert.toLong(event.window.getValue("oid"));
   	sv.createObject(oid);
   }
   public void deleteObject(GuiUserEvent event) {
   	long oid = Convert.toLong(event.window.getValue("oid"));
   	sv.deleteObject(oid);
   }
   public void setOid(GuiUserEvent event) {
   	long oid = Convert.toLong(event.window.getValue("oid"));
   	System.out.println(oid);
   }
   // Items
	public void findItems(GuiUserEvent event) {
		String item = (String) event.window.getValue("textItem");
		String[] items = sv.findItems(item);
		GuiList mem = (GuiList) event.window.getGuiMember("listItems");
		mem.setItems(items);
		if (items.length == 1) {
			mem.setValue(items[0]);
		}
	}

   public void createItem(GuiUserEvent event) {
   	String item = (String)event.window.getValue("textItem");
   	sv.createItem(item);
   }
   public void deleteItem(GuiUserEvent event) {
   	String item = (String)event.window.getValue("textItem");
   	sv.deleteItem(item);
   }
   public void itemChanged(GuiUserEvent event) {
   	//String item = (String)event.value;
   }
   // Expression
   public void ee_execute(GuiUserEvent event) {
   	String exp = (String)event.window.getValue("memExpr");
   	long start = System.currentTimeMillis();
   	ExpressionResult res = sv.execute(exp);
   	event.window.setValue("size", res.resultsetSize);
   	long end2 = System.currentTimeMillis();
   	long duraTotal = end2-start; 
   	String sd = res.duraDB1 + "/" + res.duraAlg + "/" + res.duraDB2 + " " + duraTotal;
   	event.window.setValue("exeTime", sd);
   	this.displayPage(event, res.firstPage, res.trace);
   }
   
   public void findOther(GuiUserEvent event) {
   	ArrayList<String> al = sv.getOtherItems(sessionName);
   	StringBuilder buff = new StringBuilder();
   	for(String s:al) {
   		buff.append(s);
   		buff.append('\n');
   	}
   	event.window.setValue("memOther", buff.toString());
   }
//   public void showObjects(GuiUserEvent event) {
//   	ExpressionResult res = sv.getResultSet(sessionName);
//   	String objekts = sv.getObjekts(res.objekts);
//   	event.window.setValue("memOther", objekts);
//   }
   
   public void goBack(GuiUserEvent event) {
   	JDataSet ds = sv.getPrevPage(sessionName);
   	this.displayPage(event, ds, null);
   }
   public void goMore(GuiUserEvent event) {
   	JDataSet ds = sv.getNextPage(sessionName);   	
   	this.displayPage(event, ds, null);
   }
   
   private void displayPage(GuiUserEvent event, JDataSet ds, String trace) {
   	String s = ExpressionResult.pageToString(ds);
   	if (trace != null) {
   		s += "\nResultSet Trace:\n" + trace; 
   	}
   	event.window.setValue("memResult", s);
   }
   
//   public void editObjects(GuiUserEvent event) {
//   	win = GuiFactory.getInstance().createWindow("Object.xml");
//   	win.show();
//   }
//   public void getOtherItems(GuiUserEvent event) {
//   	items = sv.getOtherItems("test");
//   	for (int i = 0; i < items.size(); i++) {
//   		System.out.println(items.get(i));
//   	}
//   }
}
