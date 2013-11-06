package de.pk86.bf;

import static org.junit.Assert.*;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.Test;

import de.pk86.bf.client.ServiceFactory;

//import de.pkjs.pltest.ConnectionPoolTest;

public class ThreadTest /*extends TestCase*/ {
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

   private long startTime;
   private long endTime;
   //private static ObjectItemServiceIF srv = ServiceFactory.getSpringService("10.8.0.1");
   private static ObjectItemServiceIF srv = ServiceFactory.getDirectService();

   public ThreadTest() {
   }
   
   public void setUp() throws Exception {
      logger.info("setUp");
      //srv = ServiceFactory.getSpringService("10.8.0.1");
   }

   public void tearDown() throws Exception {
      logger.info("tearDown");
      //p.shutdown();
      // p.reset();
   }

   private void addError(String errorText) {
      if (error == null) {
         error = errorText + "\n";
      } else {
         error += errorText + "\n";
      }
   }

   @Test public void testThread() {
      int maxThreads = 400;
      int runCounts = 100; 
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
   		//##ObjectItemService srv = new ObjectItemService();
         logger.debug("Worker started: "+name);
         long start = System.currentTimeMillis();
         for (int i = 0; i < runCount; i++) {
         	// ACHTUNG! Logger ausschalten!
        	 try {
      		ExpressionResult res1 = srv.execute("w | m"); 
      		srv.endSession(res1.sessionId);
      		ExpressionResult res2 = srv.execute("hans berlin münchen"); 
      		srv.endSession(res2.sessionId);
      		ExpressionResult res3 = srv.execute("(hans | maria) hamburg köln"); 
      		srv.endSession(res3.sessionId);
      		boolean b = srv.hasItem("Berlin"); // Postgres ? [7713]; MaxDB: 10272 [11621]
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
   
   public static void main(String[] argv) {
      //TestRunner.run(ConnectionPoolTest.class);
   }

}

