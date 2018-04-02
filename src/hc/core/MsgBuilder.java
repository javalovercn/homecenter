package hc.core;

import hc.core.util.RootBuilder;

public class MsgBuilder {
	public static final short XOR_PACKAGE_ID_LEN = 8;
	public static final short EXT_BYTE_NUM = 2 + XOR_PACKAGE_ID_LEN;// 2位校验，8位包号

	/**
	 * 进行Internet编程时则不同,因为Internet上的路由器可能会将MTU设为不同的值.
	 * 如果我们假定MTU为1500来发送数据的,而途经的某个网络的MTU值小于1500字节,
	 * 最大的坏处就是PPPoE导致MTU变小了以太网的MTU是1500，再减去PPP的包头包尾的开销（8Bytes）， 就变成1492 bytes
	 * for PPPoE。1450 for PPTP connections
	 * 又因为UDP数据报的首部8字节,所以UDP数据报的数据区最大长度为1472字节. 这个1472字节就是我们可以使用的字节数。:)
	 * 那么系统将会使用一系列的机制来调整MTU值,使数据报能够顺利到达目的地,这样就会做许多不必要的操作. 鉴于
	 * Internet上的标准MTU值为576字节,所以我建议在进行Internet的UDP编程时. 最好将UDP的数据长度控件在540字节
	 * (576-8-20-PPPoE8)以内.
	 * 
	 * 本参数须置于最前
	 */

	public static final int UDP_INTERNET_BEST_MSS = 1414;// (含PPPoE8位, 1464),
															// (1450 - 28 = 1422
															// - 8 = 1414)
	public static final int UDP_INTERNET_MIN_MSS = 540;// (含PPPoE8位)

	public final static int UDP_BYTE_SIZE = Integer.parseInt(
			RootConfig.getInstance().getProperty(RootConfig.p_DefaultUDPSize));// 484;//548;//1464;

	/**
	 * 服务器如果发现可以切换的Relay，则进行连接并挂上； 具体：服务器在等待客户机未到时，发现可供其中继的，向其先发送本信令，
	 * 中继服务回应其信令，完成中继过程
	 */
	public static final byte E_TAG_RELAY_REG = 1;
	public static final byte E_LINE_OFF_EXCEPTION = 2;
	public static final byte E_TAG_SERVER_RELAY_START = 3;

	// 中继服务器向其中继的客户(CS)发送切换中继服务信令，
	// 接受中继服务的服务器或客户机收到后进行中继切换。
	public static final byte E_TAG_MOVE_TO_NEW_RELAY = 4;
	// 在执行'E_TAG_MOVE_TO_NEW_RELAY'前，即将关闭的中继服务器向可能提供中继后续服务的服务器，发送本信令，
	// 以通知进入接受状态，提供中继后续服务器回应本信令_BACK，表明其在线并可接受中继迁移。
	public static final byte E_TAG_NOTIFY_TO_NEW_RELAY = 5;
	// 由于上述是在中继服务器间，所以需要增加_BACK，以免岐义
	// public static final byte E_TAG_NOTIFY_TO_NEW_RELAY_BACK = 10;
	public static final byte E_TAG_SHUT_DOWN = 6;// 参见E_TAG_SHUT_DOWN_BETWEEN_CS，保留为未来可能的中继之用
	public static final byte E_TAG_ROOT = 7;
	public static final byte E_TAG_DIRECT_CONN = 8;
	public static final byte E_TAG_UN_FORWARD_DATA = 9;
	public static final byte E_TAG_ACK = 10;
	public static final byte E_TAG_ROOT_UDP_ADDR_REG = 11;
	// 发送SUB_TAG型消息，只有头，没有信息体
	public static final byte E_TAG_ONLY_SUB_TAG_MSG = 12;
	public static final byte E_TAG_MTU_1472 = 13;

	public static final byte E_RANDOM_FOR_CHECK_CK_PWD = 20;
	public static final byte E_AFTER_CERT_STATUS = 21;// 返回可能为服务已占满，密码错误，证书失效
	public static final byte E_TRANS_NEW_CERT_KEY = 22;// 传输证书
	public static final byte E_TRANS_ONE_TIME_CERT_KEY = 23;// 传输OneTime证书
	public static final byte E_TRANS_SERVER_CONFIG = 24;
	public static final byte E_CLIENT_VER_INFO = 25;
	public static final byte E_GOTO_URL_UN_XOR = 26;
	public static final byte E_RANDOM_FOR_CHECK_SERVER = 27;
	public static final byte E_ACK_XOR_PACKAGE_ID = 28;
	public static final byte E_SWAP_SOCK_SYN_XOR_PACKAGE_ID = 29;
	public static final byte E_RE_TRANS_XOR_PACKAGE = 30;
	// UN_XOR_MSG_TAG_MIN以上(含)，低于(含)此值，强制使用TCP
	public static final byte UN_XOR_MSG_TAG_MIN = 39;

	// 以上段，不进行加密解密处理，
	public static final byte E_SCREEN_EXT_MOUSE_ICON = 40;// 必须能重发

	public static final byte E_CLIENT_INFO_IN_SECS_CHANNEL = 41;
	public static final byte E_CANVAS_MAIN = 50;
	public static final byte E_GOTO_URL = 51;

	public static final byte E_INPUT_EVENT = 52;
	public static final byte E_MENU_REFRESH = 53;
	public static final byte E_GOTO_URL_SUPER_LEVEL = 54;

	// 用于服务器和手机端双向通知下线，需要加密，因为要保护Token
	public static final byte E_TAG_SHUT_DOWN_BETWEEN_CS = 60;
	public static final byte E_TRANS_NEW_CERT_KEY_IN_SECU_CHANNEL = 61;// 在线更新情形时，传输证书于加密通道，
	public static final byte E_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL = 62;// 传输OneTime证书于加密通道，
	public static final byte E_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL = 63;// 应答
																					// 传输OneTime证书于加密通道，

	public static final byte E_CTRL_STATUS = 70;// 从未使用
	public static final byte E_CTRL_SUBMIT = 71;// E_JCIP_FORM_SUBMIT =
												// 71;//从未使用
	// public static final byte E_JCIP_REQUEST = 73;//从未使用
	public static final byte E_SOUND = 74;
	public static final byte E_IMAGE_PNG = 75;
	public static final byte E_IMAGE_PNG_THUMBNAIL = 76;

	public static final byte E_SCREEN_SELECT_TXT = 79;
	public static final byte E_SCREEN_MOVE_UP = 80;
	public static final byte E_SCREEN_MOVE_LEFT = 81;
	public static final byte E_SCREEN_MOVE_DOWN = 82;
	public static final byte E_SCREEN_MOVE_RIGHT = 83;

	// 由于采用全屏方式，所以不需要向服务器传送
	public static final byte E_SCREEN_ZOOM = 84;

	// CS进行RemoteScreenSize双向通讯，保留旧方法，新的传输见E_TRANS_SERVER_CONFIG
	// public static final byte E_SCREEN_REMOTE_SIZE = 86;
	public static final byte E_SCREEN_COLOR_MODE = 87;
	public static final byte E_SCREEN_REFRESH_MILLSECOND = 88;
	// public static final byte E_SCREEN_REFRESH_RECTANGLE = 89;
	public static final byte E_BIG_MSG_JS_TO_MOBILE = 90;
	public static final byte E_JS_EVENT_TO_SERVER = 91;
	public static final byte E_STREAM_MANAGE = 92;
	public static final byte E_STREAM_DATA = 93;

	// public static final byte E_JCIP_FORM_REFRESH = 101;//停止使用
	public static final byte E_SCREEN_BLOCK_COPY = 102;
	public static final byte E_LOAD_CACHE = 103;
	public static final byte E_RESP_CACHE_OK = 104;

	public static final byte E_CLASS = 126;
	public static final byte E_PACKAGE_SPLIT_TCP = 127;

	// 以下段属于UDP_CONTROLLER段专属
	public static final byte LEN_UDP_CONTROLLER_HEAD = 2;
	public static final byte E_UDP_CONTROLLER_TEST_SPEED = 1;
	public static final byte E_UDP_CONTROLLER_SET_ADDR_NULL = 2;
	public static final byte DATA_UDP_CONTROLLER_SET_ADDR_NULL_SUCC = 1;
	public static final byte DATA_UDP_CONTROLLER_SET_ADDR_NULL_NOT_FOUND = 2;

	public static final byte DATA_PACKET_SPLIT = 'Y';
	public static final byte DATA_PACKET_NOT_SPLIT = 'N';

	public static final byte DATA_IS_SERVER_TO_RELAY = 's';
	public static final byte DATA_IS_CLIENT_TO_RELAY = 'c';

	public static final byte DATA_ROOT_UPNP_TEST = 1;
	public static final byte DATA_ROOT_RESTART = 2;
	// 专供RootMonitor之用，Root间的连通检测。不能被普通服务器使用
	public static final byte DATA_ROOT_LINE_WATCHER_ON_RELAY = 3;
	public static final byte DATA_ROOT_SAME_ID_IS_USING = 4;
	public static final byte DATA_ROOT_UDP_PORT_NOTIFY = 5;
	public static final byte DATA_ROOT_LINE_WATCHER_ON_SERVERING = 6;
	public static final byte DATA_ROOT_ONLINE_DB_EXEC = 7;
	public static final byte DATA_ROOT_OS_IN_LOCK = 8;
	public static final byte DATA_ROOT_SERVER_IN_DIRECT_MODE = 9;
	public static final byte DATA_ROOT_DIRECT_CONN_OK = 10;
	public static final byte DATA_ROOT_MATCHED_FOR_CLIENT_ON_RELAY = 11;
	public static final byte DATA_ROOT_SAME_ID_CHECK_ALIVE = 12;

	public static final byte DATA_E_TAG_RELAY_REG_SUB_FIRST = 0;
	// public static final byte DATA_E_TAG_RELAY_REG_SUB_RESET = 1;
	public static final byte DATA_E_TAG_RELAY_REG_SUB_BUILD_NEW_CONN = 2;

	public static final byte DATA_SUB_TAG_MSG_MTU_1472 = 1;
	public static final byte DATA_SUB_TAG_MSG_UDP_CHECK_ALIVE = 2;

	public static final short LEN_CTRL_TAG = 1;
	public static final short LEN_CTRL_SUB = 1;
	public static final short LEN_MSG_LEN_HIGH = 1;
	public static final short LEN_MSG_LEN_MID = 1;
	public static final short LEN_MSG_LEN_LOW = 1;

	public static final short LEN_UDP_HEADER = 2;

	public static final short LEN_UUID_LEN_STORE = 0;// 存储UUID长度，1个长度，如javalover，则为9
	public static final short LEN_MAX_UUID_VALUE = 39;// 注意同步DataNatReqConn内的UUID
														// 39
	public static final short LEN_MAX_UUID_UDP_VALUE = 0;// 注意同步DataNatReqConn内的UUID
															// 39

	public static final int TCP_PACKAGE_SPLIT_EXT_BUF_SIZE = 1024;
	public static final int MAX_LEN_TCP_PACKAGE_BLOCK_BUF = getMaxLenTCP();
	public static final int MAX_LEN_TCP_PACKAGE_SPLIT = MAX_LEN_TCP_PACKAGE_BLOCK_BUF
			- TCP_PACKAGE_SPLIT_EXT_BUF_SIZE;
	public static final short LEN_TCP_PACKAGE_SPLIT_TAG = 1;
	public static final short LEN_TCP_PACKAGE_SPLIT_SUB_TAG = 1;
	// public static final short LEN_TCP_PACKAGE_SPLIT_GROUP_ID_HIGH = 1;
	// public static final short LEN_TCP_PACKAGE_SPLIT_GROUP_ID_MID_HIGH = 1;
	// public static final short LEN_TCP_PACKAGE_SPLIT_GROUP_ID_MID_LOW = 1;
	// public static final short LEN_TCP_PACKAGE_SPLIT_GROUP_ID_LOW = 1;
	// public static final short LEN_TCP_PACKAGE_SPLIT_NUM_HIGH = 1;//分组内的总块数
	// public static final short LEN_TCP_PACKAGE_SPLIT_NUM_MID_HIGH = 1;
	// public static final short LEN_TCP_PACKAGE_SPLIT_NUM_MID_LOW = 1;
	// public static final short LEN_TCP_PACKAGE_SPLIT_NUM_LOW = 1;
	public static final short LEN_TCP_PACKAGE_SPLIT_DATA_BLOCK_LEN = 10;// 需要从标准数据块中，切取另用的长度

	public static final short LEN_NEED_ACK = 1;// 需要接收确认
	public static final short LEN_PACKET_SPLIT = 1;// 分包
	public static final short LEN_PACKET_SPLIT_NUM_HIGH = 1;// 分包总数
	public static final short LEN_PACKET_SPLIT_NUM_LOW = 1;// 分包总数
	public static final short LEN_PACKET_SPLIT_NO_HIGH = 1;// 分包编号
	public static final short LEN_PACKET_SPLIT_NO_LOW = 1;// 分包编号

	public static final short LEN_MSG_ID_HIGH = 1;// 每条消息的接收确认ID
	public static final short LEN_MSG_ID_MID = 1;// 每条消息的接收确认ID
	public static final short LEN_MSG_ID_LOW = 1;// 每条消息的接收确认ID
	public static final short LEN_MSG_ID_TOTAL_SIZE = 3;// 上述三块总字节长

	// 每到65535时，又从0开始
	public static final short LEN_GROUP_ID_HIGH = 1;// 每条消息所在Blob组的接收确认ID
	public static final short LEN_GROUP_ID_LOW = 1;// 每条消息所在Blob组的接收确认ID

	public static final short LEN_SERVER_ON_RELAY = 1;

	public static final byte NULL_CTRL_SUB_TAG = (byte) 255;

	public static final short INDEX_CTRL_TAG = 0;
	public static final short INDEX_CTRL_SUB_TAG = INDEX_CTRL_TAG
			+ LEN_CTRL_TAG;// 注意：由于sub_tag与msg_len可能合并成大串。参见setBigMsgLen

	public static final short INDEX_MSG_LEN_HIGH = INDEX_CTRL_SUB_TAG
			+ LEN_CTRL_SUB;// 注意：由于sub_tag与msg_len可能合并成大串。参见setBigMsgLen
	public static final short INDEX_MSG_LEN_MID = INDEX_MSG_LEN_HIGH
			+ LEN_MSG_LEN_HIGH;
	public static final short INDEX_MSG_LEN_LOW = INDEX_MSG_LEN_MID
			+ LEN_MSG_LEN_MID;

	public static final short INDEX_MSG_DATA = INDEX_MSG_LEN_LOW
			+ LEN_MSG_LEN_LOW;
	// 只含有头标识的消息体，不需要更多内容。但含零长度信息
	public static final short MIN_LEN_MSG = INDEX_MSG_DATA;

	public static final int TCP_SPLIT_STORE_IDX = MsgBuilder.INDEX_MSG_DATA
			+ MsgBuilder.LEN_TCP_PACKAGE_SPLIT_DATA_BLOCK_LEN;
	public static final int INDEX_TCP_SPLIT_TAG = MsgBuilder.INDEX_MSG_DATA;
	public static final int INDEX_TCP_SPLIT_SUB_TAG = INDEX_TCP_SPLIT_TAG
			+ LEN_TCP_PACKAGE_SPLIT_TAG;
	public static final int INDEX_TCP_SPLIT_SUB_GROUP_ID = INDEX_TCP_SPLIT_SUB_TAG
			+ LEN_TCP_PACKAGE_SPLIT_SUB_TAG;
	public static final int INDEX_TCP_SPLIT_SUB_GROUP_NUM = INDEX_TCP_SPLIT_SUB_GROUP_ID
			+ 4;// 分组内的总块数

	private static final int getMaxLenTCP() {
		if (RootBuilder.isSimu()) {
			// System.out.println("getMaxLenTCP : 1 << 12");
			return 1 << 12;
		} else {
			// System.out.println("getMaxLenTCP : 1 << 17");
			return 1 << 17;// 128K。注意：请与以下参数保持联动 sendSlowMaxUnackPackageNum
		}
	}

	public static final int sendSlowMaxUnackPackageNum = 100;

	/**
	 * UDP数组结构区
	 */
	public static final short INDEX_UDP_HEADER = INDEX_MSG_DATA;
	public static final short INDEX_PACKET_SPLIT = INDEX_UDP_HEADER
			+ LEN_UDP_HEADER;
	public static final short INDEX_SERVER_ON_RELAY = INDEX_PACKET_SPLIT
			+ LEN_PACKET_SPLIT;
	// UUID物理最大设计存储字节为39
	public static final short INDEX_UUID_LEN = INDEX_SERVER_ON_RELAY
			+ LEN_SERVER_ON_RELAY;
	public static final short INDEX_UUID_VALUE_START = INDEX_UUID_LEN
			+ LEN_UUID_LEN_STORE;// 值开始的地方，如上为j所在。

	public static final short INDEX_NEED_ACK = INDEX_UUID_VALUE_START
			+ LEN_MAX_UUID_UDP_VALUE;

	public static final short INDEX_PACKET_SPLIT_NUM_HIGH = INDEX_NEED_ACK
			+ LEN_NEED_ACK;
	public static final short INDEX_PACKET_SPLIT_NUM_LOW = INDEX_PACKET_SPLIT_NUM_HIGH
			+ LEN_PACKET_SPLIT_NUM_HIGH;

	public static final short INDEX_PACKET_SPLIT_NO_HIGH = INDEX_PACKET_SPLIT_NUM_LOW
			+ LEN_PACKET_SPLIT_NUM_LOW;
	public static final short INDEX_PACKET_SPLIT_NO_LOW = INDEX_PACKET_SPLIT_NO_HIGH
			+ LEN_PACKET_SPLIT_NO_HIGH;

	public static final short INDEX_MSG_ID_HIGH = INDEX_PACKET_SPLIT_NO_LOW
			+ LEN_PACKET_SPLIT_NO_LOW;
	public static final short INDEX_MSG_ID_MID = INDEX_MSG_ID_HIGH
			+ LEN_MSG_ID_HIGH;
	public static final short INDEX_MSG_ID_LOW = INDEX_MSG_ID_MID
			+ LEN_MSG_ID_MID;

	public static final short INDEX_GROUP_ID_HIGH = INDEX_MSG_ID_LOW
			+ LEN_MSG_ID_LOW;
	public static final short INDEX_GROUP_ID_LOW = INDEX_GROUP_ID_HIGH
			+ LEN_GROUP_ID_HIGH;

	public static final short INDEX_UDP_MSG_DATA = INDEX_GROUP_ID_LOW
			+ LEN_GROUP_ID_LOW;
	public static final short MIN_UDP_LEN_MSG = INDEX_UDP_MSG_DATA;
	// public static final int MAX_LEN_UDP_MSG_DATA = UDP_BYTE_SIZE -
	// INDEX_UDP_MSG_DATA;
	// 结构区所能存放的最大的UDP数据长度，实际可能小于该值，因为从548到1492寻优问题
	public static final int UDP_MTU_DATA_MIN_SIZE = UDP_INTERNET_MIN_MSS
			- INDEX_UDP_MSG_DATA;
	public static final int UDP_MTU_DATA_MAX_SIZE = UDP_BYTE_SIZE
			- INDEX_UDP_MSG_DATA;
}
