package hc.server.relay;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.HCURL;
import hc.core.util.LogManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

public class LineOnManager {
	static final Vector<DataLineOn> vector = new Vector<DataLineOn>();
	public static final Comparator<DataLineOn> comparator = new Comparator<DataLineOn>() {
		@Override
		public int compare(final DataLineOn o1, final DataLineOn o2) {
			final int eqServerNum = o1.serverNum - o2.serverNum;
			if(eqServerNum == 0){
				return o1.getLenid() - o2.getLenid();
			}else{
				return eqServerNum;
			}
		}
	};

	private static String printServerInfo(final String currID, final String hideToken){
		//WHERE id = "' . $_GET['id'] . '";');

		final int size = vector.size();
		for (int i = 0; i < size; i++) {
			final DataLineOn dlo = vector.elementAt(i);
			//$ht['ip'] , ';' , $ht['port'] , ';' , $ht['nattype'] , ';' , $ht['upnpip'] , ';' , 
			//$ht['upnpport'] , ';' , $ht['relayip'] , ';' , $ht['relayport'];
			if(dlo.id.equals(currID)){
				if(dlo.hideIP.equals(IConstant.FALSE)
						|| dlo.hideToken.equals(hideToken)){
					//必须要hideToken也吻合
					final StringBuffer sb = new StringBuffer("");
					
					sb.append(dlo.ip);
					sb.append(';');
					sb.append(dlo.port);
					sb.append(';');
					sb.append(dlo.nattype);
					sb.append(';');
					sb.append(dlo.upnpip);
					sb.append(';');
					sb.append(dlo.upnpport);
					sb.append(';');
					sb.append(dlo.relayip);
					sb.append(';');
					sb.append(dlo.relayport);
					return sb.toString();
				}
				break;
			}
		}
		//不用输出任何内容
		return "";
	}

	private static String printNewRelayServers(){
		//查询可供使用的中继
		final StringBuffer obs = new StringBuffer("");
		
		//SELECT upnpip,upnpport,relayip,relayport 
		//FROM `lineon` WHERE nattype = 100 ORDER BY serverNum ASC, lenid DESC LIMIT 20;');
		Collections.sort(vector, comparator);
		
		final Iterator<DataLineOn> it = vector.iterator();
		int recordLimit = 0;
		while(it.hasNext()){
			final DataLineOn dlo = it.next();
			if(dlo.nattype.equals("100") == false){
				continue;
			}
			
			//$obs = $obs . $ht['upnpip'] . ';' . $ht['upnpport'] . ';1;'.$ht['relayip'] . ';' 
			//. $ht['relayport'] . ';1;';
			obs.append(dlo.upnpip);
			obs.append(';');
			obs.append(dlo.upnpport);
			obs.append(";1;");
			obs.append(dlo.relayip);
			obs.append(';');
			obs.append(dlo.relayport);
			obs.append(";1;");
			
			if(++recordLimit >= 20){
				break;
			}
		}
		//return obs.toString();
		final String out = obs.toString();
//		if(L.isLogInRelay) {
//			LogManager.log(" LineOnManager printNewRelayServers : " + out);
//		}
		return out;
	}

	private static void removeUnalive(final String currID){
//		if($_GET['id'] == 'root'){
//			mysql_query('DELETE FROM `lineon` WHERE alive < date_sub(now(), INTERVAL 26 hour);');//两小时未刷新
//		}
		if(currID.equals("root")){
			//由root来触发，而非处理root
			final int size = vector.size();
			final long minMS = 1000 * 60 * 60 * 26;//26 hour
			final long borderMS = System.currentTimeMillis() - minMS;
			for (int i = size - 1; i >= 0 ; i--) {
				final DataLineOn dlo = vector.elementAt(i);
				if(dlo.alive < borderMS){
					vector.remove(i);
					if(L.isLogInRelay) {
						LogManager.log(" LineOnManager REMOVE UNALIVE : " + dlo.id);
					}
				}
			}
		}
	}
	
	public static String process(final HCURL hcurl){
		final String f = hcurl.getValueofPara("f");
		if(f.equals("lineon")){
			final String para_ID = hcurl.getValueofPara("id");
			final String para_TOKEN = hcurl.getValueofPara("token");

			//WHERE id = "'.$id.'" AND token <> "'.$token.'
			int sameIdObjIdx = -1;
			final int size = vector.size();
			for (int i = 0; i < size; i++) {
				final DataLineOn dlo = vector.elementAt(i);
				if(dlo.id.equals(para_ID)){
					sameIdObjIdx = i;
					if(dlo.token.equals(para_TOKEN) == false){
						return "e";
					}
					break;
				}
			}
			
			//INSERT INTO `lineon` (id,ip,port,nattype,agent,upnpip,upnpport,alive,token,
			//serverNum,relayip,relayport,lenid) VALUES ("' . $id . '", "' .  $ip .'",
			//'.$port.','.$nattype.','.$agent.',"' . $upnpip . '", '.$upnpport.',
			//now(),"'.$token.'",0,"'.$_GET['relayip'].'",'.$_GET['relayport'].
			//',length("'.$id.'")) ON DUPLICATE KEY UPDATE ip = "'.$ip.'", port='. $port . ', nattype='.$nattype.', agent = '.$agent.', upnpip = "'.$upnpip.'", upnpport = '.$upnpport.', token="'.$token.'", serverNum=0, relayip="'.$_GET['relayip'].'",relayport='.$_GET['relayport'].', alive=now(), lenid=length("'.$id.'");');
			final DataLineOn lo = new DataLineOn();
			lo.id = para_ID;
			lo.ip = hcurl.getValueofPara("ip");
			lo.port = hcurl.getValueofPara("port");
			lo.nattype = hcurl.getValueofPara("nattype");
			lo.agent = hcurl.getValueofPara("agent");
			lo.upnpip = hcurl.getValueofPara("upnpip");
			lo.upnpport = hcurl.getValueofPara("upnpport");
			lo.token = para_TOKEN;
			lo.relayip = hcurl.getValueofPara("relayip");
			lo.relayport = hcurl.getValueofPara("relayport");
			lo.hideIP = hcurl.getValueofPara("hideIP");
			lo.hideToken = hcurl.getValueofPara("hideToken");
			
			if(sameIdObjIdx == -1){
				vector.add(lo);
				if(L.isLogInRelay) {
					LogManager.log(" LineOnManager ADD lineOn for UUID : " + para_ID);
				}
			}else{
				vector.set(sameIdObjIdx, lo);
				if(L.isLogInRelay) {
					LogManager.log(" LineOnManager UPDATE lineOn for UUID : " + para_ID);
				}
			}

			removeUnalive(para_ID);
		}else if(f.equals("chngrelay")){
			//relayip = "' . $_GET['ip'] . '", relayport = ' . $_GET['port'] . '  
			//WHERE id = "' . $_GET['id'] . '" AND token = "'.$_GET['token'].'";');
			final String para_ID = hcurl.getValueofPara("id");
			final String para_TOKEN = hcurl.getValueofPara("token");

			final int size = vector.size();
			for (int i = 0; i < size; i++) {
				final DataLineOn dlo = vector.elementAt(i);
				if(dlo.id.equals(para_ID) && dlo.token.equals(para_TOKEN)){
					dlo.relayip = hcurl.getValueofPara("ip");
					dlo.relayport = hcurl.getValueofPara("port");
					//dlo.alive = System.currentTimeMillis();
					break;
				}
			}
		}else if(f.equals("lineoff") || f.equals("mobiLineIn")){
			final String para_ID = hcurl.getValueofPara("id");
			final String para_TOKEN = hcurl.getValueofPara("token");
			
			final int size = vector.size();
			for (int i = 0; i < size; i++) {
				final DataLineOn dlo = vector.elementAt(i);
				//DELETE FROM `lineon` WHERE id = "' . $id . '" AND token = "'.$_GET['token'].'";');
				if(dlo.id.equals(para_ID) && dlo.token.equals(para_TOKEN)){
					if(L.isLogInRelay) {
						LogManager.log(" LineOnManager REMOVE lineOff | mobiLineIn for UUID : " + para_ID);
					}
					vector.remove(i);
					break;
				}
			}
		}else if(f.equals("alive")){
			final String para_ID = hcurl.getValueofPara("id");
			final String para_TOKEN = hcurl.getValueofPara("token");
			
			final int size = vector.size();
			for (int i = 0; i < size; i++) {
				final DataLineOn dlo = vector.elementAt(i);
				//alive = now() WHERE id = "'.$_GET['id'].'" AND token = "'.$_GET['token'].'";');
				if(dlo.id.equals(para_ID) && dlo.token.equals(para_TOKEN)){
					dlo.hideIP = hcurl.getValueofPara("hideIP");
					dlo.hideToken = hcurl.getValueofPara("hideToken");
					dlo.alive = System.currentTimeMillis();
					
					if(L.isLogInRelay) {
						LogManager.log(" LineOnManager UPDATE ID=" + para_ID + " WITH alive");
					}
					break;
				}
			}			
			
			removeUnalive(para_ID);
		}else if(f.equals("serverNum")){
			final String para_ID = hcurl.getValueofPara("id");
			final String para_TOKEN = hcurl.getValueofPara("token");
			
			final int size = vector.size();
			for (int i = 0; i < size; i++) {
				final DataLineOn dlo = vector.elementAt(i);
				//serverNum = ('.$_GET['serverNum'].') WHERE id = "'.$_GET['id'].'" AND token = "'.$_GET['token'].'";');
				if(dlo.id.equals(para_ID) && dlo.token.equals(para_TOKEN)){
					dlo.serverNum = Integer.parseInt(hcurl.getValueofPara("serverNum"));
					if(L.isLogInRelay) {
						LogManager.log(" LineOnManager UPDATE ID=" + para_ID + " WITH serverNum = " + dlo.serverNum);
					}
					break;
				}
			}
		}else if(f.equals("relay")){
			//仅供遗留未升级之用
//			return printRelayServers();
		}else if(f.equals("newrelay")){
			return printNewRelayServers();
		}else if(f.equals("sip")){
			final String para_ID = hcurl.getValueofPara("id");
			final String hideToken = hcurl.getValueofPara("hideToken");
			return printServerInfo(para_ID, hideToken);
		}
			
		return "";
	}
}
