package com.uhsl.flowmessage.flowmessagev2.utils;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;

import com.imgtec.flow.client.core.NetworkException;
import com.uhsl.flowmessage.flowmessagev2.flow.FlowController;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Created by Marcus on 22/02/2016.
 */

public class BackgroundTask {

    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * A background task, calls the method onBackGroundTaskResult() on the listener class (@callback)
     * @param asyncCall The task to call
     * @param callback The callback listener class
     * @param task ID of the task
     * @param <RESULT> the result type, must match the result type in @asyncCall
     */
    public static <RESULT> void call(final AsyncCall<RESULT> asyncCall, final Callback<RESULT> callback, final int task) {

       final FutureTask<RESULT> futureTask = new FutureTask<RESULT>(new Callable<RESULT>() {
           @Override
           public RESULT call() throws Exception {
               if (Looper.myLooper() == null)
                   Looper.prepare();

               return asyncCall.call();
           }
       });
       executorService.execute(futureTask);

       new Thread(new Runnable() {
           @Override
           public void run() {
               RESULT result = null;
               try {
                   result = futureTask.get();
                   callback.onBackGroundTaskResult(result, task);

               } catch (ExecutionException e) {
                   System.out.println("Execution Exception: " + e.toString() );
                   //TODO: handle these
               } catch (Exception e) {
                   System.out.println("other Exception: " + e.toString());
                   //TODO: and these
               }

           }
       }).start();

    }

    /**
     * Runs a background async task with no result
     * @param asyncRun The task to run
     */
    public static void run(final AsyncRun asyncRun){
        Runnable run = new Runnable() {
            @Override
            public void run() {
                if (Looper.myLooper() == null)
                    Looper.prepare();

                try {
                    asyncRun.run();
                } catch (Exception e) {
                    System.out.println("Threaded Exception: " + e.toString() + " -> " + e.getMessage());
                    //TODO: handle this too
                }
            }
        };
        executorService.execute(run);

    }

    /**
     * Interface for BackgroundTask.call listeners
     * @param <RESULT> result type
     */
    public interface Callback<RESULT> {
        /**
         * Callback method, called on the listener class
         * @param result Result of the async execution
         * @param task Task ID
         */
        void onBackGroundTaskResult(RESULT result, int task);
    }


}
