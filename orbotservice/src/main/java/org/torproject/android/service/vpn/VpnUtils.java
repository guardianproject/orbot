package org.torproject.android.service.vpn;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static java.lang.Runtime.getRuntime;
import static org.torproject.android.service.vpn.VpnPrefs.PREF_TOR_SHARED_PREFS;

public class VpnUtils {

    public static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(PREF_TOR_SHARED_PREFS,
                Context.MODE_MULTI_PROCESS);
    }


    public static int findProcessId(String command) throws IOException {

        String[] cmds = {"ps -ef","ps -A","toolbox ps"};

        for (int i = 0; i < cmds.length;i++) {
            Process procPs = getRuntime().exec(cmds[i]);

            BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("PID") && line.contains(command)) {
                    String[] lineParts = line.split("\\s+");
                    try {
                        return Integer.parseInt(lineParts[1]); //for most devices it is the second
                    } catch (NumberFormatException e) {
                        return Integer.parseInt(lineParts[0]); //but for samsungs it is the first
                    } finally {
                        try {
                            procPs.destroy();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        return -1;
    }

    public static void killProcess(File fileProcBin) throws Exception {
        killProcess(fileProcBin, "-9"); // this is -KILL
    }

    public static int killProcess(File fileProcBin, String signal) throws Exception {

        int procId = -1;
        int killAttempts = 0;

        while ((procId = findProcessId(fileProcBin.getName())) != -1) {
            killAttempts++;
            String pidString = String.valueOf(procId);
            boolean itBeDead = killProcess(pidString, signal);

            if (!itBeDead) {

                String[] cmds = {"", "busybox ", "toolbox "};

                for (int i = 0; i < cmds.length; i++) {

                    Process proc = null;

                    try {
                        proc = getRuntime().exec(cmds[i] + "killall " + signal + " " + fileProcBin.getName
                                ());
                        int exitValue = proc.waitFor();
                        if (exitValue == 0)
                            break;

                    } catch (IOException ioe) {
                    }
                    try {
                        proc = getRuntime().exec(cmds[i] + "killall " + signal + " " + fileProcBin.getCanonicalPath());
                        int exitValue = proc.waitFor();
                        if (exitValue == 0)
                            break;

                    } catch (IOException ioe) {
                    }
                }


                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignored
                }

                           }

            if (killAttempts > 4)
                throw new Exception("Cannot kill: " + fileProcBin.getAbsolutePath());
        }

        return procId;
    }

    public static boolean killProcess(String pidString, String signal) throws Exception {

        String[] cmds = {"","toolbox ","busybox "};

        for (int i = 0; i < cmds.length;i++) {
            try {
                Process proc = getRuntime().exec(cmds[i] + "kill " + signal + " " + pidString);
                int exitVal = proc.waitFor();
                List<String> lineErrors = IOUtils.readLines(proc.getErrorStream());
                List<String> lineInputs = IOUtils.readLines(proc.getInputStream());

                if (exitVal != 0)
                {
                    Log.d("Orbot.killProcess","exit=" + exitVal);
                    for (String line: lineErrors)
                        Log.d("Orbot.killProcess",line);

                    for (String line: lineInputs)
                        Log.d("Orbot.killProcess",line);

                }
                else
                {
                    //it worked, let's exit
                    return true;
                }


            } catch (IOException ioe) {
                Log.e("Orbot.killprcess","error killing process: " + pidString,ioe);
            }
        }

        return false;
    }
}
