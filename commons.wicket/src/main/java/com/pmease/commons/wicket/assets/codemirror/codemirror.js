pmease.commons.codemirror = {
	clearMark: function(cm) {
		var marks = cm.getAllMarks();
		for (var i=0; i<marks.length; i++)  {
			marks[i].clear();
		}
	},
	clearSelection: function(cm) {
    	cm.setCursor(cm.getCursor("from"));
	},
	mark: function(cm, mark, scroll) {
		pmease.commons.codemirror.clearMark(cm);
		cm.markText(
				{line: mark.beginLine, ch: mark.beginChar}, 
				{line: mark.endLine, ch: mark.endChar},
				{className: "CodeMirror-mark"});
		if (scroll) {
			// use a timer to scroll after possible view port resize
			setTimeout(function() {
				var top = cm.charCoords({line: mark.beginLine, ch: 0}, "local").top;
				var bottom = cm.charCoords({line: mark.endLine, ch: 0}, "local").bottom;
				
				var markHeight = bottom - top;
				var clientHeight = cm.getScrollInfo().clientHeight;
				if (clientHeight <= markHeight) {
					cm.scrollTo(null, top - 50); 			
				} else {
					cm.scrollTo(null, (top+bottom-clientHeight)/2); 			
				}
			}, 0);
		}
	},
	setMode: function(cm, filePath) {
	    var modeInfo = CodeMirror.findModeByFileName(filePath);
	    if (modeInfo) {
	    	// specify mode via mime does not work for gfm (github flavored markdown)
	    	if (modeInfo.mode === "gfm")
	    		cm.setOption("mode", "gfm");
	    	else
	    		cm.setOption("mode", modeInfo.mime);
			CodeMirror.autoLoadMode(cm, modeInfo.mode);
	    }
	},
	initState: function(cm, viewState) {
	    if (viewState) {
	    	pmease.commons.history.setViewState(viewState);
	    }

	    // use timer to minimize performance impact 
	    var timer;
	    function onViewStateChange() {
    		if (timer)
    			clearTimeout(timer);
    		timer = setTimeout(function() {
    			timer = undefined;
    			var cursor = cm.getCursor();
    	    	var scrollInfo = cm.getScrollInfo();
    	    	var scroll = {left: scrollInfo.left, top: scrollInfo.top};
    	    	pmease.commons.history.setViewState({cursor: cursor, scroll: scroll});
	    	}, 500);
	    };
	    
	    cm.on("cursorActivity", onViewStateChange);
	    cm.on("scroll", onViewStateChange);

	    var viewState = pmease.commons.history.getViewState();
	    if (viewState) {
	    	if (viewState.cursor)
	    		cm.setCursor(viewState.cursor);
	    	if (viewState.scroll)
	    		cm.scrollTo(viewState.scroll.left, viewState.scroll.top);
	    }
	    
	    CodeMirror.keyMap.default["Ctrl-L"] = "gotoLine";
	    cm.focus();
	} 
};