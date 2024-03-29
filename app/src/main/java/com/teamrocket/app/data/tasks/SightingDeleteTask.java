package com.teamrocket.app.data.tasks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.teamrocket.app.BTApplication;
import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.model.BirdSighting;

import java.util.Calendar;

public class SightingDeleteTask extends Worker {

    public SightingDeleteTask(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        return new Task().delete(getApplicationContext());
    }

    public static class Task {

        public Result delete(Context context) {
            String selectedTime = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("autoDeleteSightings", "");

            if (selectedTime.isEmpty() || selectedTime.equals("0")) {
                return Result.success();
            }

            int months = Integer.parseInt(selectedTime);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -months);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            BirdSightingDao dao = ((BTApplication) context).getBirdSightingDao();
            dao.deleteBefore(calendar.getTimeInMillis());

            return Result.success();
        }

        public Result delete2(Context context) {
            String selectedTime = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("autoDeleteSightings", "");

            if (selectedTime.isEmpty() || selectedTime.equals("0")) {
                return Result.success();
            }

            int months = Integer.parseInt(selectedTime);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -months);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            BirdSightingDao dao = ((BTApplication) context).getBirdSightingDao();

            for (BirdSighting sighting : dao.getAll()) {
                if (sighting.getTime() < calendar.getTimeInMillis()) {
                    dao.delete(sighting);
                }
            }

            return Result.success();
        }
    }
}
