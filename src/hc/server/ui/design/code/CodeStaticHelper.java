package hc.server.ui.design.code;

import hc.server.ui.Dialog;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class CodeStaticHelper {
	protected final static String[] J2SE_CLASS_SET = convertArray(J2SEClassList.getList());
	protected final static int J2SE_CLASS_SET_SIZE = J2SE_CLASS_SET.length;
	
	protected final static String[] HC_CLASS_SET = convertArray(getHCAPIList());
	protected final static int HC_CLASS_SET_SIZE = HC_CLASS_SET.length;
	
	protected final static HashMap<Class, Vector<String>> deprecatedMethodsAndFields = getDeprecated();
	
	public static final void doNothing(){
	}
	
	private final static HashMap<Class, Vector<String>> getDeprecated(){
		final HashMap<Class, Vector<String>> out = new HashMap<Class, Vector<String>>(20);

		{
			final Vector<String> fields = new Vector<String>(2);
			fields.add("goExternalURLWhenInSession(String, boolean)");
			fields.add("getIntervalMSForRestart()");
			
			out.put(ProjectContext.class, fields);
		}
		
		{
			final Vector<String> methods = new Vector<String>(1);
			methods.add("goExternalURL(String, boolean)");
			
			out.put(Dialog.class, methods);
		}
		
		{
			final Vector<String> methods = new Vector<String>(1);
			methods.add("goExternalURL(String, boolean)");
			
			out.put(Mlet.class, methods);
		}
		
		return out;
	}
	
	protected final static String[] convertArray(final List<String> list){
		final int size = list.size();
		final String[] out = new String[size];
		
		for (int i = 0; i < size; i++) {
			out[i] = list.get(i);
		}
		
		return out;
	}

//	private final static String[] buildPackageSet(final String[] set, final int size){
//		final ArrayList<String> pkg = new ArrayList<String>();
//		for (int i = 0; i < size; i++) {
//			final String item = set[i];
//			final String[] splits = item.split("\\.");
//			final int splitSize = splits.length - 1;
//			
//			String appendPkg = "";
//			for (int j = 0; j < splitSize; j++) {
//				if(appendPkg.length() > 0){
//					appendPkg += ".";
//				}
//				appendPkg += splits[j];
//				if(pkg.contains(appendPkg) == false){
//					pkg.add(appendPkg);
//				}
//			}
//		}
//		
//		final int pkgSize = pkg.size();
//		final String[] out = new String[pkgSize];
//		for (int i = 0; i < pkgSize; i++) {
//			out[i] = pkg.get(i);
//		}
//		
//		return out;
//	}

	private static java.util.ArrayList<String> getHCAPIList() {
		final ArrayList<String> out = new ArrayList<String>();
		{
			//重要：请执行一遍，并刷新J2SEClassList.java
//			apiList.add("hc.server.msb.WiFiAccount");
			
			out.add("hc.core.util.CtrlKey");
			out.add("hc.core.util.IEncrypter");
			out.add("hc.server.msb.Converter");
			out.add("hc.server.msb.Device");
			out.add("hc.server.msb.DeviceCompatibleDescription");
			out.add("hc.server.msb.Message");
			out.add("hc.server.msb.Robot");
			out.add("hc.server.msb.RobotEvent");
			out.add("hc.server.msb.RobotListener");
			out.add("hc.server.msb.AnalysableRobotParameter");
            out.add("hc.server.ui.ClientSession");
			out.add("hc.server.ui.CtrlResponse");
			out.add("hc.server.ui.Dialog");
			out.add("hc.server.ui.HTMLMlet");
			out.add("hc.server.ui.MenuItem");
			out.add("hc.server.ui.Mlet");
			out.add("hc.server.ui.ProjectContext");
			out.add("hc.server.ui.ScriptPanel");
			out.add("hc.server.util.SystemEventListener");
			out.add("hc.server.util.JavaLangSystemAgent");
			out.add("hc.server.util.Scheduler");
			out.add("hc.server.util.scheduler.AnnualJobCalendar");
			out.add("hc.server.util.scheduler.CronExcludeJobCalendar");
			out.add("hc.server.util.scheduler.DailyJobCalendar");
			out.add("hc.server.util.scheduler.HolidayJobCalendar");
			out.add("hc.server.util.scheduler.MonthlyJobCalendar");
			out.add("hc.server.util.scheduler.WeeklyJobCalendar");
			out.add("hc.server.util.Assistant");
			out.add("hc.server.util.json.JSONArray");
			out.add("hc.server.util.json.JSONException");
			out.add("hc.server.util.json.JSONML");
			out.add("hc.server.util.json.JSONObject");
			out.add("hc.server.util.json.JSONPointer");
			out.add("hc.server.util.json.JSONPointerException");
			out.add("hc.server.util.json.JSONString");
			out.add("hc.server.util.json.JSONTokener");
			out.add("hc.server.util.json.JSONXML");
			out.add("hc.server.util.json.JSONXMLTokener");
			out.add("hc.server.util.IDEUtil");
			out.add("hc.server.util.JavaString");
			out.add("hc.server.util.VoiceCommand");
			
			Collections.sort(out);
		}
		return out;
	}

}
