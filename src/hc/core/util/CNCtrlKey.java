package hc.core.util;


public class CNCtrlKey extends CtrlKey {
	public CNCtrlKey() {
		buildMapInfo();
	}
	
	public static String getRoundImageName(final String pngName){
		return pngName.substring(0, pngName.indexOf(".")) + "_c.png";
	}
	
	void buildMapInfo(){
		buildOneMap(CtrlKey.KEY_BACK, "b_back.png", "Back/Backspace");
		buildOneMap(CtrlKey.KEY_DIGIT_INPUT_TIME_SHIFT, "b_digit_input.png", "Digit Input/Time shift");
		buildOneMap(CtrlKey.KEY_DOWN_CHANNEL_DOWN, "b_down.png", "Down/CH-");
		buildOneMap(CtrlKey.KEY_NUMBER_8_TUV, "b_eight.png", "8/TUV");
		buildOneMap(CtrlKey.KEY_ENTER, "b_enter.png", "Enter");
		buildOneMap(CtrlKey.KEY_FAST_BACKWARD, "b_fast_backward.png", "Fast Backward");
		buildOneMap(CtrlKey.KEY_FAST_FORWARD, "b_fast_forward.png", "Fast Forward");
		buildOneMap(CtrlKey.KEY_FAVORITE_OR_CHARACTOR_STAR, "b_favorite.png", "Favorite/Charactor *");
		buildOneMap(CtrlKey.KEY_NUMBER_5_JKL, "b_five.png", "5/JKL");
		buildOneMap(CtrlKey.KEY_NUMBER_4_GHI, "b_four.png", "4/GHI");
		buildOneMap(CtrlKey.KEY_INFORMATION_OR_CHARACTOR_POUND, "b_information.png", "Information/Charactor #");
		buildOneMap(CtrlKey.KEY_LEFT_VOLUMN_DOWN, "b_left.png", "Left/Vol-");
		buildOneMap(CtrlKey.KEY_MENU, "b_menu.png", "Menu");
		buildOneMap(CtrlKey.KEY_MUTE, "b_mute.png", "Mute");
		buildOneMap(CtrlKey.KEY_NUMBER_9_WXYZ, "b_nine.png", "9/WXYZ");
		buildOneMap(CtrlKey.KEY_OK, "b_ok.png", "OK");
		buildOneMap(CtrlKey.KEY_NUMBER_1, "b_one.png", "1");
		buildOneMap(CtrlKey.KEY_PLAY_PAUSE, "b_play_pause.png", "Play/Pause");
		buildOneMap(CtrlKey.KEY_PROGRAM_DOWN_PAGE_DOWN, "b_program_down.png", "Program -/Page Down");
		buildOneMap(CtrlKey.KEY_PROGRAM_UP_PAGE_UP, "b_program_up.png", "Program +/Page Up");
		buildOneMap(CtrlKey.KEY_RECALL_REVIEW, "b_recall_review.png", "Recall/Review");
		buildOneMap(CtrlKey.KEY_RECORD, "b_record.png", "Record");
		buildOneMap(CtrlKey.KEY_RIGHT_VOLUMN_UP, "b_right.png", "Right/Vol+");
		buildOneMap(CtrlKey.KEY_NUMBER_7_PQRS, "b_seven.png", "7/PQRS");
		buildOneMap(CtrlKey.KEY_NUMBER_6_MNO, "b_six.png", "6/MNO");
		buildOneMap(CtrlKey.KEY_SKIP_DOWN_TUNE_DOWN, "b_skipdown_tunedown.png", "Skip-/Tune Down");
		buildOneMap(CtrlKey.KEY_SKIP_UP_TUNE_UP, "b_skipup_tuneup.png", "Skip+/Tune Up");
		buildOneMap(CtrlKey.KEY_SOURCE, "b_source.png", "Source");
		buildOneMap(CtrlKey.KEY_STANDBY, "b_standby.png", "Standby");
		buildOneMap(CtrlKey.KEY_STOP, "b_stop.png", "Stop");
		buildOneMap(CtrlKey.KEY_NUMBER_3_DEF, "b_three.png", "3/DEF");
		buildOneMap(CtrlKey.KEY_NUMBER_2_ABC, "b_two.png", "2/ABC");
		buildOneMap(CtrlKey.KEY_UP_CHANNEL_UP, "b_up.png", "Up/CH+");
		buildOneMap(CtrlKey.KEY_VOLUMN_DOWN, "b_volumn_down.png", "Volume Down");
		buildOneMap(CtrlKey.KEY_VOLUMN_UP, "b_volumn_up.png", "Volume Up");
		buildOneMap(CtrlKey.KEY_NUMBER_0, "b_zero.png", "0");
	}
	

}
