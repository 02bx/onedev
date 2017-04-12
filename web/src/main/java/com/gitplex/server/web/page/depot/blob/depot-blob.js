gitplex.server.depotBlob = {
	onDomReady: function(callback) {
		// hide browser scrollbar as we will manage it in blob content container 
		$("body").css("overflow", "hidden");
		
		var cookieKey = "depotBlob.searchResult.height";
		
		var $searchResult = $("#depot-blob>.search-result");
		var height = Cookies.get(cookieKey);
		if (height) 
			$searchResult.outerHeight(height);
		
		$searchResult.resizable({
			autoHide: false,
			handles: {"n": "#search-result-resize-handle"},
			minHeight: 100,
			resize: function(e, ui) {
				var blobContentHeight = $("#depot-blob>.blob-content").outerHeight();
			    if(blobContentHeight < 150)
			    	$(this).resizable({maxHeight: ui.size.height});
			},
			stop: function(e, ui) {
				$(this).resizable({maxHeight: undefined});
				Cookies.set(cookieKey, ui.size.height, {expires: Infinity});
			}
		});
		
		$(window).resize(function(e) {
			e.stopPropagation();
			
			var $head = $("#depot-blob>.head");
			var $revisionPicker = $head.find(">.revision-picker");
					
			// we should simply call $head.width() here, but the value is incorrect after maximize and restore
			// window in IE and Chrome (maybe due to use of table in sidebar?), so we go with the complicate 
			// approach of calculating the head width
			var headWidth = $("#depot").width() - $("#depot>.sidebar>table>tbody>tr>td.nav").outerWidth();
			
			// below code moves file navigator to bottom if it is too wide
			var maxWidth = headWidth - $revisionPicker.outerWidth() - 130;
			var maxHeight = $head.height();

			var $blobNavigator = $head.find(">.blob-navigator");
			if ($blobNavigator.length != 0) {
				if ($blobNavigator.outerWidth() > maxWidth || $blobNavigator.outerHeight() > maxHeight)
					$("#depot-blob>.blob-navigator").show().append($blobNavigator);
			} else {
				$blobNavigator = $("#depot-blob>.blob-navigator>div");
				if ($blobNavigator.outerWidth() <= maxWidth && $blobNavigator.outerHeight() <= maxHeight) {
					$blobNavigator.insertAfter($revisionPicker);
					$("#depot-blob>.blob-navigator").hide();
				}
			}

			var $blobContent = $("#depot-blob>.blob-content");
			var width = $(window).width()-$blobContent.offset().left;
			var height = gitplex.server.depotBlob.getClientHeight();
			var $searchResult = $("#depot-blob>.search-result");
			if ($searchResult.is(":visible")) {
				var $searchResultHead = $searchResult.find(".search-result>.head");
				var $searchResultBody = $searchResult.find(".search-result>.body");
				
				$searchResult.outerWidth(width);
				$searchResultBody.outerWidth($searchResult.width());

				var searchResultBodyHeight = $searchResult.height() 
						- $("#search-result-resize-handle").outerHeight() 
						- $searchResultHead.outerHeight();
				$searchResultBody.outerHeight(searchResultBodyHeight);
				
				height -= $searchResult.outerHeight();
			}
			$blobContent.outerWidth(width).outerHeight(height);
			$blobContent.find(".autofit:visible").first().triggerHandler(
					"autofit", [$blobContent.width(), $blobContent.height()]);
		});
		
		document.addEventListener("keydown", function(e) {
			if (e.altKey && e.shiftKey && $(".modal:visible").length == 0) {
				if (e.keyCode === 70) {
					e.preventDefault();
					callback("quickSearch");
				} else if (e.keyCode === 84) {
					e.preventDefault();
					callback("advancedSearch");
				}
			}
		});
	},
	getClientHeight: function() {
		var height = $(window).height()-$("#depot-blob>.blob-content").offset().top;
		if ($("#main>.foot").length != 0) 
			height -= $("#main>.foot").outerHeight();
		return height;
	},
	onWindowLoad: function() {
		if (location.hash && !gitplex.server.viewState.getFromHistory()) {
			// Scroll anchors into view (for instance the markdown headline)
			var element = document.getElementsByName(decodeURIComponent(location.hash.slice(1)))[0];
			if (element)
				element.scrollIntoView();
		}
	}
}
