package pulse;

import java.sql.*;
import java.util.concurrent.*;

import org.hyperic.sigar.*;

public class Monitor {
	
	private static Sigar sigar = new Sigar();
	private static final ScheduledExecutorService stpe = 
			Executors.newSingleThreadScheduledExecutor();
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		Connection conx = Dber.MySQLer();
		specer(conx);
	}
	
	public static void specer(final Connection cnx) {
		final Runnable sampler = new Runnable() {
			public void run() {
				CpuPerc[] cpupercs = null; 
				Mem mem = null;
				NetInterfaceStat net = null;
				FileSystemUsage filesys = null;
				
				try {
		            cpupercs = sigar.getCpuPercList();
		            mem = sigar.getMem();
		            net = sigar.getNetInterfaceStat("wlan0");
		            filesys = sigar.getFileSystemUsage("/");
				} catch (SigarException se) {
		        	se.printStackTrace();
		        }
				
				String time = GetTimeStamp();
				final double divisor = (1024*1000000);
				double[] ess = {0,0,0,0,0};
				double j = 0.0;
				
				for (CpuPerc cpuperc : cpupercs ) j = j + cpuperc.getCombined();
			    ess[0] = j / (cpupercs.length);
				ess[1] = (double) mem.getUsed() / mem.getTotal();
				ess[2] = (double) filesys.getUsed() / ((filesys.getAvail() + filesys.getUsed()));
				ess[3] = net.getRxBytes() / divisor;
				ess[4] = net.getTxBytes() / divisor;
				
				Dber.Depositor(time, ess, cnx);
				for (double a : ess) {
					System.out.println(a);
				}
			}
		};
	
		final ScheduledFuture<?> sf = stpe.scheduleAtFixedRate(
				sampler, 0, 1, TimeUnit.SECONDS);
		
		stpe.schedule(new Runnable() {
            public void run() {
            	sf.cancel(true); 
            	stpe.shutdown();
            	System.out.println(stpe.isShutdown());
            }
        }, 60, TimeUnit.SECONDS);
	}
	
	public static String GetTimeStamp() {
		java.util.Date date= new java.util.Date();
		String timeStamp = "\"" + new Timestamp(date.getTime()) + "\"";
		return timeStamp;
	}
}













