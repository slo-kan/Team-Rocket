package com.teamrocket.app.data.tasks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.teamrocket.app.BTApplication;
import com.teamrocket.app.data.db.BirdSightingDao;

import java.util.Calendar;

public class SightingDeleteTask extends Worker {

    public SightingDeleteTask(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String selectedTime = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString("autoDelete", "");

        if (selectedTime.isEmpty() || selectedTime.equals("Never")) {
            return Result.success();
        }

        int months = selectedTime.equals("After 1 month") ? 1 :
                selectedTime.equals("After 3 months") ? 3 : 6;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -months);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        BirdSightingDao dao = ((BTApplication) getApplicationContext()).getBirdSightingDao();
        dao.deleteBefore(calendar.getTimeInMillis());

        return Result.success();
    }
}
