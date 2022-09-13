package org.torproject.android.service.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class PrefsWeeklyWorker extends Worker {

    public PrefsWeeklyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        Prefs.resetSnowflakesServedWeekly();

        return Result.success();
    }
}
