package hc.core.util;

public interface CtrlKey {

	//进入/退出待机状态
	public final static int KEY_STANDBY = 0;
	//音频输出开/关
	public final static int KEY_MUTE = 1;
	//菜单开/关
	public final static int KEY_MENU = 2;
	//菜单项确认
	public final static int KEY_OK = 3;
	//上移/频道号增加1
	public final static int KEY_UP_CHANNEL_UP = 4;
	//下移/频道号减少1
	public final static int KEY_DOWN_CHANNEL_DOWN = 5;
	//左移/音量减少1
	public final static int KEY_LEFT_VOLUMN_DOWN = 6;
	//右移/音量增加1
	public final static int KEY_RIGHT_VOLUMN_UP = 7;
	//返回/退格  返回上一级菜单
	//	退格（取消前一个输入）
	public final static int KEY_BACK = 8;
	//节目号增加1/向上翻页
	public final static int KEY_PROGRAM_UP_PAGE_UP = 9;
	//节目号减少1/向下翻页
	public final static int KEY_PROGRAM_DOWN_PAGE_DOWN = 10;
	//音量增加1
	public final static int KEY_VOLUMN_UP = 11;
	//音量减少1
	public final static int KEY_VOLUMN_DOWN = 12;
	//回车键
	public final static int KEY_ENTER = 13;
	//
//	public final static int KEY_14 = 14;
//	public final static int KEY_15 = 15;
//	public final static int KEY_16 = 16;
	//大写开/关
	public final static int KEY_CAPITAL = 17;
	//大小写切换/输入法选择
	public final static int KEY_ALT = 18;
	//功能键1，厂商定义热键功能
	public final static int KEY_FUNCTION1 = 19;
	public final static int KEY_FUNCTION2 = 20;
	public final static int KEY_FUNCTION3 = 21;
	public final static int KEY_FUNCTION4 = 22;
	//图像控制子菜单的快捷键
	public final static int KEY_PICTURE_CONTROL = 23;
	//
//	public final static int KEY_24 = 24;
	//声音控制子菜单的快捷键
	public final static int KEY_SOUND_CONTROL = 25;
	//选择输入数字位数/选择回看录制节目的时间点
	public final static int KEY_DIGIT_INPUT_TIME_SHIFT = 26;
	//媒体播放/暂停
	public final static int KEY_PLAY_PAUSE = 27;
	//停止播放
	public final static int KEY_STOP = 28;
	//快进
	public final static int KEY_FAST_FORWARD = 29;
	//快退
	public final static int KEY_FAST_BACKWARD = 30;
	//录制
	public final static int KEY_RECORD = 31;
	//空格
	public final static int KEY_SPACE = 32;
	//下一曲/搜台+
	public final static int KEY_SKIP_UP_TUNE_UP = 33;
	//上一曲/搜台-
	public final static int KEY_SKIP_DOWN_TUNE_DOWN = 34;
	//信息/字母键 ‘#’  显示当前节目或其他状态信息/字母键 ‘#’
	public final static int KEY_INFORMATION_OR_CHARACTOR_POUND = 35;//NUMBER_SIGN
	//信源
	public final static int KEY_SOURCE = 36;
	//回看  回到前次收看的节目/回看之前录制的节目
	public final static int KEY_RECALL_REVIEW = 37;
	//
//	public final static int KEY_38 = 38;
//	public final static int KEY_39 = 39;
//	public final static int KEY_40 = 40;
//	public final static int KEY_41 = 41;
	//喜爱/收藏/字母键 ‘*’
	public final static int KEY_FAVORITE_OR_CHARACTOR_STAR = 42;
//	public final static int KEY_43 = 43;
//	public final static int KEY_44 = 44;
//	public final static int KEY_45 = 45;
	//符号‘.’/符号‘’’
	public final static int KEY_DOT_SINGLE_QUOTATION_MARK = 46;
//	public final static int KEY_47 = 47;
	//数字0
	public final static int KEY_NUMBER_0 = 48;
	//数字1
	public final static int KEY_NUMBER_1 = 49;
	//数字2/字母 ‘ABC’
	public final static int KEY_NUMBER_2_ABC = 50;
	//数字3/字母 ‘DEF’
	public final static int KEY_NUMBER_3_DEF = 51;
	//数字4/字母 ‘GHI’
	public final static int KEY_NUMBER_4_GHI = 52;
	//数字5/字母 ‘JKL’
	public final static int KEY_NUMBER_5_JKL = 53;
	//数字6/字母 ‘MNO’
	public final static int KEY_NUMBER_6_MNO = 54;
	//数字7/字母 ‘PQRS’
	public final static int KEY_NUMBER_7_PQRS = 55;
	//数字8/字母 ‘TUV’
	public final static int KEY_NUMBER_8_TUV = 56;
	//数字9/字母 ‘WXYZ’
	public final static int KEY_NUMBER_9_WXYZ = 57;
//	public final static int KEY_58 = 58;
	//红
	public final static int KEY_RED = 59;
	//黄
	public final static int KEY_YELLOW = 60;
	//兰
	public final static int KEY_BLUE = 61;
	//绿
	public final static int KEY_GREEN = 62;
//	public final static int KEY_63 = 63;
//	public final static int KEY_64 = 64;
//	public final static int KEY_65 = 65;
//	public final static int KEY_66 = 66;
//	public final static int KEY_67 = 67;
//	public final static int KEY_68 = 68;
//	public final static int KEY_69 = 69;
//	public final static int KEY_70 = 70;
//	public final static int KEY_71 = 71;
//	public final static int KEY_72 = 72;
//	public final static int KEY_73 = 73;
//	public final static int KEY_74 = 74;
//	public final static int KEY_75 = 75;
//	public final static int KEY_76 = 76;
//	public final static int KEY_77 = 77;
//	public final static int KEY_78 = 78;
//	public final static int KEY_79 = 79;
//	public final static int KEY_80 = 80;
//	public final static int KEY_81 = 81;
//	public final static int KEY_82 = 82;
//	public final static int KEY_83 = 83;
//	public final static int KEY_84 = 84;
//	public final static int KEY_85 = 85;
//	public final static int KEY_86 = 86;
//	public final static int KEY_87 = 87;
//	public final static int KEY_88 = 88;
//	public final static int KEY_89 = 89;
//	public final static int KEY_90 = 90;
//	public final static int KEY_91 = 91;
//	public final static int KEY_92 = 92;
//	public final static int KEY_93 = 93;
//	public final static int KEY_94 = 94;
//	public final static int KEY_95 = 95;
	//开仓/关仓
	public final static int KEY_OPEN_EJECT = 96;
	//字幕
	public final static int KEY_SUBTITLE = 97;
	//伴音:选择媒体的声音语言
	public final static int KEY_LANGUAGE = 98;
	//邮件/消息:
	public final static int KEY_MAIL_MESSAGE = 99;
	//资讯/新闻
	public final static int KEY_NEWS = 100;
	//互联网:连接到互联网
	public final static int KEY_Internet = 101;
	//点播: VOD（video on demond）点播视频节目
	public final static int KEY_VOD = 102;
//	public final static int KEY_103 = 103;
//	public final static int KEY_104 = 104;
//	public final static int KEY_105 = 105;
//	public final static int KEY_106 = 106;
//	public final static int KEY_107 = 107;
//	public final static int KEY_108 = 108;
//	public final static int KEY_109 = 109;
//	public final static int KEY_110 = 110;
//	public final static int KEY_111 = 111;
//	public final static int KEY_112 = 112;
//	public final static int KEY_113 = 113;
//	public final static int KEY_114 = 114;
//	public final static int KEY_115 = 115;
//	public final static int KEY_116 = 116;
//	public final static int KEY_117 = 117;
//	public final static int KEY_118 = 118;
//	public final static int KEY_119 = 119;
//	public final static int KEY_120 = 120;
//	public final static int KEY_121 = 121;
//	public final static int KEY_122 = 122;
//	public final static int KEY_123 = 123;
//	public final static int KEY_124 = 124;
//	public final static int KEY_125 = 125;
//	public final static int KEY_126 = 126;
//	public final static int KEY_127 = 127;
//	public final static int KEY_128 = 128;
//	public final static int KEY_129 = 129;
//	public final static int KEY_130 = 130;
//	public final static int KEY_131 = 131;
//	public final static int KEY_132 = 132;
//	public final static int KEY_133 = 133;
//	public final static int KEY_134 = 134;
//	public final static int KEY_135 = 135;
//	public final static int KEY_136 = 136;
//	public final static int KEY_137 = 137;
//	public final static int KEY_138 = 138;
//	public final static int KEY_139 = 139;
//	public final static int KEY_140 = 140;
//	public final static int KEY_141 = 141;
//	public final static int KEY_142 = 142;
//	public final static int KEY_143 = 143;
//	public final static int KEY_144 = 144;
//	public final static int KEY_145 = 145;
//	public final static int KEY_146 = 146;
//	public final static int KEY_147 = 147;
//	public final static int KEY_148 = 148;
//	public final static int KEY_149 = 149;
//	public final static int KEY_150 = 150;
//	public final static int KEY_151 = 151;
//	public final static int KEY_152 = 152;
//	public final static int KEY_153 = 153;
//	public final static int KEY_154 = 154;
//	public final static int KEY_155 = 155;
//	public final static int KEY_156 = 156;
//	public final static int KEY_157 = 157;
//	public final static int KEY_158 = 158;
//	public final static int KEY_159 = 159;
//	public final static int KEY_160 = 160;
//	public final static int KEY_161 = 161;
//	public final static int KEY_162 = 162;
//	public final static int KEY_163 = 163;
//	public final static int KEY_164 = 164;
//	public final static int KEY_165 = 165;
//	public final static int KEY_166 = 166;
//	public final static int KEY_167 = 167;
//	public final static int KEY_168 = 168;
//	public final static int KEY_169 = 169;
//	public final static int KEY_170 = 170;
//	public final static int KEY_171 = 171;
//	public final static int KEY_172 = 172;
//	public final static int KEY_173 = 173;
//	public final static int KEY_174 = 174;
//	public final static int KEY_175 = 175;
//	public final static int KEY_176 = 176;
//	public final static int KEY_177 = 177;
//	public final static int KEY_178 = 178;
//	public final static int KEY_179 = 179;
//	public final static int KEY_180 = 180;
//	public final static int KEY_181 = 181;
//	public final static int KEY_182 = 182;
//	public final static int KEY_183 = 183;
//	public final static int KEY_184 = 184;
//	public final static int KEY_185 = 185;
//	public final static int KEY_186 = 186;
//	public final static int KEY_187 = 187;
//	public final static int KEY_188 = 188;
//	public final static int KEY_189 = 189;
//	public final static int KEY_190 = 190;
//	public final static int KEY_191 = 191;
//	public final static int KEY_192 = 192;
//	public final static int KEY_193 = 193;
//	public final static int KEY_194 = 194;
//	public final static int KEY_195 = 195;
//	public final static int KEY_196 = 196;
//	public final static int KEY_197 = 197;
//	public final static int KEY_198 = 198;
//	public final static int KEY_199 = 199;
//	public final static int KEY_200 = 200;
//	public final static int KEY_201 = 201;
//	public final static int KEY_202 = 202;
//	public final static int KEY_203 = 203;
//	public final static int KEY_204 = 204;
//	public final static int KEY_205 = 205;
//	public final static int KEY_206 = 206;
//	public final static int KEY_207 = 207;
//	public final static int KEY_208 = 208;
//	public final static int KEY_209 = 209;
//	public final static int KEY_210 = 210;
//	public final static int KEY_211 = 211;
//	public final static int KEY_212 = 212;
//	public final static int KEY_213 = 213;
//	public final static int KEY_214 = 214;
//	public final static int KEY_215 = 215;
//	public final static int KEY_216 = 216;
//	public final static int KEY_217 = 217;
//	public final static int KEY_218 = 218;
//	public final static int KEY_219 = 219;
//	public final static int KEY_220 = 220;
//	public final static int KEY_221 = 221;
//	public final static int KEY_222 = 222;
//	public final static int KEY_223 = 223;
//	public final static int KEY_224 = 224;
//	public final static int KEY_225 = 225;
//	public final static int KEY_226 = 226;
//	public final static int KEY_227 = 227;
//	public final static int KEY_228 = 228;
//	public final static int KEY_229 = 229;
//	public final static int KEY_230 = 230;
//	public final static int KEY_231 = 231;
//	public final static int KEY_232 = 232;
//	public final static int KEY_233 = 233;
//	public final static int KEY_234 = 234;
//	public final static int KEY_235 = 235;
//	public final static int KEY_236 = 236;
//	public final static int KEY_237 = 237;
//	public final static int KEY_238 = 238;
//	public final static int KEY_239 = 239;
//	public final static int KEY_240 = 240;
//	public final static int KEY_241 = 241;
//	public final static int KEY_242 = 242;
//	public final static int KEY_243 = 243;
//	public final static int KEY_244 = 244;
//	public final static int KEY_245 = 245;
//	public final static int KEY_246 = 246;
//	public final static int KEY_247 = 247;
//	public final static int KEY_248 = 248;
//	public final static int KEY_249 = 249;
//	public final static int KEY_250 = 250;
//	public final static int KEY_251 = 251;
//	public final static int KEY_252 = 252;
//	public final static int KEY_253 = 253;
//	public final static int KEY_254 = 254;
//	public final static int KEY_255 = 255;

}