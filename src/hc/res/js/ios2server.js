(function(){

	function iosj2se() {
	}

	iosj2se.prototype = {
		sendCmdThreePara: function(cmds, para1, para2, para3){
			var post = {'num' : '3', 'cmds' : cmds, 'para1' : ('' + para1), 'para2' : ('' + para2), 'para3' : ('' + para3)};
			window.webkit.messageHandlers.hcj2se.postMessage(post);
		},

		sendCmdTwoPara: function(cmds, para1, para2){
			var post = {'num' : '2', 'cmds' : cmds, 'para1' : ('' + para1), 'para2' : ('' + para2)};
			window.webkit.messageHandlers.hcj2se.postMessage(post);
		},

		sendCmdOnePara: function(cmds, para1){
			var post = {'num' : '1', 'cmds' : cmds, 'para1' : ('' + para1)};
			window.webkit.messageHandlers.hcj2se.postMessage(post);
		},

		actionExt: function(cmd){
			this.sendCmdOnePara('actionExt', cmd);
		},

		clickJButton: function(hashID){
			this.sendCmdOnePara('clickJButton', '' + hashID);
		},
		
		notifyLastGone: function(){
			this.sendCmdOnePara('notifyLastGone', '');
		},

		/* webView call didFinishNavigation */
		finishOnLoad: function(){
			/*this.sendCmdOnePara('finishOnLoad', '');*/
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