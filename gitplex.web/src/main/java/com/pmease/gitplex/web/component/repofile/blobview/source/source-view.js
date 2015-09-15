gitplex.sourceview = {
	init: function(codeId, fileContent, filePath, mark, symbolTooltipId, revision, 
			blameCommits, commentId, addCommentCallback, cmState) {
		var cm;
		
		var $code = $("#" + codeId);
		var $sourceView = $code.closest(".source-view");
		$sourceView.closest(".body").css("overflow", "hidden");
		
		var $outlineToggle = $(".outline-toggle");
		var $outline = $sourceView.find(">.outline");
		var cookieKey = "sourceView.outline";
		if ($outlineToggle.length != 0) {
			if (Cookies.get(cookieKey) === "no") {
				$outline.hide();
				$outlineToggle.removeClass("active");
			}
			$outlineToggle.click(function() {
				$outline.toggle();
				$outlineToggle.toggleClass("active");
				if ($outline.is(":visible")) 
					Cookies.set(cookieKey, "yes", {expires: Infinity});
				else 
					Cookies.set(cookieKey, "no", {expires: Infinity});
				$sourceView.trigger("autofit", [$sourceView.outerWidth(), $sourceView.outerHeight()]);
			});
		}
		
		$sourceView.on("autofit", function(event, width, height) {
			event.stopPropagation();
			$sourceView.outerWidth(width);
			$sourceView.outerHeight(height);
			$code.outerHeight($sourceView.height());
			var $outline = $sourceView.find(">.outline");
			if ($outline.is(":visible")) {
				$code.outerWidth($sourceView.width()/4.0*3);
				$outline.outerWidth($sourceView.width() - $code.outerWidth()-1);
				$outline.outerHeight($sourceView.height());
			} else {
				$code.outerWidth($sourceView.width());
			}
			
			/*
			 * initialize codemirror here when we know the container width and height
			 * as otherwise the annotatescrollbar addon is inaccurate when window 
			 * initially loads
			 */ 
			var initState = !cm;
			if (!cm) {
				var options = {
					value: fileContent, 
					readOnly: pmease.commons.isDevice()?"nocursor":true,
					theme: "eclipse",
					lineNumbers: true,
					lineWrapping: true,
					styleActiveLine: true,
					styleSelectedText: true,
					foldGutter: true,
					matchBrackets: true,
					scrollbarStyle: "simple",
					highlightIdentifiers: {delay: 500},
					gutters: ["CodeMirror-linenumbers", "CodeMirror-foldgutter"],
					extraKeys: {
						"F11": function(cm) {
							cm.setOption("fullScreen", !cm.getOption("fullScreen"));
						},
						"Esc": function(cm) {
							if (cm.getOption("fullScreen"))
								cm.setOption("fullScreen", false);
				        }
					}
				};

				cm = CodeMirror($code[0], options);
				
				if (addCommentCallback) {
					$code[0].addCommentCallback = addCommentCallback;
					var gutter = "CodeMirror-addcomments";
					var gutters = cm.getOption("gutters").slice();
					gutters.push(gutter);
					cm.setOption("gutters", gutters);
					for (var line=0; line<cm.lineCount(); line++) {
		    			var $ele = $(document.createElement("div"));
		    			$ele.addClass("CodeMirror-addcomment");
		    			$ele.attr("title", "Add inline comment");
		    			var script = "document.getElementById(\"" + codeId + "\").addCommentCallback(" + line + ");";
		        		$("<a href='javascript: " + script + "'><i class='fa fa-plus-square-o'></i></a>").appendTo($ele);
						cm.setGutterMarker(line, gutter, $ele[0]);
					}
				}

				pmease.commons.codemirror.setMode(cm, filePath);

			    if (mark)
			    	pmease.commons.codemirror.mark(cm, mark);

			    if (blameCommits) {
			    	// render blame blocks with a timer to avoid the issue that occasionally 
			    	// blame gutter becomes much wider than expected
			    	setTimeout(function() {
				    	gitplex.sourceview.blame(cm, blameCommits);
			    	}, 10);
			    }

			    $sourceView.find(">.comment").each(function() {
					var lineNo = $(this).data("lineno");
					gitplex.sourceview.placeComment(cm, lineNo, this);
				});
			    if (commentId != -1) {
			    	$comment = $("#pullrequest-comment-" + commentId);
			    	var lineNo = $comment.data("lineno");
					pmease.commons.codemirror.centerLine(cm, lineNo);
			    	cm.setCursor(lineNo);
			    	setTimeout(function() {$comment.find(">div").focus();}, 10);
			    }
			    
			    $code.mouseover(function(e) {
					var node = e.target || e.srcElement, $node = $(node);
					if ($node.hasClass("cm-property") || $node.hasClass("cm-variable") || $node.hasClass("cm-variable-2") 
							|| $node.hasClass("cm-variable-3") || $node.hasClass("cm-def") || $node.hasClass("cm-meta")) {
						document.getElementById(symbolTooltipId).onMouseOverSymbol(revision, node);
					}
			    });
			} 
			if (cm.getOption("fullScreen"))
				cm.setOption("fullScreen", false);
			cm.setSize($code.width(), $code.height());
			if (initState)
				pmease.commons.codemirror.initState(cm, cmState);
		});
	}, 

	mark: function(codeId, mark) {
		var cm = $("#"+ codeId + ">.CodeMirror")[0].CodeMirror;		
		pmease.commons.codemirror.mark(cm, mark);
	},
	
	placeComment: function(cm, lineNo, comment) {
		if (typeof cm === "string") 
			cm = $("#"+ cm + ">.CodeMirror")[0].CodeMirror;		
		if (typeof comment === "string")
			comment = $("<div class='comment'><div id='" + comment + "'></div></div>")[0];
		comment.lineWidget = cm.addLineWidget(lineNo, comment, {coverGutter: true});
	},
	
	commentResized: function($comment) {
		if (typeof $comment === "string") 
			$comment = $("#" + $comment);
		$comment[0].lineWidget.changed();
		var $wrapper = $comment.find(">div");
		$wrapper.on("mouseup resized", function() {
			$comment[0].lineWidget.changed();
		});
		$wrapper.on("fullscreen", function() {
			// full screen mode is abnormal if we do not do this
			$("body").append($wrapper);
		});
		$wrapper.on("exitFullscreen", function() {
			$comment.append($wrapper);
			$comment[0].lineWidget.changed();
		});
	},
	
	blame: function(cm, blameCommits) {
		if (typeof cm === "string") 
			cm = $("#"+ cm + ">.CodeMirror")[0].CodeMirror;		
		
		if (blameCommits) {
			var gutters = cm.getOption("gutters").slice();
			gutters.splice(0, 0, "CodeMirror-annotations");
			cm.setOption("gutters", gutters);
    		for (var i in blameCommits) {
    			var commit = blameCommits[i];
        		for (var j in commit.ranges) {
        			var range = commit.ranges[j];
        			var $ele = $(document.createElement("div"));
        			$ele.addClass("CodeMirror-annotation");
            		$("<a class='hash'>" + commit.hash + "</a>").appendTo($ele).attr("href", commit.url).attr("title", commit.message);
            		$ele.append("<span class='date'>" + commit.authorDate + "</span>");
            		$ele.append("<span class='author'>" + commit.authorName + "</span>");
            		cm.setGutterMarker(range.beginLine, "CodeMirror-annotations", $ele[0]);
            		
            		for (var line = range.beginLine+1; line<range.endLine; line++) {
            			var $ele = $(document.createElement("div"));
            			$ele.addClass("CodeMirror-annotation");
                		$ele.append("<span class='same-as-above fa fa-arrow-up' title='same as above'></span>");
                		cm.setGutterMarker(line, "CodeMirror-annotations", $ele[0]);
            		}
        		}
    		}    		
		} else {
			cm.clearGutter("CodeMirror-annotations");
			var gutters = cm.getOption("gutters").slice();
			gutters.splice(0, 1);
			cm.setOption("gutters", gutters);
		}
	}
	
}
