package de.pk86.bf;

import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.junit.Test;

import de.guibuilder.framework.GuiUtil;
import de.pk86.bf.client.ServiceFactory;

public class ThreadTest {
   /**
    * Logger for this class
    */
   private static final Logger logger = Logger.getLogger(ThreadTest.class);

   private int okCounter = 0;
   private Object okSema = "x"; // okCounter++ nicht atomar!!
   private int errorCounter = 0;
   private Object errSema = "y";
   private int fin;
   private String error;
   private static boolean gui = true;

   private long startTime;
   private long endTime;
   //private static ObjectItemServiceIF srv = ServiceFactory.getSpringService("10.8.0.1:1098");
   //private static ObjectItemServiceIF srv = ServiceFactory.getDirectService();
   //private static ObjectItemSOAPService srv = ServiceFactory.getSOAPService("http://10.8.0.1:8080/bitdemo/soap?wsdl");
   //private static ObjectItemSOAPService srv = ServiceFactory.getSOAPService("http://pk86.de/bitdemo/soap?wsdl");
   //private static ObjectItemSOAPService srv = ServiceFactory.getSOAPService("http://localhost:8080/bitset/soap?wsdl");
   private static ObjectItemSOAPService srv = ServiceFactory.getSOAPService("file:///home/peter/workspace/bitset/soap.wsdl");

   public static void main(String[] args) {
   	ThreadTest me = new ThreadTest();
   	gui = false;
   	me.testThread();
   }
   public ThreadTest() {
   	
   }
   
   private void addError(String errorText) {
      if (error == null) {
         error = errorText + "\n";
      } else {
         error += errorText + "\n";
      }
   }

   @Test public void testThread() {
		if (gui && !GuiUtil.yesNoMessage(null, "Return to continue", "Start")) {
			return;
		}
      int maxThreads = 300; // 300
      int runCounts = 5; // 20 
      //int totalRuns = runCounts * maxThreads;
      int totalRuns = maxThreads;
      
      okCounter = 0;
      errorCounter = 0;

      this.startTime = System.currentTimeMillis();
      int sum = 0;
      for (int i = 0; i < maxThreads; i++) {
         Worker w = new Worker(i, runCounts);
         new Thread(w).start();
         try {
            Thread.sleep(1);
         } catch (InterruptedException e) {}
      }
      do {
         try {
            Thread.sleep(200);
         } catch (InterruptedException e) {

         }
         System.err.println("--> RunCount: " + (okCounter + errorCounter) + " Must run: "
               + totalRuns);
      } while ((okCounter + errorCounter) < totalRuns);
      this.endTime = System.currentTimeMillis();

      if (errorCounter != 0) {
         System.err.println("Errors: "+errorCounter);
         logger.error(error);
         fail(error);
      }
      double duration = endTime - startTime;
      double durSec = duration/1000;
      System.out.println("Dauer: " + durSec);
      System.out.println("Je Sekunde: " + totalRuns * runCounts/durSec);
      int xxx = 0; xxx++;
		if (gui && !GuiUtil.yesNoMessage(null, "Return to continue", "End")) {
			return;
		}

   }

   class Worker implements Runnable {
      private int runCount;
      private int workerNR;
      private String name;
      
      Worker(int workerNR, int runCount) {
         this.workerNR = workerNR;
         this.runCount = runCount;
         name = "Worker " + workerNR;
      }
      
      public void run() {   		
         logger.debug("Worker started: "+name);
         long start = System.currentTimeMillis();
         for (int i = 0; i < runCount; i++) {
         	// ACHTUNG! Logger ausschalten!
         	// ACHTUNG! Hier nur Items befragen, die es in der DB auch wirklich gibt! Dauert sonst!
        	 try {
//        		 String h = srv.sayHello();
//        		 String e = srv.echo("äöüÄÖÜß");
      		int sessionId1 = srv.createSession("w | m"); 
      		srv.endSession(sessionId1);
      		int sessionId2 = srv.createSession("hans berlin straße"); 
      		srv.endSession(sessionId2);
      		int sessionId3= srv.createSession("(hans | maria) berlin m"); 
      		srv.endSession(sessionId3);
      		//boolean b = srv.hasItem("Berlin");
      		//int len = x.length;
      		int xxx = 0;
        	 } catch (Exception ex) {
        		 System.out.println(ex.getMessage());
        	 
        	 }
         } // End For
         synchronized(okSema) {
         	okCounter++;
         }
         long dauer = System.currentTimeMillis() - start;
         fin++;
         System.out.println(name+" "+Thread.currentThread()+" Dauer: "+dauer + " / finished: "+fin);
      }
   }

}

