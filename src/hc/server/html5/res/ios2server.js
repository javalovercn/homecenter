(function(){

	function iosj2se() {
	}

	iosj2se.prototype = {
		sendCmdThreePara: function(cmds, para1, para2, para3){
			window.location.href = 'int_iosj2se' + '¢¢¢' + cmds + '¢¢¢' + para1 + '¢¢¢' + para2 + '¢¢¢' + para3;
		},

		sendCmdTwoPara: function(cmds, para1, para2){
			window.location.href = 'int_iosj2se' + '¢¢¢' + cmds + '¢¢¢' + para1 + '¢¢¢' + para2;
		},

		sendCmdOnePara: function(cmds, para1){
			window.location.href = 'int_iosj2se' + '¢¢¢' + cmds + '¢¢¢' + para1;
		},

		clickJButton: function(hashID){
			this.sendCmdOnePara('clickJButton', '' + hashID);
		},
		
		notifyLastGone: function(){
			this.sendCmdOnePara('notifyLastGone', '');
		},

		finishOnLoad: function(){
			//this.sendCmdOnePara('finishOnLoad', '');//by agent
		},

		selectComboBox: function(hashID, selectedIndex){
			this.sendCmdTwoPara('selectComboBox', '' + hashID, '' + selectedIndex);
		},

		selectSlider: function(hashID, value){
			this.sendCmdTwoPara('selectSlider', '' + hashID, '' + value);
		},

		notifyTextFieldValue: function(hashID, value){
			this.sendCmdTwoPara('notifyTextFieldValue', '' + hashID, '' + value);
		},

		notifyTextAreaValue: function(hashID, value){
			this.sendCmdTwoPara('notifyTextAreaValue', '' + hashID, '' + value);
		},

		clickJRadioButton: function(hashID){
			this.sendCmdOnePara('clickJRadioButton', '' + hashID);
		},

		clickJCheckbox: function(hashID){
			this.sendCmdOnePara('clickJCheckbox', '' + hashID);
		},

		mouseReleased: function(hashID, x, y){
			this.sendCmdThreePara('mouseReleased', '' + hashID, '' + x, '' + y);
		},

		mousePressed: function(hashID, x, y){
			this.sendCmdThreePara('mousePressed', '' + hashID, '' + x, '' + y);
		},

		mouseExited: function(hashID, x, y){
			this.sendCmdThreePara('mouseExited', '' + hashID, '' + x, '' + y);
		},

		mouseEntered: function(hashID, x, y){
			this.sendCmdThreePara('mouseEntered', '' + hashID, '' + x, '' + y);
		},

		mouseClicked: function(hashID, x, y){
			this.sendCmdThreePara('mouseClicked', '' + hashID, '' + x, '' + y);
		},

		mouseDragged: function(hashID, x, y){
			this.sendCmdThreePara('mouseDragged', '' + hashID, '' + x, '' + y);
		}
	};

	window.hcserver = new iosj2se();

}());