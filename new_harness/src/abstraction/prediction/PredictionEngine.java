package abstraction.prediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public class PredictionEngine {
	
	/*this thread is used to populate the main memory cache. It is triggered after each request*/
	public class PredictionTask implements Runnable {
		public synchronized void run() {
			System.out.println();
		}
	}
}
