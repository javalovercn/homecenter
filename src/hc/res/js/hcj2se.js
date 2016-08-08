(function(){

	function hcj2se() {
		this.ltr = true;
		this.oldScrollTop = 0;
		this.isRunningTip = false;

		window.onscroll = function () {
			var tipdiv = window.hcj2se.getDiv(CONS.HC_TIP_DIV);
			var scrollTop = window.pageYOffset|| document.documentElement.scrollTop || document.body.scrollTop;
			tipdiv.style.top = (parseInt(tipdiv.style.top) - window.hcj2se.oldScrollTop + scrollTop) + 'px';
			window.hcj2se.oldScrollTop = scrollTop;
		}
	}

	var CONS = hcj2se.CONS = {
		HC_DIV: 'hc_div_',
		HC_IMG: 'hc_img_',
		HC_CMP: 'hc_cmp_',
		HC_FOR: 'id',
		HC_TIP_DIV: 'tip'
	};

	hcj2se.prototype = {
		removeFromJPanel: function(hashID){
			var delID = CONS.HC_DIV + hashID;
			var delDiv = document.getElementById(delID);
			delDiv.parentNode.removeChild(delDiv);
		},
		
		addJButton: function(containerHashID, index, hashID){
			var newdiv = this.insertDiv(containerHashID, index, hashID);

			var jsOnClick = 'javascript:window.hcj2se.clickJButton(' + hashID + ');';/*注意：是window.hcj2se，不是window.hcserver*/
			newdiv.setAttribute('onclick', jsOnClick);

			var newButton = document.createElement('button');
			newButton.setAttribute('id',CONS.HC_CMP + hashID);
			newButton.setAttribute('type','button');
			newButton.clickMS = 0;

			newdiv.appendChild(newButton);
		},

		clickJButton: function(hashID){
			var btnID = CONS.HC_CMP + hashID;
			var btn = document.getElementById(btnID);
			if(btn){
				var d = new Date();
				var curr_msec = d.getTime();
				var minInternalMS = 1000;/*点击间隔不小于1000毫秒*/
				if(btn.clickMS + minInternalMS < curr_msec){
					/*console.log('btn last clickMS : ' + btn.clickMS);*/
					btn.clickMS = curr_msec;
					window.hcserver.clickJButton(hashID);
				}
			}
		},

		addJCheckbox: function(containerHashID, index, hashID){
			var newdiv = this.insertDiv(containerHashID, index, hashID);

			var jsOnClick = 'javascript:window.hcserver.clickJCheckbox(' + hashID + ');';
			newdiv.setAttribute('onclick', jsOnClick);

			/*newdiv.setAttribute('class','JCheckbox');*/
			
			var newCheckbox = document.createElement('input');
			newCheckbox.setAttribute('id',CONS.HC_CMP + hashID);
			newCheckbox.setAttribute('type','checkbox');
			/* newCheckbox.setAttribute('onclick', jsOnClick);*/

			newdiv.appendChild(newCheckbox);


			var label = document.createElement('label');
			label.setAttribute(CONS.HC_FOR, CONS.HC_CMP + hashID);
			newdiv.appendChild(label);
		},

		addJRadioButton: function(containerHashID, index, hashID, groupHashID){
			var newdiv = this.insertDiv(containerHashID, index, hashID);

			var jsOnClick = 'javascript:window.hcserver.clickJRadioButton(' + hashID + ');';
			newdiv.setAttribute('onclick', jsOnClick);
			
			/*newdiv.setAttribute('class','JRadioButton');*/
			
			var newRadioButton = document.createElement('input');
			newRadioButton.setAttribute('id',CONS.HC_CMP + hashID);
			newRadioButton.setAttribute('type','radio');
			newRadioButton.setAttribute('name',CONS.HC_CMP + groupHashID);/*name='radiobutton'则为一组，以实现单选*/
			/* newRadioButton.setAttribute('onclick', jsOnClick);*/
			
			newdiv.appendChild(newRadioButton);

			var label = document.createElement('label');
			label.setAttribute(CONS.HC_FOR, CONS.HC_CMP + hashID);
			newdiv.appendChild(label);
		},

		addImage: function(hashID, baseData){
			var imgID = CONS.HC_IMG + hashID;
			var img = document.getElementById(imgID);
			if(img){
				img.setAttribute('src', 'data:image/png;base64,' + baseData);
			}else{
				img = document.createElement('img');
				img.setAttribute('id', imgID);
				img.setAttribute('src', 'data:image/png;base64,' + baseData);

				var compNode = document.getElementById(CONS.HC_CMP + hashID);
				if(compNode.nodeName.toLowerCase() == 'button'){
					if(compNode.firstChild){
						compNode.insertBefore(img, compNode.firstChild);
					}else{
						compNode.appendChild(img);
					}
				}else{
					/*jLable*/
					var divNode = document.getElementById(CONS.HC_DIV + hashID);

					/*JLabel置于外，JButton置于内*/
					if(divNode.firstChild){
						divNode.insertBefore(img, divNode.firstChild);
					}else{
						divNode.appendChild(img);
					}
				}
			}
		},

		addJLabel: function(containerHashID, index, hashID){
			var newdiv = this.insertDiv(containerHashID, index, hashID);

			var label = document.createElement('label');
			label.setAttribute(CONS.HC_FOR, CONS.HC_CMP + hashID);
			newdiv.appendChild(label);
		},

		lostFocusFromJTextField: function(hashID){
			var value = document.getElementById(CONS.HC_CMP + hashID).value;
			window.hcserver.notifyTextFieldValue(hashID, value);
		},

		addSlider: function(containerHashID, index, hashID, min, max, value, step){
			var newdiv = this.insertDiv(containerHashID, index, hashID);

			var newSlider = document.createElement('input');
			newSlider.setAttribute('id',CONS.HC_CMP + hashID);
			newSlider.setAttribute('type','range');
			newSlider.min = min;
			newSlider.max = max;
			newSlider.value = value;
			if(step == 0){
			}else{
				newSlider.step = step;
			}
			newSlider.setAttribute('onchange','javascript:window.hcj2se.selectSlider(' + hashID + ');');

			newdiv.appendChild(newSlider);
		},

		addJTextField: function(containerHashID, index, hashID, isPassword, tip){
			var newdiv = this.insertDiv(containerHashID, index, hashID);

			/*newdiv.setAttribute('class','JTextField');*/
			
			var newTextField = document.createElement('input');
			var typeValue = 'text';
			if(isPassword==1){
				typeValue = 'password';
			}
			newTextField.setAttribute('type',typeValue);
			newTextField.setAttribute('id',CONS.HC_CMP + hashID);
			if(tip == 'null'){
			}else{
				newTextField.setAttribute('placeholder', tip);/*input tip*/
			}
			newTextField.setAttribute('onblur', 'javascript:window.hcj2se.lostFocusFromJTextField(' + hashID + ');');
			
			newdiv.appendChild(newTextField);
		},

		lostFocusFromJTextArea: function(hashID){
			var value = document.getElementById(CONS.HC_CMP + hashID).value;
			window.hcserver.notifyTextAreaValue(hashID, value);
		},

		addJTextArea: function(containerHashID, index, hashID, tip){
			var newdiv = this.insertDiv(containerHashID, index, hashID);

			/*newdiv.setAttribute('class','JTextField');*/
			
			var newTextArea = document.createElement('textarea');
			newTextArea.setAttribute('id',CONS.HC_CMP + hashID);
			newTextArea.setAttribute('wrap', 'soft');
			if(tip == 'null'){
			}else{
				newTextField.setAttribute('placeholder', tip);/*input tip*/
			}
			newTextArea.setAttribute('onblur', 'javascript:window.hcj2se.lostFocusFromJTextArea(' + hashID + ');');
			
			newdiv.appendChild(newTextArea);
		},

		insertAfter: function(newEl, targetEl){ 
			var parentEl = targetEl.parentNode;
			if(parentEl.lastChild == targetEl){
				parentEl.appendChild(newEl);
			}else{
				parentEl.insertBefore(newEl,targetEl.nextSibling);
			}
		},

		getDiv: function(hashID){
			return document.getElementById(CONS.HC_DIV + hashID);
		},
		
		insertDiv: function(containerHashID, index, hashID){
			var newdiv = document.createElement('div');
			newdiv.setAttribute('id',CONS.HC_DIV + hashID);
			
			var containerdiv = this.getDiv(containerHashID);
			if(index == 0){
				if(containerdiv.firstChild){
					containerdiv.insertBefore(newdiv, containerdiv.firstChild);
				}else{
					containerdiv.appendChild(newdiv);
				}
			}else{
				this.insertAfter(newdiv, containerdiv.children[index - 1]);
			}
			return newdiv;
		},

		selectCombo: function(hashID){
			window.hcserver.selectComboBox(hashID, document.getElementById(CONS.HC_CMP + hashID).selectedIndex);
		},

		selectSlider: function(hashID){
			var value = parseInt(document.getElementById(CONS.HC_CMP + hashID).value);
			window.hcserver.selectSlider(hashID, value);
		},

		changeComboBoxSelected: function(hashID, selectedIndex){
			document.getElementById(CONS.HC_CMP + hashID).options[selectedIndex].selected = true;
		},

		changeSliderValue: function(hashID, min, max, value){
			var newSlider = document.getElementById(CONS.HC_CMP + hashID);

			newSlider.min = min;
			newSlider.max = max;
			newSlider.value = value;
		},

		changeComboBoxValue: function(hashID, selectionValues){
			var oldSelect = document.getElementById(CONS.HC_CMP + hashID);

			var oldSelected = oldSelect.selectedIndex;

			oldSelect.options.length=0;/*删除全部旧的*/

			this.appendComboBoxOption(oldSelect, selectionValues);

			oldSelect.selectedIndex = oldSelected;
		},

		appendComboBoxOption: function(select, selectionValues){
			var options = selectionValues.split('###');
			for (var i = 0; i < options.length; i++) {
				var oneOption = document.createElement('option');
				oneOption.innerText = options[i];
				oneOption.textContent = options[i];
				
				select.appendChild(oneOption);
			}
		},

		addComboBox: function(containerHashID, index, hashID, selectionValues){
			var newdiv = this.insertDiv(containerHashID, index, hashID);

			var newSelect = document.createElement('select');
			newSelect.setAttribute('id',CONS.HC_CMP + hashID);
			newSelect.setAttribute('onchange', 'javascript:window.hcj2se.selectCombo(' + hashID + ');');
			newSelect.setAttribute('style', 'width:100%;height:100%;');

			this.appendComboBoxOption(newSelect, selectionValues);

			newdiv.appendChild(newSelect);
		},

		addJPanel: function(containerHashID, index, hashID){
			var newdiv = this.insertDiv(containerHashID, index, hashID);
			var j2se = this;

			/*newdiv.setAttribute('class','JPanel');*/
			newdiv.addEventListener('mouseup', function(e){
				if(!e) e=window.event;
				if(e.target != newdiv){
					return;
				}
				/* var scroll=getScrollOffsets();*/
				/* elementToDrag.style.left=(e.offsetX+scroll.x-deltaX)+'px';*/
				/* elementToDrag.style.top=(e.offsetY+scroll.y-deltaY)+'px';*/
				/* e.stopPropagation();*/
				window.hcserver.mouseReleased(hashID, e.offsetX, e.offsetY);
				}, false);
			newdiv.addEventListener('mousedown', function(e){
					if(!e) e=window.event;
					if(e.target != newdiv){
						return;
					}
					window.hcserver.mousePressed(hashID, e.offsetX, e.offsetY);
				}, false);
			newdiv.addEventListener('mouseleave', function(e){
					if(!e) e=window.event;
					if(e.target != newdiv){
						return;
					}
					window.hcserver.mouseExited(hashID, e.offsetX, e.offsetY);
				}, false);
			newdiv.addEventListener('mouseenter', function(e){
					if(!e) e=window.event;
					if(e.target != newdiv){
						return;
					}
					window.hcserver.mouseEntered(hashID, e.offsetX, e.offsetY);
				}, false);
			newdiv.addEventListener('click', function(e){
					if(!e) e=window.event;
					if(e.target != newdiv){
						return;
					}
					/* console.log('mouseClick id : ' + hashID + ', x : ' + e.offsetX + ', y : ' + e.offsetY);*/
					window.hcserver.mouseClicked(hashID, e.offsetX, e.offsetY);
				}, false);
			newdiv.addEventListener('touchmove', function(e){
					if(!e) e=window.event;
					if(e.target != newdiv){
						return;
					}
					/* e.preventDefault();*/
					/* console.log('touchmove id : ' + hashID + ', getDivTop : ' + j2se.getDivTop(newdiv) + ', y : ' + e.touches[0].clientX);*/
					/* console.log('touchmove id : ' + hashID + ', x : ' + (e.touches[0].clientX - j2se.getDivLeft(newdiv)) + ', y : ' + e.touches[0].clientX);*/
					window.hcserver.mouseDragged(hashID, 
						e.touches[0].clientX - j2se.getDivLeft(newdiv), 
						e.touches[0].clientY - j2se.getDivTop(newdiv));/*注意：offsetX是undefined*/
				}, false);
		},

		getScrollOffsets: function(w){
			w=w|| window;
			/* if(w.pageXOffset!=null)*/
			return {x:w.pageXOffset,y:w.pageYOffset};
		},

		addProgressBar: function(containerHashID, index, hashID, max, value){
			var newdiv = this.insertDiv(containerHashID, index, hashID);

			var newProgress = document.createElement('progress');
			newProgress.setAttribute('id',CONS.HC_CMP + hashID);
			newProgress.max = max;
			newProgress.value = value;

			newdiv.appendChild(newProgress);
		},

		setDivStyle: function(divID, className, styles){
			var div = document.getElementById(CONS.HC_DIV + divID);
			if(className == 'null'){				
			}else{
				div.setAttribute('class', className);
			}
			if(styles == 'null'){
			}else{
				div.style.userHCCSS = styles;
				this.rebuildStyle(divID, div);			
			}
		},

		rebuildStyle: function(divID, div){
			var buildedCss = 'position:absolute;';
			if(divID == 0){
				buildedCss += 'overflow:hidden;';
			}
			if(div.hclocation){
				buildedCss += 'left:' + div.hclocation.x + 'px;';
				buildedCss += 'top:' + div.hclocation.y + 'px;';
				buildedCss += 'width:' + div.hclocation.w + 'px;';
				buildedCss += 'height:' + div.hclocation.h + 'px;';
			}

			if(div.style.userHCCSS){
				buildedCss += div.style.userHCCSS;
			}
			div.style.cssText = buildedCss;
		},

		getDivTop: function(div){
			return div.hclocation.y;
		},

		getDivLeft: function(div){
			return div.hclocation.x;
		},

		rebuildItemStyle: function(item){
			if(item){
				var buildedCss = '';
				if(item.style.initHCCSS){
					buildedCss = item.style.initHCCSS;
				}
				if(item.style.userHCCSS){
					buildedCss += item.style.userHCCSS;
				}
				item.style.cssText = buildedCss;
			}
		},

		setItemStyle: function(item, className, styles){
			if(item){
				if(className == 'null'){
				}else{
					item.setAttribute('class', className);
				}
				if(styles == 'null'){
				}else{
					item.style.userHCCSS = styles;
					this.rebuildItemStyle(item);
				}
			}
		},

		setElementStyle: function(eleID, className, styles){
			var ele = document.getElementById(CONS.HC_CMP + eleID);
			if(ele){
			}else{
				ele = this.getLabelForID(eleID);
			}

			this.setItemStyle(ele, className, styles);
		},

		setLabelStyle: function(labelID, className, styles){
			var ele = this.getLabelForID(labelID);

			this.setItemStyle(ele, className, styles);
		},

		setLocation: function(hashID, x, y, w, h){
			var loc_div = this.getDiv(hashID);

			loc_div.hclocation = function(){};
			loc_div.hclocation.x = x;
			loc_div.hclocation.y = y;
			loc_div.hclocation.w = w;
			loc_div.hclocation.h = h;

			this.rebuildStyle(hashID, loc_div);
		},

		setLTR: function(p_ltr){
			this.ltr = p_ltr;

			var tag_html = document.getElementsByTagName('html');
			if(tag_html.length > 0){
				if(p_ltr){
					tag_html[0].setAttribute('dir', 'ltr');
				}else{
					tag_html[0].setAttribute('dir', 'rtl');
				}
			}
		},

		isLTR: function(){
			return this.ltr;	
		},
		
		setComponentEnable: function(hashID, enable) {
			var ele = document.getElementById(CONS.HC_CMP + hashID);
			if(ele){
			}else{
				ele = this.getLabelForID(hashID);
			}
			ele.disabled = !enable;
		},

		setButtonText: function(hashID, text) {
			var comp = document.getElementById(CONS.HC_CMP + hashID);
			comp.innerText = text;
			comp.textContent = text;
		},

		getLabelForID: function(hashID){
			var cmpHashID = CONS.HC_CMP + hashID;
			var labels = document.getElementsByTagName('label');
			for (var i=0; i < labels.length; i++) {
				var lableTmp = labels[i];
				if(lableTmp.getAttribute(CONS.HC_FOR) == cmpHashID){
					return lableTmp;
				}
			}
			return;
		},

		setCheckboxText: function(hashID, text) {
			var lableTmp = this.getLabelForID(hashID);
			if(lableTmp){
				lableTmp.innerText = text;/*注意：与下面的showTip同步*/
				lableTmp.textContent = text;
				return;
			}
		},

		setProgressBarValue: function(hashID, value, text){
			var progress = document.getElementById(CONS.HC_CMP + hashID);

			progress.value = value;
		},

		setLabelText: function(hashID, text) {
			this.setCheckboxText(hashID, text);
		},

		setComponentVisible: function(hashID, visible) {
			if(visible){
				this.getDiv(hashID).style.visibility='visible';
			}else{
				this.getDiv(hashID).style.visibility='hidden';
			}
		},

		setButtonSelected: function(hashID, isSelected) {
			document.getElementById(CONS.HC_CMP + hashID).checked=isSelected;
		},
		
		setTextComponentEditable: function(hashID, isEditable) {
			if(isEditable){
				document.getElementById(CONS.HC_CMP + hashID).removeAttribute('readonly');
			}else{
				document.getElementById(CONS.HC_CMP + hashID).setAttribute('readonly', 'readonly');/*W3C standara*/
			}
		},
		
		getScreenWidth: function(){
			return document.documentElement.clientWidth;
		},
		
		getScreenHeight: function(){
			return document.documentElement.clientHeight;
		},
		
		setTextComponentText: function(hashID, text) {
			document.getElementById(CONS.HC_CMP + hashID).value=text;
		},

		setTextAreaText: function(hashID, text){
			var eleID = document.getElementById(CONS.HC_CMP + hashID);
			/*eleID.value = text;*/
			eleID.innerText = text;
			eleID.textContent = text;
		},

		loadStyles: function(css){
			css = css.replace('\\"', '"');
			var style = document.createElement('style');
			style.type = 'text/css';
			try{
				style.appendChild(document.createTextNode(css))
			}catch(ex){
				style.styleSheet.cssText = css;
			}
			var head = document.getElementsByTagName('head')[0];
			head.appendChild(style);
		},

		loadJS: function(js){
			js = js.replace('\\"', '"');
			var jsScript = document.createElement('script');
			
			jsScript.language = 'javascript';
			jsScript.type = 'text/javascript';
			
			try{
				jsScript.appendChild(document.createTextNode(js))
			}catch(ex){
				jsScript.innerText = js;/*pass : opera*/
			}
			var head = document.getElementsByTagName('head')[0];
			head.appendChild(jsScript);
		},

		moveTip: function(step){
			if(step == 40){
				this.clearCurrentDisplayTip();
				window.hcserver.notifyLastGone();
				return;
			}else if(this.isRunningTip){
				var tipdiv = this.getDiv(CONS.HC_TIP_DIV);
				tipdiv.style.left = (parseInt(tipdiv.style.left) - 1) + 'px';
				setTimeout('window.hcj2se.moveTip(' + (step + 1) + ')', 100);
			}
		},

		clearCurrentDisplayTip: function(){
			this.isRunningTip = false;

			try{
				this.setComponentVisible(CONS.HC_TIP_DIV, false);
				var tipdiv = this.getDiv(CONS.HC_TIP_DIV);
				var size = tipdiv.childNodes.length;
				for (var i=size - 1;i>=0;i--){
					var childNode = tipdiv.childNodes[i];
					tipdiv.removeChild(childNode);
				} 
			}catch(ex){/*synchronize clearCurrentDisplayTip*/
			}
		},

		showTip: function(tipMsg, fontRGBColor, backRColor, backGColor, backBColor, level){
			this.clearCurrentDisplayTip();

			this.isRunningTip = true;

			var tipdiv = this.getDiv(CONS.HC_TIP_DIV);
			var firstLabel = document.createElement('label');
			firstLabel.innerText = tipMsg;
			firstLabel.textContent = tipMsg;
			var labelStyle = 'position:absolute;white-space:nowrap;';

			for (var i = level; i > 1; i--) {
				var colorRGB = 'rgb(' + 
					parseInt(backRColor + (255 - backRColor) * (i - 1) / (level - 1)) + ',' + 
					parseInt(backGColor + (255 - backGColor) * (i - 1) / (level - 1)) + ',' + 
					parseInt(backBColor + (255 - backBColor) * (i - 1) / (level - 1)) + ');';
				var offLeft = level - i;
				for (var m = 0; m < (i*2-1); m++) {
					for (var n = 0; n < (i*2-1); n++) {
						var tempLabel = document.createElement('label');
						tempLabel.innerText = tipMsg;
						tempLabel.textContent = tipMsg;


						tempLabel.setAttribute('style', labelStyle + 'left:' + (offLeft + n) + 'px;top:' + (offLeft + m) + 'px;' +
							'color:' + colorRGB);

						tipdiv.appendChild(tempLabel);
					}
				}
			};

			firstLabel.setAttribute('style', labelStyle + 'left:' + (level - 1) + 'px;top:' + (level - 1) + 'px;color:' + fontRGBColor + ';');
			tipdiv.appendChild(firstLabel);

			/* var div_0 = document.getElementById(CONS.HC_DIV + 0);*/
			var screenWidth = parseInt(this.getDiv('loading').style.width);
			tipdiv.style.left = screenWidth + 'px';
			this.setComponentVisible(CONS.HC_TIP_DIV, true);
			var visibleWidth = firstLabel.offsetWidth;
			/*console.log('screenWidth : ' + screenWidth + ', label width : ' + visibleWidth);*/
			tipdiv.style.left = (screenWidth - visibleWidth - level) + 'px';
			/* tipdiv.style.left = (parseInt(div_0.style.width) - visibleWidth) + 'px';*/

			setTimeout('window.hcj2se.moveTip(1)', 100);
		}
	};

	window.hcj2se = new hcj2se();
	window.hcserver.finishOnLoad();
}());