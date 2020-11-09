/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rems;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 *
 * @author richard.adjei-mensah
 */
public class MntrUserAlertsNtRnngfunc extends Thread {

    private Thread t;
    private String threadName;

    MntrUserAlertsNtRnngfunc(String name) {
        threadName = name;
        System.out.println("Creating " + threadName);
    }

    @Override
    public void run() {

        try {
            do {
                //Get all rquest runs not running
                //Launch appropriate process runner
                System.out.println("Inside MntrUserAlertsNtRnngfunc");
                Program.checkNClosePrgrm();
                ResultSet dtst = Global.get_UserAlertsNtRnng();
                long mxConns = 0;
                long curCons = 0;
                mxConns = Global.getMxAllwdDBConns();
                dtst.last();
                int ttlRws = dtst.getRow();
                dtst.beforeFirst();

                for (int i = 0; i < ttlRws; i++) {
                    dtst.next();
                    long rptid = Long.parseLong(dtst.getString(2));
                    long rptrnid = Long.parseLong(dtst.getString(1));
                    long rptrnnrid = Long.parseLong(dtst.getString(3));
                    String rptRnnrNm = Global.getGnrlRecNm("rpt.rpt_reports", "report_id", "process_runner", rptid);
                    String rnnrPrcsFile = Global.getGnrlRecNm("rpt.rpt_prcss_rnnrs", "rnnr_name", "executbl_file_nm", rptRnnrNm);
                    if (rptRnnrNm == "") {
                        rptRnnrNm = "Standard Process Runner";
                    }
                    if (rnnrPrcsFile == "") {
                        rnnrPrcsFile = "/bin/REMSProcessRunner.jar";
                    }

                    rnnrPrcsFile = rnnrPrcsFile.replace("/bin/", "").replace("\\bin\\", "");

                    if (Global.doesLstRnTmExcdIntvl(rptid, "1 second", rptrnid) == true) {
                        Global.updatePrcsRnnrCmd(rptRnnrNm, "0", rptrnnrid);
                        Global.updateRptRnStopCmd(rptrnid, "0");
                        String[] args = {"\"" + Global.Hostnme + "\"",
                            Global.Portnum,
                            "\"" + Global.Uname + "\"",
                            "\"" + Global.Pswd + "\"",
                            "\"" + Global.Dbase + "\"",
                            "\"" + rptRnnrNm + "\"",
                            String.valueOf(rptrnid),
                            "\"" + Global.appStatPath + "\"",
                            "WEB",
                            "\"" + Global.dataBasDir + "\"",
                            "\"" + Global.AppUrl + "\""};

                        if (rnnrPrcsFile.contains(".jar")) {
                            System.out.println(("java -jar " + Global.appStatPath + "/" + rnnrPrcsFile + " " + String.join(" ", args)).replace(Global.Pswd, "**************"));
                            //Runtime runTime = Runtime.getRuntime();
                            //Process process = runTime.exec("java -jar " + Global.appStatPath + "/" + rnnrPrcsFile + " " + String.join(" ", args));
                            String batchFilnm = Global.appStatPath + "/" + "MntrUserAlertsNtRnngfunc_" + String.valueOf(rptrnid) + ".sh";
                            PrintWriter fileWriter;
                            fileWriter = new PrintWriter(batchFilnm, "UTF-8");
                            StringBuilder strSB = new StringBuilder("#!/bin/sh").append(System.getProperty("line.separator"));
                            strSB.append("echo \"import pty; pty.spawn('/bin/bash')\" > /tmp/asdf.py").append(System.getProperty("line.separator"));
                            strSB.append("java -jar ").append(Global.appStatPath).append("/").append(rnnrPrcsFile).append(" ").append(String.join(" ", args));
                            fileWriter.println(strSB);
                            fileWriter.close();
                            //Runtime.getRuntime().exec("sed -i.bak 's/\r//g' " + batchFilnm);
                            Runtime.getRuntime().exec("chmod +x " + batchFilnm);
                            Runtime.getRuntime().exec("chmod 7777 -R /opt");
                            try {
                                ProcessBuilder pb = new ProcessBuilder(batchFilnm);
                                pb.redirectErrorStream(true);
                                Process p = pb.start();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                String line = null;
                                while ((line = reader.readLine()) != null) {
                                    System.out.print("#");//line
                                }
                                boolean success = (new java.io.File(batchFilnm)).delete();
                            } catch (IOException ex) {
                                //write to log file
                                Global.errorLog = ex.getMessage() + "\r\n" + Arrays.toString(ex.getStackTrace()) + "\r\n";
                                System.out.println(Global.errorLog);
                                Global.writeToLog();
                            }
                        }
                    }
                    do {
                        curCons = Global.getCurDBConns();
                        Global.errorLog = "Inside Running of User Initiated Alerts=> Current Connections: " + curCons + " Max Connections: " + mxConns;
                        System.out.println(Global.errorLog);
                        Global.writeToLog();
                        Program.checkNClosePrgrm();
                        Thread.sleep(50);
                        if (curCons >= mxConns) {
                            Thread.sleep(50000);
                        }
                    } while (curCons >= mxConns);
                }
                Thread.sleep(10000);
                long prgmID = Global.getGnrlRecID("rpt.rpt_prcss_rnnrs", "rnnr_name", "prcss_rnnr_id", Program.runnerName);
                Program.updatePrgrm(prgmID);
            } while (true);
        } catch (SQLException ex) {
            //write to log file
            Global.errorLog = ex.getMessage() + "\r\n" + Arrays.toString(ex.getStackTrace()) + "\r\n";
            System.out.println(Global.errorLog);
            Global.writeToLog();
            if (Program.thread7.isAlive()) {
                Program.thread7.interrupt();
            }
        } catch (NumberFormatException ex) {
            //write to log file
            Global.errorLog = ex.getMessage() + "\r\n" + Arrays.toString(ex.getStackTrace()) + "\r\n";
            System.out.println(Global.errorLog);
            Global.writeToLog();
            if (Program.thread7.isAlive()) {
                Program.thread7.interrupt();
            }
        } catch (IOException ex) {
            //write to log file
            Global.errorLog = ex.getMessage() + "\r\n" + Arrays.toString(ex.getStackTrace()) + "\r\n";
            System.out.println(Global.errorLog);
            Global.writeToLog();
            if (Program.thread7.isAlive()) {
                Program.thread7.interrupt();
            }
        } catch (InterruptedException ex) {
            //write to log file
            Global.errorLog = ex.getMessage() + "\r\n" + Arrays.toString(ex.getStackTrace()) + "\r\n";
            System.out.println(Global.errorLog);
            Global.writeToLog();
            if (Program.thread7.isAlive()) {
                Program.thread7.interrupt();
            }
        } catch (Exception ex) {
            //write to log file
            Global.errorLog = ex.getMessage() + "\r\n" + Arrays.toString(ex.getStackTrace()) + "\r\n";
            System.out.println(Global.errorLog);
            Global.writeToLog();
            if (Program.thread7.isAlive()) {
                Program.thread7.interrupt();
            }
        } finally {
        }
    }

    @Override
    public void start() {
        System.out.println("Starting " + threadName);
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }

}
