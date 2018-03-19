package hc.core.util;

public class CNCtrlKey extends CtrlKeySet {
	public static final String CORNET_EXT = "_c";
	public static final String PNG_EXT = ".png";

	public CNCtrlKey() {
		buildMapInfo();
	}

	void buildMapInfo() {
		buildOneMap(CtrlKey.KEY_BACK, "b_back", "Back/Backspace");
		buildOneMap(CtrlKey.KEY_DIGIT_INPUT_TIME_SHIFT, "b_digit_input",
				"Digit Input/Time shift");
		buildOneMap(CtrlKey.KEY_DOWN_CHANNEL_DOWN, "b_down", "Down/CH-");
		buildOneMap(CtrlKey.KEY_NUMBER_8_TUV, "b_eight", "8/TUV");
		buildOneMap(CtrlKey.KEY_ENTER, "b_enter", "Enter");
		buildOneMap(CtrlKey.KEY_FAST_BACKWARD, "b_fast_backward",
				"Fast Backward");
		buildOneMap(CtrlKey.KEY_FAST_FORWARD, "b_fast_forward", "Fast Forward");
		buildOneMap(CtrlKey.KEY_FAVORITE_OR_CHARACTOR_STAR, "b_favorite",
				"Favorite/Charactor *");
		buildOneMap(CtrlKey.KEY_NUMBER_5_JKL, "b_five", "5/JKL");
		buildOneMap(CtrlKey.KEY_NUMBER_4_GHI, "b_four", "4/GHI");
		buildOneMap(CtrlKey.KEY_INFORMATION_OR_CHARACTOR_POUND, "b_information",
				"Information/Charactor #");
		buildOneMap(CtrlKey.KEY_LEFT_VOLUMN_DOWN, "b_left", "Left/Vol-");
		buildOneMap(CtrlKey.KEY_MENU, "b_menu", "Menu");
		buildOneMap(CtrlKey.KEY_MUTE, "b_mute", "Mute");
		buildOneMap(CtrlKey.KEY_NUMBER_9_WXYZ, "b_nine", "9/WXYZ");
		buildOneMap(CtrlKey.KEY_OK, "b_ok", "OK");
		buildOneMap(CtrlKey.KEY_NUMBER_1, "b_one", "1");
		buildOneMap(CtrlKey.KEY_PLAY_PAUSE, "b_play_pause", "Play/Pause");
		buildOneMap(CtrlKey.KEY_PROGRAM_DOWN_PAGE_DOWN, "b_program_down",
				"Program -/Page Down");
		buildOneMap(CtrlKey.KEY_PROGRAM_UP_PAGE_UP, "b_program_up",
				"Program +/Page Up");
		buildOneMap(CtrlKey.KEY_RECALL_REVIEW, "b_recall_review",
				"Recall/Review");
		buildOneMap(CtrlKey.KEY_RECORD, "b_record", "Record");
		buildOneMap(CtrlKey.KEY_RIGHT_VOLUMN_UP, "b_right", "Right/Vol+");
		buildOneMap(CtrlKey.KEY_NUMBER_7_PQRS, "b_seven", "7/PQRS");
		buildOneMap(CtrlKey.KEY_NUMBER_6_MNO, "b_six", "6/MNO");
		buildOneMap(CtrlKey.KEY_SKIP_DOWN_TUNE_DOWN, "b_skipdown_tunedown",
				"Skip-/Tune Down");
		buildOneMap(CtrlKey.KEY_SKIP_UP_TUNE_UP, "b_skipup_tuneup",
				"Skip+/Tune Up");
		buildOneMap(CtrlKey.KEY_SOURCE, "b_source", "Source");
		buildOneMap(CtrlKey.KEY_STANDBY, "b_standby", "Standby");
		buildOneMap(CtrlKey.KEY_STOP, "b_stop", "Stop");
		buildOneMap(CtrlKey.KEY_NUMBER_3_DEF, "b_three", "3/DEF");
		buildOneMap(CtrlKey.KEY_NUMBER_2_ABC, "b_two", "2/ABC");
		buildOneMap(CtrlKey.KEY_UP_CHANNEL_UP, "b_up", "Up/CH+");
		buildOneMap(CtrlKey.KEY_VOLUMN_DOWN, "b_volumn_down", "Volume Down");
		buildOneMap(CtrlKey.KEY_VOLUMN_UP, "b_volumn_up", "Volume Up");
		buildOneMap(CtrlKey.KEY_NUMBER_0, "b_zero", "0");
	}

}
