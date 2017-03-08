package hc.server.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VoiceCommand {
	public final String command;
	
	public VoiceCommand(final String cmd){
		this.command = cmd;
	}
	
	public final boolean equalsIgnoreCase(final String m){
		return command.equalsIgnoreCase(m);
	}
	
	private static final Pattern singleChars = Pattern.compile("^(\\w{1}\\s{1})+\\w{1}$");//将[H t m l]转换成[Html]
	
	public static String format(final String cmd){
		final Matcher m = singleChars.matcher(cmd);
		if(m.find()){
			return cmd.replace(" ", "");
		}else{
			return cmd;
		}
	}
}
