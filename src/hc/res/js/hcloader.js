(function(){

	function Loader() {
		this.isStopLoading = false;
		this.internalMS = 200;
		this.loadingTag = 'loading';
		this.loadRootDiv = 'HC_DIV_' + this.loadingTag;
		this.centerDiv = 'HC_DIV_loading_center';
		this.leftMovDiv = 'HC_DIV_loading_m_left';
		this.rightMovDiv = 'HC_DIV_loading_m_right';
		this.centerMovDiv = 'HC_DIV_loading_m_center';
	}

	Loader.prototype = {
		stop: function(){
			this.isStopLoading = true;
			var delDiv = document.getElementById(this.loadRootDiv);
			window.hcj2se.setComponentVisible(this.loadingTag, false);
			/* delDiv.parentNode.removeChild(delDiv);*/

			window.hcserver.notifyLastGone();
		},

		init: function() {
			var rootDiv = document.getElementById(this.loadRootDiv);
			if(rootDiv){
			}else{
				return;
			}
			var r_w = parseInt(rootDiv.style.width);
			var r_h = parseInt(rootDiv.style.height);
			var c_w = r_w / 3;
			var c_h = c_w;
			var i_w = r_w / 10;

			var newdiv = document.createElement('div');
			newdiv.setAttribute('id', this.centerDiv);
			newdiv.setAttribute('style', 'position:absolute;left:'+ ((r_w - c_w) / 2) + 'px;top:'+ ((r_h - c_h) / 2) + 'px;width:' + c_w + 'px;height:' + c_h + 'px;');
			rootDiv.appendChild(newdiv);

			{
				var leftdiv = document.createElement('div');
				leftdiv.setAttribute('id', this.leftMovDiv);
				leftdiv.setAttribute('style', 'background-color:red;position:absolute;left:0px;top:'+ ((c_w - i_w) / 2) + 'px;width:' + i_w + 'px;height:' + i_w + 'px;');
				newdiv.appendChild(leftdiv);
			}

			{
				var rightdiv = document.createElement('div');
				rightdiv.setAttribute('id', this.rightMovDiv);
				rightdiv.setAttribute('style', 'background-color:blue;position:absolute;left:'+ (c_w - i_w) + 'px;top:'+ ((c_w - i_w) / 2) + 'px;width:' + i_w + 'px;height:' + i_w + 'px;');
				newdiv.appendChild(rightdiv);
			}

			{
				var cdiv = document.createElement('div');
				cdiv.setAttribute('id', this.centerMovDiv);
				cdiv.setAttribute('style', 'background-color:yellow;position:absolute;left:'+ ((c_w - i_w) / 2) + 'px;top:'+ ((c_w - i_w) / 2) + 'px;width:' + i_w + 'px;height:' + i_w + 'px;');
				newdiv.appendChild(cdiv);
			}

			setTimeout("window.hcloader.move(5)", this.internalMS);
		},

		move: function(direction) {
			if(this.isStopLoading == true){
				return;
			}
			
			var leftdiv = document.getElementById(this.leftMovDiv);
			var rightdiv = document.getElementById(this.rightMovDiv);
			var step = 10;

			if(direction == 5){
				direction = -5;
			}else if(direction > 0){
				leftdiv.style.left = (parseInt(leftdiv.style.left) + step) + "px";
				rightdiv.style.left = (parseInt(rightdiv.style.left) - step) + "px";
				direction += 1;
			}else if(direction == -1){
				direction = 1;
			}else if(direction < 0){
				leftdiv.style.left = (parseInt(leftdiv.style.left) - step) + "px";
				rightdiv.style.left = (parseInt(rightdiv.style.left) + step) + "px";
				direction += 1;
			}

			setTimeout("window.hcloader.move(" + direction + ")", this.internalMS);
		}
	};

	window.hcloader = new Loader();
	window.hcloader.init();

}());
